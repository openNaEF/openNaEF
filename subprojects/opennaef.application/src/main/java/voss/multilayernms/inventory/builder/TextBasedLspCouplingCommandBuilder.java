package voss.multilayernms.inventory.builder;

import naef.dto.mpls.RsvpLspDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;

public class TextBasedLspCouplingCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final String lsp1;
    private final String lsp2;

    public TextBasedLspCouplingCommandBuilder(String lsp1, String lsp2, String editorName) {
        super(RsvpLspDto.class, null, editorName);
        setConstraint(RsvpLspDto.class);
        if (Util.isNull(lsp1, lsp2)) {
            throw new IllegalArgumentException();
        }
        this.lsp1 = lsp1;
        this.lsp2 = lsp2;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        InventoryBuilder.changeContext(cmd, lsp1);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, this.lsp2);
        cmd.addLastEditCommands();
        InventoryBuilder.changeContext(cmd, lsp2);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, this.lsp1);
        cmd.addLastEditCommands();
        recordChange("Create", "non-paired", "paired");
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        InventoryBuilder.changeContext(cmd, lsp1);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, null);
        cmd.addLastEditCommands();
        InventoryBuilder.changeContext(cmd, lsp2);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.LSP_PAIR, null);
        cmd.addLastEditCommands();
        recordChange("Create", "paired", "non-paired");
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.RSVPLSP.getCaption();
    }
}