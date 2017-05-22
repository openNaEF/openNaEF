package voss.multilayernms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class IpSubnetMemberChangeCommandBuilder extends AbstractCommandBuilder {
    private final PortDto from;
    private final PortDto to;

    public IpSubnetMemberChangeCommandBuilder(PortDto from, PortDto to, String editorName) {
        super(PortDto.class, null, editorName);
        if (Util.isNull(from, to)) {
            throw new IllegalArgumentException("argument is null.");
        }
        this.from = from;
        this.to = to;
    }

    private static final long serialVersionUID = 1L;


    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        IpIfDto fromIP = NodeUtil.getIpOn(from);
        IpIfDto toIP = NodeUtil.getIpOn(to);
        if (toIP == null) {
            InventoryBuilder.changeContext(cmd, from);
            InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, null);
            InventoryBuilder.changeContext(cmd, to);
            InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, fromIP.getAbsoluteName());
            InventoryBuilder.changeContext(cmd, fromIP);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.FIXED_RTT, null);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.VARIABLE_RTT, null);
        } else {
            InventoryBuilder.changeContext(cmd, from);
            InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, null);
            InventoryBuilder.changeContext(cmd, to);
            InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, null);
            InventoryBuilder.changeContext(cmd, from);
            InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, toIP.getAbsoluteName());
            InventoryBuilder.changeContext(cmd, to);
            InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, fromIP.getAbsoluteName());
            InventoryBuilder.changeContext(cmd, fromIP);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.FIXED_RTT, null);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.VARIABLE_RTT, null);
            InventoryBuilder.changeContext(cmd, toIP);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.FIXED_RTT, null);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.VARIABLE_RTT, null);
        }
        recordChange("MOVE_IP_ADDRESS", "", "");
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() {
        throw new IllegalStateException("not implemented.");
    }

    public String getObjectType() {
        return DiffObjectType.L3_LINK.getCaption();
    }

}