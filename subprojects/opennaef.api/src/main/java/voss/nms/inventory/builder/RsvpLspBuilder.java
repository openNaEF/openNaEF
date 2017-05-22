package voss.nms.inventory.builder;

import naef.dto.mpls.RsvpLspDto;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;

public class RsvpLspBuilder extends InventoryBuilder {

    public static void buildAbortedCommand(ShellCommands commands, RsvpLspDto lsp) {
        changeContext(commands, lsp);
        buildAttributeSetOrReset(commands, MPLSNMS_ATTR.ABORTED, Boolean.TRUE.toString());
    }

    public static void buildCompletedCommand(ShellCommands commands, RsvpLspDto lsp) {
        changeContext(commands, lsp);
        buildAttributeSetOrReset(commands, MPLSNMS_ATTR.ABORTED, Boolean.FALSE.toString());
    }

    public static void buildTestedCommand(ShellCommands commands, RsvpLspDto lsp) {
        changeContext(commands, lsp);
        buildAttributeSetOrReset(commands, MPLSNMS_ATTR.SETUP_DATE, DtoUtil.getMvoDateFormat().format(new Date()));
    }

    public static void buildOperationBeginCommand(ShellCommands commands, RsvpLspDto lsp) {
        changeContext(commands, lsp);
        buildAttributeSetOrReset(commands, MPLSNMS_ATTR.OPERATION_BEGIN_DATE, DtoUtil.getMvoDateFormat().format(new Date()));
    }

    public static void buildOperationEndCommand(ShellCommands commands, RsvpLspDto lsp) {
        changeContext(commands, lsp);
        buildAttributeSetOrReset(commands, MPLSNMS_ATTR.OPERATION_BEGIN_DATE, DtoUtil.getMvoDateFormat().format(new Date()));
    }

}