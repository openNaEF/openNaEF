package voss.core.server.builder;

import naef.dto.NaefDto;
import voss.core.server.exception.InventoryException;

import java.io.IOException;

public class GenericAttributeCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;

    public GenericAttributeCommandBuilder(NaefDto target, String editorName) {
        super(NaefDto.class, target, editorName);
        setConstraint(target.getClass());
    }

    public void setAttribute(String key, String value) {
        setValue(key, value);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        InventoryBuilder.changeContext(cmd, this.target);
        InventoryBuilder.buildAttributeUpdateCommand(cmd, target, attributes, false);
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