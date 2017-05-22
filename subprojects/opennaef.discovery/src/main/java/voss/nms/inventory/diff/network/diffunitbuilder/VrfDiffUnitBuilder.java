package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CommandUtil;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.model.Device;
import voss.model.Port;
import voss.model.VrfInstance;
import voss.nms.inventory.builder.conditional.VrfCreateCommands;
import voss.nms.inventory.builder.conditional.VrfDeleteCommands;
import voss.nms.inventory.builder.conditional.VrfUpdateCommands;
import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffUnit;
import voss.nms.inventory.diff.network.DiffConstants;
import voss.nms.inventory.diff.network.DiffPolicy;
import voss.nms.inventory.diff.network.NetworkDiffUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VrfDiffUnitBuilder extends DiffUnitBuilderImpl<VrfInstance, VrfIfDto> {
    private static final Logger log = LoggerFactory.getLogger(VrfDiffUnitBuilder.class);
    private String userName;

    public VrfDiffUnitBuilder(DiffSet set, DiffPolicy policy, String editorName) throws InventoryException, ExternalServiceException, IOException {
        super(set, DiffObjectType.VRFIF.getCaption(), DiffConstants.vrfDepth, editorName);
        this.userName = editorName;
    }

    @Override
    protected DiffUnit update(String inventoryID, VrfInstance networkVrf, VrfIfDto vrfIf) throws IOException {
        VrfUpdateCommands cmd = new VrfUpdateCommands(vrfIf, userName);
        for (Port port : networkVrf.getAttachmentPorts()) {
            String ip = NetworkDiffUtil.getVpnIpAddressOf(port);
            String mask = NetworkDiffUtil.getVpnIpMaskLengthOf(port);
            cmd.addMemberPorts(port.getIfName(), ip, mask);
            log.debug("vrf-update: " + port.getIfName() + " - " + ip + "/" + mask);
        }
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.UPDATE);
        cmd.evaluate();
        unit.addDiffs(cmd);
        unit.addShellCommands(cmd);
        CommandUtil.logCommands(cmd);
        return unit;
    }

    @Override
    protected DiffUnit create(String inventoryID, VrfInstance vrf) throws IOException {
        VrfCreateCommands cmd = new VrfCreateCommands(vrf, userName);
        for (Port port : vrf.getAttachmentPorts()) {
            String ip = NetworkDiffUtil.getVpnIpAddressOf(port);
            String mask = NetworkDiffUtil.getVpnIpMaskLengthOf(port);
            cmd.addMemberPorts(port.getIfName(), ip, mask);
            log.debug("vrf-create: " + port.getIfName() + " - " + ip + "/" + mask);
        }
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.ADD);
        cmd.evaluate();
        unit.addDiffs(cmd);
        unit.addShellCommands(cmd);
        return unit;
    }

    protected DiffUnit delete(String inventoryID, VrfIfDto vrfIf) throws IOException {
        VrfDeleteCommands cmd = new VrfDeleteCommands(vrfIf, userName);
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.REMOVE);
        cmd.evaluate();
        unit.addDiffs(cmd);
        unit.addShellCommands(cmd);
        return unit;
    }

    public static Map<String, VrfIfDto> getDbVrfIfs(Collection<NodeDto> nodes) {
        Map<String, VrfIfDto> dbVrfIfs = new HashMap<String, VrfIfDto>();
        for (NodeDto node : nodes) {
            for (PortDto port : node.getPorts()) {
                if (!(port instanceof VrfIfDto)) {
                    continue;
                }
                VrfIfDto vrfIf = (VrfIfDto) port;
                String id = InventoryIdCalculator.getId(vrfIf);
                dbVrfIfs.put(id, vrfIf);
            }
        }
        return dbVrfIfs;
    }

    public static Map<String, VrfInstance> getNetworkVrfIfs(Collection<Device> devices) {
        Map<String, VrfInstance> networkVrfIfs = new HashMap<String, VrfInstance>();
        for (Device device : devices) {
            for (VrfInstance vrfIf : device.selectPorts(VrfInstance.class)) {
                String id = InventoryIdCalculator.getId(vrfIf);
                networkVrfIfs.put(id, vrfIf);
            }
        }
        return networkVrfIfs;
    }
}