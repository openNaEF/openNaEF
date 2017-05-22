package tef.ui.shell;

import java.text.DecimalFormat;

public class PrintFreeMemoryCommand extends ShellCommand {

    public String getArgumentDescription() {
        return "";
    }

    @Override
    public void process(Commandline commandline) {
        long memoryUsage
                = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        println("memory usage: " + new DecimalFormat().format(memoryUsage));
    }
}
