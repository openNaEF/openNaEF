package voss.nms.inventory.builder;

import naef.dto.NaefDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;

public class AttributeUpdateCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NaefDto dto;

    public AttributeUpdateCommandBuilder(NaefDto target, String editorName) {
        super(target.getClass(), target, editorName);
        setConstraint(target.getClass());
        this.dto = target;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException, ExternalServiceException {
        if (!hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        InventoryBuilder.changeContext(cmd, dto);
        InventoryBuilder.buildAttributeUpdateCommand(cmd, dto, getAttributes(), false);
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException, ExternalServiceException {
        throw new IllegalStateException("This builder doesn't support delete operation.");
    }

    @Override
    public String getObjectType() {
        return target.getObjectTypeName();
    }
}