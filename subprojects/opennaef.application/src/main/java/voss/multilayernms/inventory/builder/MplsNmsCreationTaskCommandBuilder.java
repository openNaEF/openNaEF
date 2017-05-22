package voss.multilayernms.inventory.builder;

import naef.dto.NaefDto;
import naef.dto.mpls.RsvpLspDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.multilayernms.inventory.constants.FacilityStatus;
import voss.multilayernms.inventory.renderer.GenericRenderer;
import voss.multilayernms.inventory.util.FacilityStatusUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MplsNmsCreationTaskCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NaefDto target;
    private final SimpleDateFormat df = GenericRenderer.getMvoDateFormat();

    public MplsNmsCreationTaskCommandBuilder(NaefDto target, String editorName) {
        super(NaefDto.class, target, editorName);
        this.target = target;
    }

    public void checkPreCondition() {
        FacilityStatus fs = FacilityStatusUtil.getStatus(target);
        if (fs == null) {
            throw new IllegalStateException("no facility-status.");
        } else if (fs != FacilityStatus.RESERVED) {
            throw new IllegalStateException("unexpected facility-status: " + fs.getDisplayString());
        }
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        checkPreCondition();
        InventoryBuilder.changeContext(cmd, target);
        cmd.addCommand(CMD.TASK_COMMIT);
        cmd.addCommand(CMD.TASK_CANCEL);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TASK_ENABLED, "true");
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TASK_ENABLED_TIME, df.format(new Date()));
        InventoryBuilder.buildAttributeSetOrReset(cmd,
                MPLSNMS_ATTR.FACILITY_STATUS, FacilityStatus.CONFIGURED.getDisplayString());
        recordChange("create", "", "");
        built();
        return setResult(BuildResult.SUCCESS);
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        checkPreCondition();
        InventoryBuilder.changeContext(cmd, target);
        cmd.addCommand(CMD.TASK_CANCEL);

        RsvpLspDto lsp = (RsvpLspDto) target;
        if (lsp == null) {
            throw new IllegalArgumentException("no lsp found.");
        }
        if (lsp.getActiveHopSeries() != null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd,
                    MPLSNMS_ATTR.LSP_ACTIVE_PATH, null);
        }
        if (lsp.getHopSeries1() != null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd,
                    MPLSNMS_ATTR.LSP_PRIMARY_PATH, null);
        }
        if (lsp.getHopSeries2() != null) {
            InventoryBuilder.buildAttributeSetOrReset(cmd,
                    MPLSNMS_ATTR.LSP_BACKUP_PATH, null);
        }

        InventoryBuilder.changeContext(cmd, lsp);
        InventoryBuilder.buildNetworkIDReleaseCommand(cmd, ATTR.ATTR_RSVPLSP_ID, ATTR.ATTR_RSVPLSP_POOL);
        recordChange("delete", "", "");
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.TASK.getCaption();
    }

}