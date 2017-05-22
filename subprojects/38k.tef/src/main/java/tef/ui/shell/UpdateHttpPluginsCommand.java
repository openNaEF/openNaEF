package tef.ui.shell;

import lib38k.plugin.PluginsConfig;
import tef.TefService;

import java.io.IOException;

public class UpdateHttpPluginsCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) {
        try {
            TefService.instance().httpd().getPluginsConfig().updateConfig();
        } catch (PluginsConfig.UpdateException ue) {
            throw new RuntimeException(ue);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
