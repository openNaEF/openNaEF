package voss.core.server.builder;

import naef.dto.NaefDto;
import org.slf4j.LoggerFactory;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.TaskUtil;
import voss.core.server.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TaskCommandBuilder extends AbstractCommandBuilder {
    public static final String TASK_TYPE = "taskType";

    private static final long serialVersionUID = 1L;
    private final NaefDto target;
    private final String targetName;
    private final Map<String, String> attributes = new HashMap<String, String>();
    private String taskType;

    public TaskCommandBuilder(NaefDto target, String editorName) {
        super(NaefDto.class, target, editorName);
        if (target == null) {
            throw new IllegalArgumentException("no target.");
        }
        this.target = target;
        this.targetName = target.getAbsoluteName();
    }

    public TaskCommandBuilder(String targetName, String editorName) {
        super(NaefDto.class, null, editorName);
        if (targetName == null) {
            throw new IllegalArgumentException("no target.");
        }
        this.target = null;
        this.targetName = targetName;
    }

    public void setTaskType(String type) {
        if (Util.equals(this.taskType, type)) {
            return;
        }
        recordChange(TASK_TYPE, this.taskType, type);
        this.taskType = type;
    }

    public void setAttributes(Map<String, String> updateValues) {
        this.attributes.clear();
        this.attributes.putAll(updateValues);
    }

    public void putAttribute(String key, String value) {
        this.attributes.put(key, value);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        try {
            if (TaskUtil.hasTask(target)) {
                throw new IllegalStateException();
            }
        } catch (ExternalServiceException e) {
            throw new InventoryException(e);
        }
        if (this.taskType == null) {
            throw new IllegalStateException("no task-type set.");
        }
        if (this.target != null) {
            cmd.addVersionCheckTarget(target);
            InventoryBuilder.changeContext(cmd, target);
        } else if (this.targetName != null) {
            InventoryBuilder.changeContext(cmd, targetName);
        } else {
            throw new IllegalStateException("no target.");
        }
        cmd.addCommand(CMD.TASK_NEW + " " + this.taskType);
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            InventoryBuilder.translate(cmd, CMD.TASK_ADD_ENTRY,
                    CMD.TASK_ADD_ENTRY_ARG1_TYPE, entry.getKey(),
                    CMD.TASK_ADD_ENTRY_ARG2_VALUE, entry.getValue());
        }
        recordChange("Task", "", "Created");
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        checkBuilt();
        try {
            if (!TaskUtil.hasTask(target)) {
                throw new IllegalStateException("target has no task. " +
                        "target=" + target.getAbsoluteName());
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(TaskCommandBuilder.class).error(
                    "failed to get task status. target=" + target.getAbsoluteName(), e);
        }
        InventoryBuilder.changeContext(cmd, target);
        cmd.addCommand(CMD.TASK_CANCEL);
        recordChange("Task", "Deleted", "");
        built();
        return setResult(BuildResult.SUCCESS);
    }

    public String getObjectType() {
        return DiffObjectType.TASK.getCaption();
    }

}