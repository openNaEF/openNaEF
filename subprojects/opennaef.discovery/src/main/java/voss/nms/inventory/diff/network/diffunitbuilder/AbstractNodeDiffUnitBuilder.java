package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.NodeUtil;
import voss.model.Device;
import voss.model.Port;
import voss.nms.inventory.builder.conditional.ConditionalCommands;
import voss.nms.inventory.builder.conditional.NodeCleanUpCommands;
import voss.nms.inventory.diff.*;
import voss.nms.inventory.diff.network.DiffConstants;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.IpAddressDB;
import voss.nms.inventory.diff.network.NetworkDiffUtil;
import voss.nms.inventory.diff.network.analyzer.DeviceAnalyzer;
import voss.nms.inventory.diff.network.analyzer.Renderer;

import java.io.IOException;
import java.util.*;

public abstract class AbstractNodeDiffUnitBuilder<T extends Device> {
    protected final Logger log;
    private final DiffSet set;
    private final DiffPolicy policy;
    private final IpAddressDB ipDB;
    private final Map<Port, String> portToAbsoluteNameMap;
    private final boolean enableNodeCreation;
    private final boolean enableNodeDeletion;
    private final String userName;

    public AbstractNodeDiffUnitBuilder(DiffSet set, DiffPolicy policy, IpAddressDB ipDB,
                                       Map<Port, String> portAbsoluteNameMap, boolean enableNodeCreation,
                                       boolean enableNodeDeletion, String userName) {
        this.log = LoggerFactory.getLogger(this.getClass());
        this.set = set;
        this.policy = policy;
        this.ipDB = ipDB;
        this.portToAbsoluteNameMap = portAbsoluteNameMap;
        this.enableNodeCreation = enableNodeCreation;
        this.enableNodeDeletion = enableNodeDeletion;
        this.userName = userName;
    }

    public void buildDiffUnits(Collection<T> devices, Collection<NodeDto> nodes)
            throws IOException, InventoryException {
        List<NodeDiffUnit> nodeDiffs = toNodeDiffList(devices, nodes);
        for (NodeDiffUnit vmDiff : nodeDiffs) {
            log.info("making diff: " + vmDiff.id);
            if (vmDiff.isCreated()) {
                createNode(vmDiff.device);
            } else if (vmDiff.isChanged()) {
                updateNode(vmDiff.device, vmDiff.node);
            } else if (vmDiff.isDeleted()) {
                deleteNode(vmDiff.node);
            } else {
                log.info("- do nothing (unexpected).");
                continue;
            }
        }
    }

    protected void createNode(Device device) {
        log.info("- create node");
        if (!enableNodeCreation) {
            log.info("-- skip");
            return;
        }
        try {
            DeviceAnalyzer analyzer = new DeviceAnalyzer(device);
            Map<String, Renderer> deviceElements = analyzer.analyze();
            Map<String, NodeElementDto> nodeElements = new HashMap<String, NodeElementDto>();
            buildNodeElementDiff(deviceElements, nodeElements);
        } catch (Exception e) {
            log.error("diff calculation failed: " + device.getDeviceName(), e);
        }
    }

    protected void updateNode(Device device, NodeDto node) {
        log.info("- update node");
        try {
            DeviceAnalyzer analyzer = new DeviceAnalyzer(device);
            Map<String, Renderer> deviceElements = analyzer.analyze();
            Map<String, NodeElementDto> nodeElements = new HashMap<String, NodeElementDto>();
            NodeElementDiffUnitBuilder.prepareNodeElements(node, nodeElements);
            buildNodeElementDiff(deviceElements, nodeElements);
            DiffUnit cleaner = createNodeCleanerDiffUnit(node);
            this.set.addDiffUnit(cleaner);
        } catch (Exception e) {
            log.error("diff calculation failed: " + node.getName(), e);
        }
    }

    protected void buildNodeElementDiff(Map<String, Renderer> deviceElements,
                                        Map<String, NodeElementDto> nodeElements) {
        NodeElementDiffUnitBuilder builder = new NodeElementDiffUnitBuilder(this.policy, this.ipDB);
        List<String> list = NetworkDiffUtil.getWholeIDs(deviceElements, nodeElements);
        for (String inventoryID : list) {
            log.debug("target inventory-id:" + inventoryID);
            Renderer discovered = deviceElements.get(inventoryID);
            NodeElementDto onDatabase = nodeElements.get(inventoryID);
            buildDiffUnit(inventoryID, builder, discovered, onDatabase);
        }
    }

