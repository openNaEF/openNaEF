package voss.multilayernms.inventory.diff.service;

import naef.dto.NodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DiscoveryUtil;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.discovery.iolib.DeviceAccessFactory;
import voss.model.Device;
import voss.model.NodeInfo;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.network.DiscoveryResult;
import voss.nms.inventory.diff.network.DiscoveryTask;
import voss.nms.inventory.diff.network.NetworkDiffCalculator;
import voss.nms.inventory.diff.network.PolicyViolation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class NetworkDiffThread extends Thread {
    private static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);
    private static final int DIFF_EVENT_CATEGORY = 19;

    private final DiffCategory category;
    private final List<ExecutionTask> tasks;
    private final DeviceAccessFactory factory;
    private final DiffConfiguration config;
    private final InventoryConnector conn;
    private final ExecutorService threadPoolExecutor;
    private final int defaultThreadSize = 5;
    private final Map<NodeDto, Device> results = new HashMap<NodeDto, Device>();
    private final List<DiscoveryTask> discoveryTasks = new ArrayList<DiscoveryTask>();
    private boolean isRunning = false;
    private final String userName;
    private boolean isInterrupted = false;

    public NetworkDiffThread(DiffCategory category, String userName) {
        this.category = category;
        this.tasks = getExecutionTask();
        this.userName = userName;
        try {
            this.factory = DiscoveryUtil.createDeviceAccessFactory();
            this.conn = InventoryConnector.getInstance();
            this.config = DiffConfiguration.getInstance();
            this.threadPoolExecutor = java.util.concurrent.Executors.newFixedThreadPool(defaultThreadSize);
        } catch (IOException e) {
            voss.multilayernms.inventory.diff.util.Util.unlock(DiffCategory.DISCOVERY, userName);
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void run() {
        try {
            info("Network Diff Creation started.");
            voss.multilayernms.inventory.diff.util.Util.unlockAndLockForce(category, userName);
            setRunning(true);
            doPreProcess();
            if (isInterrupted) {
                return;
            }
            doExecute();
            if (isInterrupted) {
                return;
            }
            NetworkDiffCalculator calculator = new NetworkDiffCalculator(this.results, getCategory());
            calculator.setUserName(userName);
            if (isInterrupted) {
                return;
            }
            calculator.execute();
            if (isInterrupted) {
                return;
            }
            voss.multilayernms.inventory.diff.util.Util.saveDiffSet(calculator.getDiffSet());
            info("Network Diff Creation completed.");
            try {
                List<PolicyViolation> violations = calculator.getIllegalEvents();
                sendEvents(violations);
            } catch (Exception e) {
                warn("failed to send event as policy-violations.", e);
            }
            DiffSetManager.getInstance().setSuccessResult(DiffCategory.DISCOVERY,
                    calculator.getDiffSet().getCreationDate());
        } catch (Exception e) {
            warn("Got error while processing task.", e);
            StringBuffer msg = new StringBuffer();
            msg.append("Abnormally End");
            msg.append("\r\n");
            msg.append(voss.multilayernms.inventory.diff.util.Util.getCauseException(e).getMessage());
            if (!isInterrupted) {
                DiffSetManager.getInstance().setErrorResult(DiffCategory.DISCOVERY, msg.toString());
            }
        } finally {
            setRunning(false);
            this.results.clear();
            voss.multilayernms.inventory.diff.util.Util.unlock(category, userName);
        }
    }

    protected void doPreProcess() {
        try {
            for (NodeDto node : conn.getActiveNodes()) {
                if (NodeRenderer.getManagementIpAddress(node) != null) {
                    NodeInfo nodeInfo = this.config.getNodeInfoFactory().createNodeInfo(node);
                    if (nodeInfo == null) {
                        log.debug("task not created. no node-info: " + node.getName());
                        continue;
                    }
                    boolean saveDebugArchive = DtoUtil.getBoolean(node, MPLSNMS_ATTR.DIFF_DEBUG_ARCHIVE);
                    DiscoveryTask task = new DiscoveryTask(this.factory, node.getName(), node, nodeInfo, saveDebugArchive);
                    discoveryTasks.add(task);
                    log.debug("task added: " + node.getName());
                } else {
                    log.debug("task ignored: " + node.getName());
                }
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    protected void doExecute() {
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
            if (this.tasks != null)
                this.tasks.clear();
        }
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
    public void interrupt() {
        super.interrupt();
        isInterrupted = true;
        this.threadPoolExecutor.shutdownNow();
        log.debug("interrupted.", new Exception());
        setRunning(false);
    }

    private void sendEvents(List<PolicyViolation> violations) throws IOException {
        for (PolicyViolation violation : violations) {
            log.info("diff violation " + violation.inventoryID + "  " + violation.matter + " (" + violation.eventContents + ")");
        }
    }

    public DiffCategory getCategory() {
        return this.category;
    }

    public void setRunning(boolean value) {
        this.isRunning = value;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    protected String getLogString(String s) {
        return Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") " + s;
    }

    protected void error(String s) {
        log.error(getLogString(s));
    }

    protected void error(String s, Throwable t) {
        log.error(getLogString(s), t);
    }

    protected void warn(String s) {
        log.warn(getLogString(s));
    }

    protected void warn(String s, Throwable t) {
        log.warn(getLogString(s), t);
    }

    protected void info(String s) {
        log.info(getLogString(s));
    }

    protected void info(String s, Throwable t) {
        log.info(getLogString(s), t);
    }

    protected void debug(String s) {
        log.debug(getLogString(s));
    }

    protected void debug(String s, Throwable t) {
        log.debug(getLogString(s), t);
    }

    protected void trace(String s) {
        log.trace(getLogString(s));
    }

    protected void trace(String s, Throwable t) {
        log.trace(getLogString(s), t);
    }

    private List<ExecutionTask> getExecutionTask() {
        return tasks;
    }

    public static interface ExecutionTask {
        String getTaskName();
    }

}