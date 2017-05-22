package voss.multilayernms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class PortRttEditCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;

    public PortRttEditCommandBuilder(IpIfDto target, String editorName) {
        super(PortDto.class, target, editorName, false);
        setConstraint(PortDto.class);
        if (target == null) {
            throw new IllegalArgumentException("port is null.");
        }
    }

    public void setFixedRoundTripTime(Double rtt) {
        String value = null;
        if (rtt == null) {
            value = null;
        } else {
            value = rtt.toString();
        }
        setValue(MPLSNMS_ATTR.FIXED_RTT, value);
    }

    public void setVariableRoundTripTime(Double rtt) {
        String value = null;
        if (rtt == null) {
            value = null;
        } else {
            value = rtt.toString();
        }
        setValue(MPLSNMS_ATTR.VARIABLE_RTT, value);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        InventoryBuilder.changeContext(cmd, target);
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        return BuildResult.SUCCESS;
    }

    @Override
    public BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        InventoryBuilder.changeContext(cmd, target);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.FIXED_RTT, null);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.VARIABLE_RTT, null);
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.PORT.getCaption();
    }

}