package voss.nms.inventory.builder.conditional;

import naef.dto.PortDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.SimpleNodeBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;

public class VplsDeleteCommands extends ConditionalCommands<VplsIfDto> {
    private static final long serialVersionUID = 1L;

    public VplsDeleteCommands(VplsIfDto vplsIf, String editorName) {
        super(vplsIf, editorName);
        if (vplsIf == null) {
            throw new IllegalArgumentException("vplsIf is null.");
        }
    }

    @Override
    public void evaluateDiffInner(ShellCommands cmd) {
        try {
            VplsIfDto vplsIf = getDto(VplsIfDto.class);
            if (vplsIf == null) {
                throw new IllegalStateException("vplsIf is null.");
            }
            vplsIf.renew();
            InventoryBuilder.changeContext(cmd, vplsIf);
            for (PortDto removed : vplsIf.getAttachedPorts()) {
                InventoryBuilder.buildDisconnectPortFromNetworkIfCommands(cmd, vplsIf, removed);
                recordChange("attachment port", DtoUtil.getStringOrNull(removed, MPLSNMS_ATTR.IFNAME), null);
            }
            VplsDto vpls = vplsIf.getTrafficDomain();
            if (vpls != null) {
                InventoryBuilder.changeContext(cmd, vpls);
                cmd.addLastEditCommands();
                InventoryBuilder.buildUnbindPortFromNetworkCommands(cmd, vplsIf);
                if (vpls.getMemberVplsifs().size() == 1) {
                    if (vpls.getStringId() != null) {
                        InventoryBuilder.buildNetworkIDReleaseCommand(cmd,
                                ATTR.ATTR_VPLS_ID_STRING, ATTR.ATTR_VPLS_POOL_STRING);
                    } else {
                        InventoryBuilder.buildNetworkIDReleaseCommand(cmd,
                                ATTR.ATTR_VPLS_ID_INTEGER, ATTR.ATTR_VPLS_POOL_INTEGER);
                    }
                }
            }
            InventoryBuilder.changeContext(cmd, vplsIf.getOwner());
            SimpleNodeBuilder.buildPortDeletionCommands(cmd, vplsIf);
            recordChange("VPLS Instance", vplsIf.getName(), null);
            cmd.addLastEditCommands();
            clearAssertions();
            addAssertion(vplsIf);
            if (vpls != null) {
                addAssertion(vpls);
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

}