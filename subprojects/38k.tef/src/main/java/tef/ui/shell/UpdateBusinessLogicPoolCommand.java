package tef.ui.shell;

import tef.TefService;

public class UpdateBusinessLogicPoolCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) {
        TefService.instance().getBusinessLogicPool().updatePool();
    }
}