    protected void deleteNode(NodeDto node) {
        log.info("- delete node");
        if (!enableNodeDeletion) {
            log.info("-- skip");
            return;
        }
    }

    protected void buildDiffUnit(String inventoryID, NodeElementDiffUnitBuilder builder, Renderer discovered,
                                 NodeElementDto onDatabase) {
        if (discovered != null && discovered.getModel() instanceof Port) {
            this.portToAbsoluteNameMap.put((Port) discovered.getModel(), discovered.getAbsoluteName());
        }
        DiffOperationType opType = NetworkDiffUtil.getOperationType(discovered, onDatabase);
        if (opType == null) {
            log.warn("unable to determine operation-type. opType = null.");
            return;
        }
        log.debug("- opType: " + opType);
        try {
            DiffUnit unit = builder.buildDiffUnit(inventoryID, discovered, onDatabase, opType, userName);
            if (unit == null) {
                log.info("- skip (no diff-unit): " + opType + " " + inventoryID);
                return;
            }
            this.set.addDiffUnit(unit);
            log.info("- diff-unit created: [" + unit.getDepth() + "] " + inventoryID);
        } catch (DuplicationException e) {
            log.error("- error (duplicated diff-unit): " + inventoryID, e);
        } catch (Exception e) {
            log.info("- diff-unit failed: " + inventoryID, e);
        }
    }

    private DiffUnit createNodeCleanerDiffUnit(NodeDto node) {
        DiffUnit cleaner = new DiffUnit(node.getName(), DiffOperationType.UPDATE);
        ConditionalCommands<NodeDto> cleanUp = new NodeCleanUpCommands(node, userName);
        cleanUp.evaluate();
        cleaner.addDiffs(cleanUp);
        cleaner.addShellCommands(cleanUp);
        cleaner.setStatus(DiffStatus.INITIAL);
        cleaner.setDepth(DiffConstants.nodeIpIfCleanUpDiff);
        cleaner.setDescription("Node clean-up.");
        cleaner.setSourceSystem(DiffCategory.DISCOVERY.name());
        cleaner.setTypeName("misc.");
        log.debug("clearner added: " + node.getName());
        return cleaner;
    }

    abstract protected Map<String, Device> toDeviceMap(Collection<T> devices);

    abstract protected Map<String, NodeDto> toNodeMap(Collection<NodeDto> devices);

    protected List<NodeDiffUnit> toNodeDiffList(Collection<T> devices, Collection<NodeDto> virtualNodes) {
        Map<String, Device> virtualDeviceMap = toDeviceMap(devices);
        Map<String, NodeDto> virtualNodeMap = toNodeMap(virtualNodes);
        Map<String, NodeDiffUnit> vmDiffMap = new HashMap<String, NodeDiffUnit>();
        for (Map.Entry<String, Device> entry : virtualDeviceMap.entrySet()) {
            String id = entry.getKey();
            NodeDiffUnit unit = new NodeDiffUnit(id);
            unit.device = entry.getValue();
            vmDiffMap.put(id, unit);
        }
        for (Map.Entry<String, NodeDto> entry : virtualNodeMap.entrySet()) {
            String id = entry.getKey();
            NodeDiffUnit unit = vmDiffMap.get(id);
            if (unit == null) {
                unit = new NodeDiffUnit(id);
                vmDiffMap.put(id, unit);
            }
            unit.node = entry.getValue();
        }
        List<NodeDiffUnit> result = new ArrayList<NodeDiffUnit>();
        result.addAll(vmDiffMap.values());
        Collections.sort(result);
        return result;
    }

    protected String getNodeId(NodeDto node) {
        if (node == null) {
            return null;
        } else if (!NodeUtil.isVirtualNode(node)) {
            return null;
        }
        return node.getName();
    }

    protected static class NodeDiffUnit implements Comparable<NodeDiffUnit> {
        public final String id;
        public Device device;
        public NodeDto node;

        public NodeDiffUnit(String id) {
            this.id = id;
        }

        public boolean isCreated() {
            return this.device != null && this.node == null;
        }

        public boolean isDeleted() {
            return this.device == null && this.node != null;
        }

        public boolean isChanged() {
            return this.device != null && this.node != null;
        }

        @Override
        public int compareTo(NodeDiffUnit o) {
            return this.id.compareTo(o.id);
        }
    }
}