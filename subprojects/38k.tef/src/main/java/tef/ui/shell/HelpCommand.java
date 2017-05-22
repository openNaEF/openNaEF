package tef.ui.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HelpCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) {
        for (String commandDescription : getCommandDescriptions()) {
            println(commandDescription);
        }
    }

    private List<String> getCommandDescriptions() {
        List<String> result = new ArrayList<String>();
        for (String cmdname : getSession().getAvailableCommandNames()) {
            ShellCommand cmd = getSession().getCommand(cmdname);
            result.add(cmdname + " " + cmd.getArgumentDescription());
        }

        result.add(ShellSession.EXIT_COMMAND);
        result.add(ShellSession.PROCESS_BATCH_COMMAND + " [batch-file path (optional)]");

        Collections.sort(result, new Comparator<String>() {

            public int compare(String o1, String o2) {
                return o1.split(" ")[0].compareTo(o2.split(" ")[0]);
            }
        });
        return result;
    }
}
