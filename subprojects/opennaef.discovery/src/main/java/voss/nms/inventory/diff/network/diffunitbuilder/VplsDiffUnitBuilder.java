package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vpls.VplsIfDto;
import voss.core.server.builder.CommandUtil;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.model.Device;
import voss.model.Port;
import voss.model.VplsInstance;
import voss.nms.inventory.builder.conditional.VplsCreateCommands;
import voss.nms.inventory.builder.conditional.VplsDeleteCommands;
import voss.nms.inventory.builder.conditional.VplsUpdateCommands;
import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffUnit;
import voss.nms.inventory.diff.network.DiffConstants;
import voss.nms.inventory.diff.network.DiffPolicy;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VplsDiffUnitBuilder extends DiffUnitBuilderImpl<VplsInstance, VplsIfDto> {
    private String userName;

    public VplsDiffUnitBuilder(DiffSet set, DiffPolicy policy, String editorName) throws InventoryException, ExternalServiceException, IOException {
        super(set, DiffObjectType.VPLSIF.getCaption(), DiffConstants.vplsDepth, editorName);
        this.userName = editorName;
    }

    @Override
    protected DiffUnit update(String inventoryID, VplsInstance networkVpls, VplsIfDto vplsIf) throws IOException {
        VplsUpdateCommands cmd = new VplsUpdateCommands(vplsIf, userName);
        for (Port port : networkVpls.getAttachmentPorts()) {
            cmd.addMemberPorts(port.getIfName());
        }
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.UPDATE);
        cmd.evaluate();
        unit.addDiffs(cmd);
        unit.addShellCommands(cmd);
        CommandUtil.logCommands(cmd);
        return unit;
    }

    @Override
    protected DiffUnit create(String inventoryID, VplsInstance vpls) throws IOException {
        VplsCreateCommands cmd = new VplsCreateCommands(vpls, userName);
        for (Port port : vpls.getAttachmentPorts()) {
            cmd.addMemberPorts(port.getIfName());
        }
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.ADD);
        cmd.evaluate();
        unit.addDiffs(cmd);
        unit.addShellCommands(cmd);
        return unit;
    }

    protected DiffUnit delete(String inventoryID, VplsIfDto vplsIf) throws IOException {
        VplsDeleteCommands cmd = new VplsDeleteCommands(vplsIf, userName);
        DiffUnit unit = new DiffUnit(inventoryID, DiffOperationType.REMOVE);
        cmd.evaluate();
        unit.addDiffs(cmd);
        unit.addShellCommands(cmd);
        return unit;
    }

    public static Map<String, VplsIfDto> getDbVplsIfs(Collection<NodeDto> nodes) {
        Map<String, VplsIfDto> dbVplses = new HashMap<String, VplsIfDto>();
        for (NodeDto node : nodes) {
            for (PortDto port : node.getPorts()) {
                if (!(port instanceof VplsIfDto)) {
                    continue;
                }
                VplsIfDto vplsIf = (VplsIfDto) port;
                String id = InventoryIdCalculator.getId(vplsIf);
                dbVplses.put(id, vplsIf);
            }
        }
        return dbVplses;
    }

    public static Map<String, VplsInstance> getNetworkVplsIfs(Collection<Device> devices) {
        Map<String, VplsInstance> networkVplses = new HashMap<String, VplsInstance>();
        for (Device device : devices) {
            for (VplsInstance vplsIf : device.selectPorts(VplsInstance.class)) {
                String id = InventoryIdCalculator.getId(vplsIf);
                networkVplses.put(id, vplsIf);
            }
        }
        return networkVplses;
    }

}