package voss.nms.inventory.builder.conditional;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vpls.VplsIfDto;
import naef.ui.NaefDtoFacade;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.List;

public class VplsUpdateCommands extends ConditionalCommands<VplsIfDto> {
    private static final long serialVersionUID = 1L;
    private final List<String> memberPorts = new ArrayList<String>();

    public VplsUpdateCommands(VplsIfDto vplsIf, String editorName) {
        super(vplsIf, editorName);
    }

    public void addMemberPorts(String ifName) {
        if (!memberPorts.contains(ifName)) {
            memberPorts.add(ifName);
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
            List<String> toAdd = new ArrayList<String>(this.memberPorts);
            List<PortDto> removedPorts = new ArrayList<PortDto>();
            getAttachmentPortDiff(vplsIf.getAttachedPorts(), this.memberPorts, toAdd, removedPorts);
            NodeDto node = vplsIf.getNode();
            NaefDtoFacade facade = DtoUtil.getNaefDtoFacade(node);
            List<PortDto> addedPorts = super.getAddedPortsByIfName(toAdd, node, facade);
            if (addedPorts.size() == 0 && removedPorts.size() == 0) {
                setRawCommands(new ArrayList<String>());
            }
            InventoryBuilder.changeContext(cmd, vplsIf);
            cmd.addLastEditCommands();
            for (PortDto added : addedPorts) {
                InventoryBuilder.buildConnectPortToNetworkIfCommands(cmd, vplsIf, added);
                recordChange("attachment port", null, DtoUtil.getStringOrNull(added, MPLSNMS_ATTR.IFNAME));
            }
            for (PortDto removed : removedPorts) {
                InventoryBuilder.buildDisconnectPortFromNetworkIfCommands(cmd, vplsIf, removed);
                recordChange("attachment port", DtoUtil.getStringOrNull(removed, MPLSNMS_ATTR.IFNAME), null);
            }
            addAssertion(vplsIf);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

}