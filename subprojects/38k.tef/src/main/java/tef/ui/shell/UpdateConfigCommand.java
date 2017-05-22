package tef.ui.shell;

import tef.config.ConfigUpdateException;
import tef.config.ConfigUpdateNotifier;

public class UpdateConfigCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "[config name]";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        checkArgsSize(commandline, 1);
        String configName = commandline.arg(0);

        try {
            int numUpdated = ConfigUpdateNotifier.notifyUpdated(configName);
            println(numUpdated + " listener(s) has invoked.");
        } catch (ConfigUpdateException cue) {
            printStackTrace(cue);
        }
    }
}
