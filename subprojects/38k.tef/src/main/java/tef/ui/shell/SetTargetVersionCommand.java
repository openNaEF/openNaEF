package tef.ui.shell;

import tef.TransactionId;

public class SetTargetVersionCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "[target version]";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        checkArgsSize(commandline, 0, 1);

        TransactionId.W version = null;
        switch (commandline.argsSize()) {
            case 0:
                version = null;
                break;
            case 1:
                String versionStr = commandline.arg(0);
                if (versionStr.charAt(0) != 'w') {
                    throw new ShellCommandException("invalid version: " + versionStr);
                }
                version = (TransactionId.W) TransactionId.getInstance(versionStr);
                break;
        }

        getSession().setTargetVersion(version);
    }
}
