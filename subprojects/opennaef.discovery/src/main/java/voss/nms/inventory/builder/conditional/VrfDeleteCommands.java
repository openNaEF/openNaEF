package voss.nms.inventory.builder.conditional;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.SimpleNodeBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;

public class VrfDeleteCommands extends ConditionalCommands<VrfIfDto> {
    private static final long serialVersionUID = 1L;

    public VrfDeleteCommands(VrfIfDto vrfIf, String editorName) {
        super(editorName);
        if (vrfIf == null) {
            throw new IllegalArgumentException("vplsIf is null.");
        }
    }

    @Override
    public void evaluateDiffInner(ShellCommands cmd) {
        cmd.addCommand("# built by VrfDeleteCommands.");
        try {
            VrfIfDto vrfIf = getDto(VrfIfDto.class);
            if (vrfIf == null) {
                throw new IllegalStateException("vplsIf is null.");
            }
            vrfIf.renew();
            for (PortDto sub : vrfIf.getParts()) {
                if (sub instanceof IpIfDto) {
                    IpIfDto ip = (IpIfDto) sub;
                    for (PortDto assoc : ip.getAssociatedPorts()) {
                        InventoryBuilder.changeContext(cmd, assoc);
                        InventoryBuilder.buildPortIpUnbindAsPrimaryCommand(cmd);
                    }
                }
                InventoryBuilder.changeContext(cmd, vrfIf);
                SimpleNodeBuilder.buildPortDeletionCommands(cmd, sub);
            }
            InventoryBuilder.changeContext(cmd, vrfIf);
            cmd.addLastEditCommands();
            for (PortDto removed : vrfIf.getAttachedPorts()) {
                InventoryBuilder.buildDisconnectPortFromNetworkIfCommands(cmd, vrfIf, removed);
                recordChange("attachment port", DtoUtil.getStringOrNull(removed, MPLSNMS_ATTR.IFNAME), null);
            }
            VrfDto vrf = vrfIf.getTrafficDomain();
            if (vrf != null) {
                InventoryBuilder.changeContext(cmd, vrf);
                cmd.addLastEditCommands();
                InventoryBuilder.buildUnbindPortFromNetworkCommands(cmd, vrfIf);
                if (vrf.getMemberVrfifs().size() == 1) {
                    if (vrf.getStringId() != null) {
                        InventoryBuilder.buildNetworkIDReleaseCommand(cmd,
                                ATTR.ATTR_VRF_ID_STRING, ATTR.ATTR_VRF_POOL_STRING);
                    } else {
                        InventoryBuilder.buildNetworkIDReleaseCommand(cmd,
                                ATTR.ATTR_VRF_ID_INTEGER, ATTR.ATTR_VRF_POOL_INTEGER);
                    }
                }
            }
            InventoryBuilder.changeContext(cmd, vrfIf.getOwner());
            SimpleNodeBuilder.buildPortDeletionCommands(cmd, vrfIf);
            recordChange("VRF Instance", vrfIf.getName(), null);
            clearAssertions();
            addAssertion(vrfIf);
            if (vrf != null) {
                addAssertion(vrf);
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

}