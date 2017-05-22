package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.serial.TdmSerialIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;

public class ChannelPortCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final PortDto owner;
    private final String ownerName;
    private final String nodeName;
    private final TdmSerialIfDto port;

    private String ifName = null;

    public ChannelPortCommandBuilder(String ownerName, String editorName) {
        super(TdmSerialIfDto.class, null, null, editorName);
        try {
            this.owner = null;
            this.ownerName = ownerName;
            this.nodeName = ownerName.substring(0, ownerName.indexOf(','));
            this.port = null;
            setConstraint(TdmSerialIfDto.class);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public ChannelPortCommandBuilder(PortDto owner, String editorName) {
        super(TdmSerialIfDto.class, owner.getNode(), null, editorName);
        this.owner = owner;
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = ownerName.substring(0, ownerName.indexOf(','));
        this.port = null;
        setConstraint(TdmSerialIfDto.class);
    }

    public ChannelPortCommandBuilder(TdmSerialIfDto port, String editorName) {
        super(TdmSerialIfDto.class, port.getNode(), port, editorName);
        this.owner = (PortDto) port.getOwner();
        this.ownerName = owner.getAbsoluteName();
        this.nodeName = ownerName.substring(0, ownerName.indexOf(','));
        this.port = port;
        if (port != null) {
            initialize(port);
        }
        setConstraint(TdmSerialIfDto.class);
    }

    private void initialize(TdmSerialIfDto port) {
        this.ifName = DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.IFNAME);
        this.cmd.addVersionCheckTarget(port);
    }

    public void setIfName(String ifName) {
        super.setIfName(ifName);
        this.ifName = ifName;
        setValue(MPLSNMS_ATTR.IFNAME, ifName);
    }

    public void setBandwidth(Long bandwidth) {
        setValue(MPLSNMS_ATTR.BANDWIDTH, bandwidth);
    }

    public void setSource(String source) {
        setValue(MPLSNMS_ATTR.SOURCE, source);
    }

    public void setTimeSlot(String timeslot) {
        setValue(MPLSNMS_ATTR.TIMESLOT, timeslot);
    }

    public void setChannelGroup(String group) {
        setValue(MPLSNMS_ATTR.CHANNEL_GROUP, group);
    }

    public String getPortContext() {
        if (owner != null) {
            return owner.getAbsoluteName() + ATTR.NAME_DELIMITER_PRIMARY + getIfName();
        }
        return ownerName + ATTR.NAME_DELIMITER_PRIMARY + getIfName();
    }

    public String getNodeContext() {
        if (owner != null) {
            return owner.getNode().getName();
        }
        return this.nodeName;
    }

    @Override
    public BuildResult buildPortCommands() throws InventoryException {
        if (this.port != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (port == null) {
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.changeContext(cmd, ownerName);
            SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_TDM_SERIAL_PORT, ifName);
        } else {
            InventoryBuilder.changeContext(cmd, port);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        InventoryBuilder.buildSetAttributeUpdateCommand(cmd, listAttributes);
        this.cmd.addLastEditCommands();
        assignTargetPortToShellContextVariable();

        if (port != null) {
            String currentPortName = port.getName();
            if (!currentPortName.equals(ifName)) {
                InventoryBuilder.buildRenameCommands(cmd, ifName);
            }
        }
        return BuildResult.SUCCESS;
    }

    public PortDto getPort() {
        return this.port;
    }

    public String getObjectType() {
        return DiffObjectType.SERIAL.getCaption();
    }

}