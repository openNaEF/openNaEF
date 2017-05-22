package voss.multilayernms.inventory.builder;

import naef.dto.NaefDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.util.FacilityStatusUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MplsNmsDeletionTaskCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NaefDto target;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    public MplsNmsDeletionTaskCommandBuilder(NaefDto target, String editorName) {
        super(NaefDto.class, target, editorName);
        this.target = target;
    }

    public void checkPreCondition() {
        FacilityStatus fs = FacilityStatusUtil.getStatus(target);
        if (fs == null) {
            throw new IllegalStateException("no facility-status.");
        } else if (!FacilityStatusUtil.isDeletableStatus(fs)) {
            throw new IllegalStateException("unexpected facility-status: " + fs.getDisplayString());
        }
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        checkPreCondition();
        InventoryBuilder.changeContext(cmd, target);
        cmd.addCommand(CMD.TASK_COMMIT);
        cmd.addCommand(CMD.TASK_CANCEL);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TASK_ENABLED, "false");
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TASK_DISABLED_TIME, df.format(new Date()));
        InventoryBuilder.buildAttributeSetOrReset(cmd,
                MPLSNMS_ATTR.FACILITY_STATUS, FacilityStatus.REVOKED.getDisplayString());
        recordChange("create", "", "");
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        checkPreCondition();
        InventoryBuilder.changeContext(cmd, target);
        cmd.addCommand(CMD.TASK_CANCEL);
        recordChange("delete", "", "");
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.TASK.getCaption();
    }

}