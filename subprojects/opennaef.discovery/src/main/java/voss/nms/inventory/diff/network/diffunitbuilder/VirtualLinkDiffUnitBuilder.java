package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;
import voss.model.Device;
import voss.model.Link;
import voss.model.PhysicalPort;
import voss.nms.inventory.builder.LinkCommandBuilder;
import voss.nms.inventory.builder.TextBasedLinkCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.*;
import voss.nms.inventory.diff.network.DiffConstants;

import java.io.IOException;
import java.util.*;

public class VirtualLinkDiffUnitBuilder {
    private final Logger log;
    private final DiffSet set;
    private final String editorName;

    public VirtualLinkDiffUnitBuilder(DiffSet set, String editorName) {
        this.log = LoggerFactory.getLogger(getClass());
        this.set = set;
        this.editorName = editorName;
    }

    public void buildDiffUnits(Set<Device> virtualDevices, List<NodeDto> virtualNodes) {
        Map<String, Link> networks = getLinks(virtualDevices);
        Map<String, L2LinkDto> dbs = getL2Links(virtualNodes);
        Set<String> idSet = new HashSet<String>();
        idSet.addAll(networks.keySet());
        idSet.addAll(dbs.keySet());
        for (String id : idSet) {
            Link network = networks.get(id);
            L2LinkDto db = dbs.get(id);
            try {
                buildDiffUnit(id, network, db);
            } catch (Exception e) {
                log.warn("failed to build diff-unit: " + id, e);
            }
        }
    }

    private void buildDiffUnit(String id, Link network, L2LinkDto db) throws IOException,
            InventoryException, ExternalServiceException, DuplicationException {
        log.debug("build link diff: " + id);
        if (network != null && db == null) {
            log.debug("- create: " + id);
            create(id, network);
        } else if (network != null && db != null) {
            log.debug("- update: " + id);
            update(id, network, db);
        } else if (network == null && db != null) {
            log.debug("- delete: " + id);
            delete(id, db);
        } else {
            log.debug("- unexpected: " + id);
        }
    }

    private void create(String id, Link network) throws IOException, DuplicationException,
            InventoryException, ExternalServiceException {
        TextBasedLinkCommandBuilder builder = new TextBasedLinkCommandBuilder(editorName);
        PhysicalPort port1 = network.getPort1();
        String nodeName1 = AbsoluteNameFactory.getNodeName(port1.getDevice());
        PhysicalPort port2 = network.getPort2();
        String nodeName2 = AbsoluteNameFactory.getNodeName(port2.getDevice());
        builder.setPort1Name(nodeName1, port1.getIfName());
        builder.setPort2Name(nodeName2, port2.getIfName());
        builder.setLinkType(ATTR.TYPE_ETH_LINK);
        builder.setSource(DiffCategory.DISCOVERY.name());
        BuildResult result = builder.buildCommand();
        if (BuildResult.SUCCESS != result) {
            return;
        }
        DiffUnit unit = new DiffUnit(id, DiffOperationType.ADD);
        unit.addBuilder(builder);
        unit.setDepth(DiffConstants.linkDepth);
        unit.setSourceSystem(DiffCategory.DISCOVERY.name());
        unit.setStatus(DiffStatus.INITIAL);
        unit.setTypeName(DiffObjectType.L2_LINK.name());
        set.addDiffUnit(unit);
    }

    private void update(String id, Link network, L2LinkDto db) {
    }

    private void delete(String id, L2LinkDto db) throws IOException, InventoryException, DuplicationException,
            ExternalServiceException {
        if (!DtoUtil.hasStringValue(db, MPLSNMS_ATTR.SOURCE, DiffCategory.DISCOVERY.name())) {
            log.debug("SKIP: link[" + DtoUtil.toDebugString(db) + "] is not discovered by diff-service.");
            return;
        }
        LinkCommandBuilder builder = new LinkCommandBuilder(db, editorName);
        BuildResult result = builder.buildDeleteCommand();
        if (BuildResult.SUCCESS != result) {
            return;
        }
        DiffUnit unit = new DiffUnit(id, DiffOperationType.REMOVE);
        unit.addBuilder(builder);
        unit.setDepth(DiffConstants.linkDepth);
        unit.setSourceSystem(DiffCategory.DISCOVERY.name());
        unit.setStatus(DiffStatus.INITIAL);
        unit.setTypeName(DiffObjectType.L2_LINK.name());
        set.addDiffUnit(unit);
    }

    private Map<String, Link> getLinks(Set<Device> devices) {
        Map<String, Link> result = new HashMap<String, Link>();
        for (Device device : devices) {
            for (PhysicalPort phy : device.selectPorts(PhysicalPort.class)) {
                Link link = phy.getLink();
                if (link == null) {
                    continue;
                }
                String id = InventoryIdCalculator.getId(link);
                log.debug("found link: " + id);
                result.put(id, link);
            }
        }
        return result;
    }

    private Map<String, L2LinkDto> getL2Links(List<NodeDto> virtualNodes) {
        Map<String, L2LinkDto> result = new HashMap<String, L2LinkDto>();
        for (NodeDto node : virtualNodes) {
            for (JackDto jack : node.getJacks()) {
                PortDto p = jack.getPort();
                if (p == null) {
                    continue;
                }
                LinkDto link = NodeUtil.getLayer2Link(p);
                if (!L2LinkDto.class.isInstance(link)) {
                    continue;
                }
                L2LinkDto l2Link = L2LinkDto.class.cast(link);
                String id = InventoryIdCalculator.getId(l2Link);
                log.debug("found link: " + id);
                result.put(id, l2Link);
            }
        }
        return result;
    }

}