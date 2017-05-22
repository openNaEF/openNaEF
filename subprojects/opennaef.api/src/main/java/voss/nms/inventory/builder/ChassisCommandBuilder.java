package voss.nms.inventory.builder;

import naef.dto.ChassisDto;
import naef.dto.NodeDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.Date;

public class ChassisCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final ChassisDto chassis;
    private final NodeDto owner;
    private final String ownerName;
    private String chassisName = "";
    private final String originalName;

    public ChassisCommandBuilder(String owner, String editorName) {
        super(ChassisDto.class, null, editorName);
        if (owner == null) {
            throw new IllegalArgumentException("owner is null.");
        }
        setConstraint(ChassisDto.class);
        this.chassis = null;
        this.owner = null;
        this.ownerName = owner;
        this.originalName = null;
    }

    public ChassisCommandBuilder(NodeDto owner, String editorName) {
        super(ChassisDto.class, null, editorName);
        if (owner == null) {
            throw new IllegalArgumentException("owner is null.");
        }
        setConstraint(ChassisDto.class);
        this.chassis = null;
        this.owner = owner;
        this.ownerName = owner.getAbsoluteName();
        this.originalName = null;
    }

    public ChassisCommandBuilder(ChassisDto chassis, String editorName) {
        super(ChassisDto.class, chassis, editorName);
        setConstraint(ChassisDto.class);
        this.chassis = chassis;
        this.owner = chassis.getNode();
        this.ownerName = this.owner.getAbsoluteName();
        this.originalName = chassis.getName();
    }

    public void setChassisName(String name) {
        if (Util.equals(name, originalName)) {
            return;
        }
        this.chassisName = name;
        recordChange(MPLSNMS_ATTR.NAME, this.originalName, name);
    }

    public void setChassisType(String type) {
        setValue(MPLSNMS_ATTR.CHASSIS_TYPE, type);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        if (this.chassis != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.chassis == null) {
            InventoryBuilder.changeContext(cmd, ownerName);
            InventoryBuilder.translate(cmd, CMD.NEW_HARDWARE,
                    CMD.NEW_HARDWARE_KEY_1, ATTR.TYPE_CHASSIS,
                    CMD.NEW_HARDWARE_KEY_2, chassisName);
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
        } else {
            InventoryBuilder.changeContext(this.cmd, chassis);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
        this.cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        checkBuilt();
        InventoryBuilder.changeContext(this.cmd, chassis.getOwner());
        InventoryBuilder.translate(cmd, CMD.REMOVE_ELEMENT,
                CMD.ARG_TYPE, ATTR.TYPE_CHASSIS, CMD.ARG_NAME, chassis.getName());
        recordChange(ATTR.TYPE_CHASSIS, this.originalName, null);
        built();
        return BuildResult.SUCCESS;
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    public String getObjectType() {
        return DiffObjectType.CHASSIS.getCaption();
    }

}