package tef.ui.shell;

import java.util.Arrays;

public class ShowUpdatableConfigNamesCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) {
        String[] updatableConfigNames = tef.config.ConfigUpdateNotifier.getConfigNames();
        Arrays.sort(updatableConfigNames);
        for (String configName : updatableConfigNames) {
            println(configName);
        }
    }
}
