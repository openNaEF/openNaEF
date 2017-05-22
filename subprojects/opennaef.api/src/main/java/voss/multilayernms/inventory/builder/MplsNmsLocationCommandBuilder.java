package voss.multilayernms.inventory.builder;

import naef.dto.LocationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.constant.ModelConstant;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.LocationUtil;
import voss.core.server.util.Util;
import voss.multilayernms.inventory.database.LocationType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class MplsNmsLocationCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MplsNmsLocationCommandBuilder.class);
    public static final String LOCATION_PATH_DELIMITER = "//";

    private final LocationDto location;
    private LocationType type;
    private LocationDto parent;
    private String parentAbsoluteName;
    private String originalParentName;
    private String caption;

    public MplsNmsLocationCommandBuilder(LocationDto location, String editorName) {
        super(LocationDto.class, location, editorName);
        setConstraint(LocationDto.class);
        if (location == null) {
            throw new IllegalArgumentException();
        }
        this.location = location;
        this.caption = LocationUtil.getCaption(this.location);
        this.parentAbsoluteName = this.location.getParent().getAbsoluteName();
        this.originalParentName = parentAbsoluteName;
        this.type = LocationUtil.getLocationType(location);
    }

    public MplsNmsLocationCommandBuilder(String parentAbsoluteName, LocationType type, String caption, String editorName) {
        super(LocationDto.class, null, editorName);
        setConstraint(LocationDto.class);
        this.location = null;
        this.originalParentName = null;
        this.parentAbsoluteName = parentAbsoluteName;
        this.type = type;
        this.caption = caption;
        setValue(MPLSNMS_ATTR.LOCATION_TYPE, type.getCaption());
        setValue(MPLSNMS_ATTR.CAPTION, caption);
        recordChange("Parent", this.originalParentName, parentAbsoluteName);
    }

    public MplsNmsLocationCommandBuilder(LocationDto parent, LocationType type, String caption, String editorName) {
        super(LocationDto.class, null, editorName);
        setConstraint(LocationDto.class);
        this.location = null;
        this.originalParentName = null;
        this.parent = parent;
        this.parentAbsoluteName = parent.getAbsoluteName();
        this.type = type;
        this.caption = caption;
        setValue(MPLSNMS_ATTR.LOCATION_TYPE, type.getCaption());
        setValue(MPLSNMS_ATTR.CAPTION, caption);
        recordChange("Parent", this.originalParentName, parent.getName());
    }

    public void setCaption(String caption) {
        setValue(MPLSNMS_ATTR.CAPTION, caption);
        this.caption = caption;
    }

    public void setParent(LocationDto parent) {
        checkCaptionAndParentConsistency(parent);
        if (this.location != null) {
            if (DtoUtil.mvoEquals(location.getParent(), parent)) {
                return;
            } else if (!DtoUtil.hasSameAttributeValue(location.getParent(),
                    parent, MPLSNMS_ATTR.LOCATION_TYPE)) {
                throw new IllegalStateException("cannot change parent type: "
                        + LocationUtil.getLocationType(location.getParent())
                        + "->" + LocationUtil.getLocationType(parent));
            }
        }
        recordChange("Parent", this.originalParentName, parent.getName());
        this.parentAbsoluteName = parent.getAbsoluteName();
    }

    public void setParentName(String parentAbsoluteName) {
        if (this.location != null && this.location.getParent() == null && parentAbsoluteName != null) {
            throw new IllegalArgumentException("cannot change root location to non-root.");
        }
        if (this.location != null) {
            String _parentName = location.getParent().getAbsoluteName();
            if (_parentName.equals(parentAbsoluteName)) {
                return;
            }
        }
        recordChange("Parent", this.originalParentName, parentAbsoluteName);
        this.parentAbsoluteName = parentAbsoluteName;
    }

    public void setExternalInventoryDBID(String externalInventoryDBID) {
        setValue(MPLSNMS_ATTR.EXTERNAL_INVENTORY_DB_ID, externalInventoryDBID);
    }

    public void setSortOrder(String order) {
        setValue(MPLSNMS_ATTR.SORT_ORDER, order);
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    public void setBuildingCode(String code) {
        if (LocationType.BUILDING != this.type) {
            throw new IllegalStateException("location-type is not building.");
        }
        setValue(MPLSNMS_ATTR.BUILDING_CODE, code);
    }

    public void setPopName(String pop) {
        if (LocationType.FLOOR != this.type) {
            throw new IllegalStateException("location-type is not floor.");
        }
        setValue(MPLSNMS_ATTR.POP_NAME, pop);
    }

    private void checkCaptionAndParentConsistency(LocationDto parent) {
        if (DtoUtil.mvoEquals(parent, this.location.getParent())) {
            return;
        }
        for (LocationDto child : parent.getChildren()) {
            if (DtoUtil.mvoEquals(location, child)) {
                continue;
            }
            if (LocationUtil.getCaption(child) != null && LocationUtil.getCaption(child).equals(caption)) {
                throw new IllegalStateException("duplicated location found.");
            }
        }
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        log.debug("location.type=" + this.type);
        if (this.location == null) {
            if (this.parent != null && LocationUtil.hasChild(this.location, this.caption)) {
                throw new IllegalStateException("duplicated child: " + this.caption);
            }
            log.debug("create: " + this.caption);
            InventoryBuilder.changeContext(cmd, ATTR.TYPE_LOCATION, this.parentAbsoluteName);
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.TYPE_LOCATION, this.caption);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
        } else {
            log.debug("update: " + location.getName());
            InventoryBuilder.changeContext(this.cmd, this.location);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
            LocationDto originalParent = this.location.getParent();
            if (!Util.equals(originalParent.getAbsoluteName(), this.parentAbsoluteName)) {
                InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, this.parentAbsoluteName);
            }
            if (!this.location.getName().equals(this.caption)) {
                InventoryBuilder.buildRenameCommands(cmd, this.caption);
            }
        }
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        cmd.addVersionCheckTarget(location);
        String path = getNameForDelete(this.location.getName());
        InventoryBuilder.changeContext(cmd, location);
        InventoryBuilder.buildRenameCommands(cmd, path);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.DELETE_FLAG, Boolean.TRUE.toString());
        InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, ModelConstant.LOCATION_TRASH);
        recordChange(this.caption, "Delete", null);
        return BuildResult.SUCCESS;
    }

    public String getPath() {
        String newPath = this.parentAbsoluteName + LOCATION_PATH_DELIMITER + this.caption;
        return newPath;
    }

    public String getObjectType() {
        return DiffObjectType.LOCATION.getCaption();
    }

}