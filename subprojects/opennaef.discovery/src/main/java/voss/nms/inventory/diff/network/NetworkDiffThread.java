package voss.nms.inventory.diff.network;

import naef.dto.NodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DiscoveryUtil;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.discovery.iolib.DeviceAccessFactory;
import voss.model.Device;
import voss.model.NodeInfo;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffExecutionThread;
import voss.nms.inventory.diff.DiffSetManagerImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class NetworkDiffThread extends DiffExecutionThread {
    static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);
    private final List<ExecutionTask> tasks = new ArrayList<ExecutionTask>();
    final DeviceAccessFactory factory;
    protected final DiffConfiguration config;
    private final InventoryConnector conn;
    private final ExecutorService threadPoolExecutor;
    private final int defaultThreadSize = 5;
    private final Map<NodeDto, Device> results = new HashMap<NodeDto, Device>();
    private final List<DiscoveryTask> discoveryTasks = new ArrayList<DiscoveryTask>();
    private final List<String> targetNodes = new ArrayList<String>();

    public NetworkDiffThread(DiffSetManagerImpl manager, DiffCategory category) {
        super(manager, category);
        try {
            this.factory = DiscoveryUtil.createDeviceAccessFactory();
            this.conn = InventoryConnector.getInstance();
            this.config = DiffConfiguration.getInstance();
            this.threadPoolExecutor = java.util.concurrent.Executors.newFixedThreadPool(defaultThreadSize);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void setTargetNodes(List<String> targetNodes) {
        this.targetNodes.addAll(targetNodes);
    }

    public void clearTargetNodes() {
        this.targetNodes.clear();
    }

    @Override
    protected void doPreProcess() {
        try {
            for (NodeDto node : conn.getActiveNodes()) {
                if (this.targetNodes.size() > 0 && !this.targetNodes.contains(node.getName())) {
                    continue;
                }
                if (!NetworkDiffUtil.isDiffTarget(node)) {
                    log.debug("skipped: non diff-target device: " + node.getName());
                    continue;
                }
                if (DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.MANAGEMENT_IP) == null) {
                    log.debug("task ignored: " + node.getName());
                }
                try {
                    NodeInfoFactory factory = this.config.getNodeInfoFactory();
                    NodeInfo nodeInfo = factory.createNodeInfo(node);
                    if (nodeInfo == null) {
                        log.debug("task not created. no node-info: " + node.getName());
                        continue;
                    }
                    boolean diffDebugArchive = DtoUtil.getBoolean(node, MPLSNMS_ATTR.DIFF_DEBUG_ARCHIVE);
                    DiscoveryTask task = new DiscoveryTask(this.factory, node.getName(), node, nodeInfo, diffDebugArchive);
                    discoveryTasks.add(task);
                    log.debug("discovery task added: " + node.getName());
                } catch (Exception e) {
                    log.warn("failed to add discovery task: " + node.getName(), e);
                }
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }


    @Override
    protected void doExecute() {
        setRunning(true);
        try {
            List<Future<DiscoveryResult>> futures = this.threadPoolExecutor.invokeAll(discoveryTasks);
            for (Future<DiscoveryResult> future : futures) {
                DiscoveryResult r = future.get();
                Device d = r.device;
                results.put(r.node, d);
            }
            log.debug("doExecute completed.");
        } catch (InterruptedException e) {
            log.debug("doExecute interrupted.", e);
        } catch (Exception e) {
            this.threadPoolExecutor.shutdownNow();
            log.debug("doExecute got exception. shutdown now.", e);
        } finally {
            closeAll(discoveryTasks);
            this.tasks.clear();
        }
        setRunning(false);
    }

    private void closeAll(List<DiscoveryTask> discoveryTasks) {
        for (DiscoveryTask task : discoveryTasks) {
            if (task.getDeviceAccess() != null) {
                try {
                    task.getDeviceAccess().close();
                } catch (Exception ex) {
                    log.debug("failed to close: " + task.getTaskName(), ex);
                }
            }
        }
    }

    @Override
    protected void doPostProcess(boolean success) throws IOException, InventoryException {
        try {
            if (success) {
                NetworkDiffCalculator calculator = new NetworkDiffCalculator(this.results, getCategory());
                calculator.execute();
                getManager().updateDiffSet(getCategory(), calculator.getDiffSet());
                info("Network Diff Creation completed.");
                try {
                    List<PolicyViolation> violations = calculator.getIllegalEvents();
                    for (PolicyViolation violation : violations) {
                        System.err.println(violation.toString());
                    }
                } catch (Exception e) {
                    warn("failed to send event as policy-violations.", e);
                }
            }
        } catch (ExternalServiceException e) {
            throw new InventoryException(e);
        } finally {
            this.results.clear();
        }
    }

    public Map<NodeDto, Device> getResults() {
        return this.results;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        this.threadPoolExecutor.shutdownNow();
        log.debug("interrupted.", new Exception());
    }
}