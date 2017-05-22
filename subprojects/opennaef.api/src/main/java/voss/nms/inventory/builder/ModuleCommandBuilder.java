package voss.nms.inventory.builder;

import naef.dto.ModuleDto;
import naef.dto.SlotDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

public class ModuleCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final SlotDto owner;
    private final String ownerName;
    private final ModuleDto module;
    private String moduleName;
    private boolean useMetadata = false;
    private boolean cascadeDelete = false;

    public ModuleCommandBuilder(SlotDto owner, String editorName) {
        super(ModuleDto.class, null, editorName);
        setConstraint(ModuleDto.class);
        this.owner = owner;
        this.ownerName = owner.getAbsoluteName();
        this.module = null;
        this.moduleName = null;
    }

    public ModuleCommandBuilder(String ownerName, String editorName) {
        super(ModuleDto.class, null, editorName);
        setConstraint(ModuleDto.class);
        this.owner = null;
        this.ownerName = ownerName;
        this.module = null;
        this.moduleName = null;
    }

    public ModuleCommandBuilder(ModuleDto module, String editorName) {
        super(ModuleDto.class, module, editorName);
        setConstraint(ModuleDto.class);
        if (module == null) {
            throw new IllegalArgumentException();
        }
        this.module = module;
        this.owner = (SlotDto) module.getOwner();
        this.ownerName = this.owner.getAbsoluteName();
        this.moduleName = DtoUtil.getStringOrNull(module, MPLSNMS_ATTR.MODULE_TYPE);
    }

    public void setCascadeDelete(boolean value) {
        this.cascadeDelete = value;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        checkMandatory();
        if (this.target != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.module == null) {
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            if (this.useMetadata) {
                buildCommandsUsingMetadata();
            } else {
                buildCommandsUsingAttribute();
            }
        } else {
            InventoryBuilder.changeContext(this.cmd, module);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
        this.cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    @Override
    public BuildResult buildDeleteCommandInner() throws IOException {
        InventoryBuilder.changeContext(this.cmd, owner);
        if (cascadeDelete) {
            SimpleNodeBuilder.buildModuleRemoveCommands(cmd, owner);
        } else {
            InventoryBuilder.translate(cmd, CMD.REMOVE_ELEMENT,
                    "_TYPE_", module.getObjectTypeName(),
                    "_NAME_", module.getName());
            cmd.addLastEditCommands();
        }
        recordChange("Module", module.getName(), null);
        return BuildResult.SUCCESS;
    }

    private void buildCommandsUsingMetadata() throws IOException, InventoryException {
        if (owner == null) {
            throw new IllegalArgumentException("owner is null. " +
                    "metadata mode can be used only if owner object is present on inventory.");
        }
        SimpleNodeBuilder.buildModuleInsertionCommands(cmd, owner, this.moduleName, new HashMap<String, String>());
    }

    private void buildCommandsUsingAttribute() {
        InventoryBuilder.changeContext(cmd, ownerName);
        InventoryBuilder.translate(cmd, CMD.NEW_HARDWARE,
                CMD.NEW_HARDWARE_KEY_1, ATTR.TYPE_MODULE,
                CMD.NEW_HARDWARE_KEY_2, "");
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.MODULE_TYPE, moduleName);
    }

    private void checkMandatory() {
        if (this.module == null) {
            if (this.moduleName == null) {
                throw new IllegalStateException();
            }
        } else {

        }
    }

    public ModuleDto getModule() {
        return this.module;
    }

    public void setMetadata(String modelTypeName) {
        this.useMetadata = true;
        this.moduleName = modelTypeName;
    }

    public void setModelTypeName(String modelTypeName) {
        if (this.moduleName == null && modelTypeName == null) {
            return;
        } else if (this.moduleName != null && this.moduleName.equals(modelTypeName)) {
            return;
        }
        recordChange(MPLSNMS_ATTR.MODULE_TYPE, this.moduleName, modelTypeName);
        this.moduleName = modelTypeName;
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
        return DiffObjectType.MODULE.getCaption();
    }

}