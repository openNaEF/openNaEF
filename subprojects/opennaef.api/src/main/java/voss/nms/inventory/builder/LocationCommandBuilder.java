package voss.nms.inventory.builder;

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
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.Date;

public class LocationCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LocationCommandBuilder.class);

    public static final String LOCATION_PARENT = "Parent Location";

    private final LocationDto location;
    private String type;
    private String parentAbsoluteName;
    private String caption;

    public LocationCommandBuilder(LocationDto location, String editorName) {
        super(LocationDto.class, location, editorName);
        setConstraint(LocationDto.class);
        this.location = location;
        if (this.location != null) {
            this.caption = LocationUtil.getCaption(this.location);
            this.parentAbsoluteName = this.location.getParent().getAbsoluteName();
            this.type = DtoUtil.getStringOrNull(location, MPLSNMS_ATTR.LOCATION_TYPE);
        }
    }

    public LocationCommandBuilder(LocationDto parent, String type, String caption, String editorName) {
        super(LocationDto.class, null, editorName);
        setConstraint(LocationDto.class);
        this.location = null;
        this.parentAbsoluteName = parent.getAbsoluteName();
        this.type = type;
        this.caption = caption;
        setValue(MPLSNMS_ATTR.LOCATION_TYPE, type);
        setValue(MPLSNMS_ATTR.CAPTION, caption);
    }

    public LocationCommandBuilder(String parentAbsoluteName, String type, String caption, String editorName) {
        super(LocationDto.class, null, editorName);
        setConstraint(LocationDto.class);
        this.location = null;
        this.parentAbsoluteName = parentAbsoluteName;
        this.type = type;
        this.caption = caption;
        setValue(MPLSNMS_ATTR.LOCATION_TYPE, type);
        setValue(MPLSNMS_ATTR.CAPTION, caption);
    }

    public void setCaption(String caption) {
        if (caption == null) {
            throw new IllegalArgumentException("Location name is null.");
        }
        setValue(MPLSNMS_ATTR.CAPTION, caption);
        this.caption = caption;
    }

    public void setParent(LocationDto newParent) {
        if (this.location != null &&
                this.location.getParent() != null &&
                this.location.getParent().getAbsoluteName().equals(newParent)) {
            return;
        }
        this.parentAbsoluteName = newParent.getAbsoluteName();
        recordChange(LOCATION_PARENT, this.parentAbsoluteName, this.parentAbsoluteName);
    }

    public void setLocationType(String type) {
        setValue(MPLSNMS_ATTR.LOCATION_TYPE, type);
    }

    public void setSortOrder(String order) {
        setValue(MPLSNMS_ATTR.SORT_ORDER, order);
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        checkBuilt();
        log.debug("location.type=" + this.type);
        if (this.target != null && !hasChange()) {
            built();
            return BuildResult.NO_CHANGES;
        }
        if (this.location == null) {
            if (this.parentAbsoluteName == null) {
                throw new IllegalStateException("no myself/parent name.");
            }
            log.debug("create: " + this.caption);
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.changeContext(this.cmd, this.parentAbsoluteName);
            InventoryBuilder.buildHierarchicalModelCreationCommand(cmd, ATTR.TYPE_LOCATION, this.caption);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
            this.cmd.addLastEditCommands();
        } else {
            log.debug("update: " + location.getAbsoluteName());
            InventoryBuilder.changeContext(this.cmd, location);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
            this.cmd.addLastEditCommands();

            String originalParentName = this.location.getParent().getAbsoluteName();
            if (!Util.equals(originalParentName, this.parentAbsoluteName)) {
                InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, this.parentAbsoluteName);
            }
            if (!this.caption.equals(this.location.getName())) {
                InventoryBuilder.buildRenameCommands(this.cmd, this.caption);
            }
        }
        built();
        return setResult(BuildResult.SUCCESS);
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        this.cmd.addVersionCheckTarget(location);
        String path = getNameForDelete(this.location.getName());
        InventoryBuilder.changeContext(this.cmd, this.location);
        InventoryBuilder.buildRenameCommands(this.cmd, path);
        InventoryBuilder.buildHierarchicalModelParentChangeCommand(cmd, ModelConstant.LOCATION_TRASH);
        recordChange(this.caption, "Delete", null);
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.LOCATION.getCaption();
    }

}