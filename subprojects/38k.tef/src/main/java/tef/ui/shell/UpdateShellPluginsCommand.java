package tef.ui.shell;

import lib38k.plugin.PluginsConfig;

import java.io.IOException;

public class UpdateShellPluginsCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) {
        try {
            ShellPluginsConfig.getInstance().updateConfig();
        } catch (PluginsConfig.UpdateException ue) {
            throw new RuntimeException(ue);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        getSession().updateCommands();
    }
}
