package voss.core.server.builder;

import naef.dto.NaefDto;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;

import java.io.IOException;
import java.util.Date;

public class CreationTaskCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NaefDto target;

    public CreationTaskCommandBuilder(NaefDto target, String editorName) {
        super(NaefDto.class, target, editorName);
        this.target = target;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        InventoryBuilder.changeContext(cmd, target);
        cmd.addCommand(CMD.TASK_COMMIT);
        cmd.addCommand(CMD.TASK_CANCEL);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TASK_ENABLED, "true");
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.TASK_ENABLED_TIME, DtoUtil.getMvoDateFormat().format(new Date()));
        recordChange("Task", "", "");
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        InventoryBuilder.changeContext(cmd, target);
        cmd.addCommand(CMD.TASK_CANCEL);
        recordChange("Task", "", "");
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.TASK.getCaption();
    }

}