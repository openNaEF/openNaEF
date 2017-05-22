package tef.ui.shell;

import tef.SystemProperties;
import tef.TefService;

public class ShowSystemPropertiesCommand extends ShellCommand {

    @Override
    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        checkArgsSize(commandline, 0);

        SystemProperties properties = TefService.instance().getSystemProperties();
        for (String propertyName : properties.getPropertyNames()) {
            println(propertyName + ": " + properties.get(propertyName));
        }
    }
}
