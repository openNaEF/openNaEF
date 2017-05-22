package tef.ui.shell;

public class GcCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) {
        System.gc();
    }
}
