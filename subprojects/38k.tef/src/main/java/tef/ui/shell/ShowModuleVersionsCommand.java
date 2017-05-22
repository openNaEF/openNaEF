package tef.ui.shell;

import lib38k.text.TextTable;
import tef.TefService;

public class ShowModuleVersionsCommand extends ShellCommand {

    @Override
    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        checkArgsSize(commandline, 0);

        TefService service = TefService.instance();

        TextTable table = new TextTable(new String[]{"module", "version"});
        for (String moduleName : service.getModuleNames()) {
            table.addRow(new String[]{moduleName, service.getModuleVersion(moduleName)});
        }

        printTable(table);
    }
}
