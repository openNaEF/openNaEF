package tef.ui.shell;

import tef.TefService;

public class SetSystemPropertyCommand extends ShellCommand {

    @Override
    public String getArgumentDescription() {
        return "[property name] [value]";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        checkArgsSize(commandline, 2);
        String propertyName = commandline.arg(0);
        String value = commandline.arg(1);

        TefService.instance().getSystemProperties().set(propertyName, value);
    }
}
