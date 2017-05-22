package voss.nms.inventory.builder;

import naef.dto.NodeElementDto;
import naef.dto.SlotDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.Date;

public class SlotCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final SlotDto slot;
    private final NodeElementDto owner;
    private final String ownerName;
    private final String oldName;
    private String name = null;

    public SlotCommandBuilder(String parent, String editorName) {
        super(SlotDto.class, null, editorName);
        setConstraint(SlotDto.class);
        if (parent == null) {
            throw new IllegalArgumentException("parent is null.");
        }
        this.owner = null;
        this.slot = null;
        this.ownerName = parent;
        this.oldName = null;
    }

    public SlotCommandBuilder(NodeElementDto parent, String editorName) {
        super(SlotDto.class, null, editorName);
        setConstraint(SlotDto.class);
        if (parent == null) {
            throw new IllegalArgumentException("parent is null.");
        }
        this.owner = parent;
        this.ownerName = parent.getAbsoluteName();
        this.slot = null;
        this.oldName = null;
    }

    public SlotCommandBuilder(SlotDto slot, String editorName) {
        super(SlotDto.class, slot, editorName);
        setConstraint(SlotDto.class);
        if (slot == null) {
            throw new IllegalArgumentException("slot is null.");
        }
        this.slot = slot;
        this.owner = slot.getOwner();
        this.ownerName = this.owner.getAbsoluteName();
        if (owner == null) {
            throw new IllegalArgumentException();
        }
        this.oldName = slot.getName();
        this.name = slot.getName();
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        checkMandatory();
        if (this.slot != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.slot == null) {
            InventoryBuilder.changeContext(cmd, ownerName);
            InventoryBuilder.translate(cmd, CMD.NEW_HARDWARE,
                    CMD.NEW_HARDWARE_KEY_1, ATTR.TYPE_SLOT,
                    CMD.NEW_HARDWARE_KEY_2, name);
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
        } else {
            InventoryBuilder.changeContext(this.cmd, slot);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
        this.cmd.addLastEditCommands();

        if (this.oldName != null && !this.oldName.equals(this.name)) {
            InventoryBuilder.buildRenameCommands(this.cmd, this.name);
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        InventoryBuilder.changeContext(this.cmd, slot.getOwner());
        InventoryBuilder.translate(cmd, CMD.REMOVE_ELEMENT,
                "_TYPE_", slot.getObjectTypeName(),
                "_NAME_", slot.getName());
        recordChange("Slot", slot.getName(), null);
        return BuildResult.SUCCESS;
    }

    private void checkMandatory() {
        if (this.slot == null) {
            if (this.name == null) {
                throw new IllegalStateException();
            }
        } else {

        }
    }

    public SlotDto getSlot() {
        return this.slot;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSlotTypeName(String slotTypeName) {
        setValue(MPLSNMS_ATTR.SLOT_TYPE, slotTypeName);
    }

    public void setPurpose(String purpose) {
        setValue(MPLSNMS_ATTR.PURPOSE, purpose);
    }

    public void setExternalInventoryDBID(String externalInventoryDBID) {
        setValue(MPLSNMS_ATTR.EXTERNAL_INVENTORY_DB_ID, externalInventoryDBID);
    }

    public void setExternalInventoryDBStatus(String externalInventoryDBStatus) {
        setValue(MPLSNMS_ATTR.EXTERNAL_INVENTORY_DB_STATUS, externalInventoryDBStatus);
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    public String getObjectType() {
        return DiffObjectType.SLOT.getCaption();
    }

}