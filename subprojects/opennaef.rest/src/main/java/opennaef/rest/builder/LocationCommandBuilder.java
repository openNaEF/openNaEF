package opennaef.rest.builder;

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

public class LocationCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LocationCommandBuilder.class);
    public static final String PARENT = "parent";

    private final LocationDto location;
    private String locationType;
    private LocationDto parent;
    private String parentAbsoluteName;
    private String originalParentName;
    private String caption;

    /**
     * 編集・削除用
     *
     * @param location
     * @param editorName
     */
    public LocationCommandBuilder(LocationDto location, String editorName) {
        super(LocationDto.class, location, editorName);
        setConstraint(LocationDto.class);
        if (location == null) {
            throw new IllegalArgumentException();
        }
        this.location = location;
        this.caption = LocationUtil.getCaption(location);
        this.parentAbsoluteName = this.location.getParent().getAbsoluteName();
        this.originalParentName = parentAbsoluteName;
        this.locationType = DtoUtil.getStringOrNull(location, MPLSNMS_ATTR.LOCATION_TYPE);
    }

    /**
     * 新規作成
     *
     * @param parentAbsoluteName 親となるLocationの absolute-name
     * @param locationType       ロケーションの種別
     * @param caption            表示名
     * @param editorName         editor
     */
    public LocationCommandBuilder(String parentAbsoluteName, String locationType, String caption, String editorName) {
        super(LocationDto.class, null, editorName);
        setConstraint(LocationDto.class);
        this.location = null;
        this.originalParentName = null;
        this.parentAbsoluteName = parentAbsoluteName;
        this.locationType = locationType;
        this.caption = caption;
        setValue(MPLSNMS_ATTR.LOCATION_TYPE, locationType);
        setValue(MPLSNMS_ATTR.CAPTION, caption);
        recordChange("Parent", this.originalParentName, parentAbsoluteName);
    }

    /**
     * 新規作成
     *
     * @param parent       親となるLocation
     * @param locationType ロケーションの種別
     * @param caption      表示名
     * @param editorName   editor
     */
    public LocationCommandBuilder(LocationDto parent, String locationType, String caption, String editorName) {
        this(parent != null ? parent.getAbsoluteName() : null, locationType, caption, editorName);
    }

    public void setCaption(String caption) {
        setValue(MPLSNMS_ATTR.CAPTION, caption);
        this.caption = caption;
    }

    /**
     * 親の変更をするときに、新しい親を指定する.
     *
     * @param parent
     */
    public void setParent(LocationDto parent) {
        checkCaptionAndParentConsistency(parent);
        if (this.location != null) {
            if (DtoUtil.mvoEquals(location.getParent(), parent)) { return; }
//            if (!DtoUtil.hasSameAttributeValue(location.getParent(), parent, MPLSNMS_ATTR.LOCATION_TYPE)) {
//                throw new IllegalStateException("cannot change parent type: "
//                        + LocationRenderer.getLocationType(location.getParent())
//                        + "->" + LocationRenderer.getLocationType(parent));
//            }
        }
        recordChange(PARENT, this.originalParentName, parent.getName());
        this.parentAbsoluteName = parent.getAbsoluteName();
    }

    /**
     * 親の変更をするときに、新しい親を指定する.
     *
     * @param parentAbsoluteName 親の絶対名
     */
    public void setParent(String parentAbsoluteName) {
        if (this.location != null && this.location.getParent() == null && parentAbsoluteName != null) {
            throw new IllegalArgumentException("cannot change root location to non-root.");
        }
        if (this.location != null) {
            String _parentName = location.getParent().getAbsoluteName();
            if (_parentName.equals(parentAbsoluteName)) {
                return;
            }
        }
        recordChange(PARENT, this.originalParentName, parentAbsoluteName);
        this.parentAbsoluteName = parentAbsoluteName;
    }

    public void setLocationType(String locationType) {
        setValue(MPLSNMS_ATTR.LOCATION_TYPE, locationType);
    }

    /**
     * Location のソートで使う文字列
     * @param order
     */
    public void setSortOrder(String order) {
        setValue(MPLSNMS_ATTR.SORT_ORDER, order);
    }

    public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    /**
     * 表示名の重複チェックと、親ロケーションの一貫性をチェックする
     * @param parent 親Location
     */
    private void checkCaptionAndParentConsistency(LocationDto parent) {
        if (this.location == null) return;  // 新規作成の場合はチェックしない

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
        log.debug("location.type=" + this.locationType);
        if (this.location == null) {
            if (this.parent != null && LocationUtil.hasChild(this.location, this.caption)) {
                throw new IllegalStateException("duplicated child: " + this.caption);
            }
            log.debug("create: " + this.caption);
            InventoryBuilder.changeContext(cmd, this.parentAbsoluteName);
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
        recordChange(this.caption, "Delete", null);    //dummy
        return BuildResult.SUCCESS;
    }



    @Override
    public String getObjectType() {
        return DiffObjectType.LOCATION.getCaption();
    }

}
