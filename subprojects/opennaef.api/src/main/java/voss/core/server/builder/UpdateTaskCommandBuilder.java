package voss.core.server.builder;

import naef.dto.NaefDto;
import naef.dto.mpls.RsvpLspDto;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.TaskUtil;

import java.io.IOException;
import java.util.Date;

public class UpdateTaskCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NaefDto target;

    public UpdateTaskCommandBuilder(NaefDto target, String editorName) {
        super(NaefDto.class, target, editorName);
        this.target = target;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        commitTask(target);
        if (target instanceof RsvpLspDto) {
            try {
                RsvpLspDto lsp = (RsvpLspDto) target;
                if (lsp.getHopSeries1() != null && TaskUtil.hasTask(lsp.getHopSeries1())) {
                    commitTask(lsp.getHopSeries1());
                }
                if (lsp.getHopSeries2() != null && TaskUtil.hasTask(lsp.getHopSeries2())) {
                    commitTask(lsp.getHopSeries2());
                }
            } catch (ExternalServiceException e) {
                throw new InventoryException(e);
            }
        }
        return BuildResult.SUCCESS;
    }

    private void commitTask(NaefDto inventory) {
        InventoryBuilder.changeContext(cmd, inventory);
        cmd.addCommand(CMD.TASK_COMMIT);
        cmd.addCommand(CMD.TASK_CANCEL);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TASK_ENABLED_TIME, DtoUtil.getMvoDateFormat().format(new Date()));
        recordChange("Task", null, "Create");
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        InventoryBuilder.changeContext(cmd, target);
        cmd.addCommand(CMD.TASK_CANCEL);
        recordChange("Task", "Cancel", null);
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.TASK.getCaption();
    }

}