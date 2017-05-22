package voss.core.server.builder;

import naef.dto.NaefDto;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;

import java.io.IOException;

public class LastUpdateCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;

    public LastUpdateCommandBuilder(NaefDto target, String editorName) {
        super(NaefDto.class, target, editorName);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        recordChange(ATTR.LAST_EDITOR, null, getEditor());
        InventoryBuilder.changeContext(cmd, this.target);
        cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        throw new IllegalStateException();
    }

    @Override
    public String getObjectType() {
        return null;
    }

}