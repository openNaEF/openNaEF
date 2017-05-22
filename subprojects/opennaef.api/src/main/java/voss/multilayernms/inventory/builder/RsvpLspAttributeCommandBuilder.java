package voss.multilayernms.inventory.builder;

import naef.dto.mpls.RsvpLspDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class RsvpLspAttributeCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final String targetName;

    public RsvpLspAttributeCommandBuilder(RsvpLspDto target, String editorName) {
        super(RsvpLspDto.class, target, editorName);
        setConstraint(RsvpLspDto.class);
        this.targetName = null;
    }

    public RsvpLspAttributeCommandBuilder(String targetName, String editorName) {
        super(RsvpLspDto.class, null, editorName);
        setConstraint(RsvpLspDto.class);
        this.targetName = targetName;
    }

    public void setOperationStatus(String status) {
        setValue(MPLSNMS_ATTR.OPER_STATUS, status);
    }

    public void setPrimaryPathOperationStatus(String status) {
        setValue(MPLSNMS_ATTR.LSP_PRIMARY_PATH_OPER_STATUS, status);
    }

    public void setSecondaryPathOperationStatus(String status) {
        setValue(MPLSNMS_ATTR.LSP_SECONDARY_PATH_OPER_STATUS, status);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        if (this.target != null) {
            InventoryBuilder.changeContext(cmd, target);
        } else {
            InventoryBuilder.changeContext(cmd, this.targetName);
        }
        cmd.addLastEditCommands();
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException {
        return BuildResult.NO_CHANGES;
    }

    public String getObjectType() {
        return DiffObjectType.RSVPLSP.getCaption();
    }

}