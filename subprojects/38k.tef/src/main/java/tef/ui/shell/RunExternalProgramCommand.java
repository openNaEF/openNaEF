package tef.ui.shell;

import tef.ExternalProgramLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RunExternalProgramCommand extends ShellCommand {

    private static final String EXTERNAL_PROGRAM_METHOD_NAME = "run";

    public String getArgumentDescription() {
        return "[external program class name] [args(optional)]";
    }

    @Override
    public void process(Commandline commandline) throws ShellCommandException {
        final String externalProgramName = commandline.arg(0);
        List<String> args = new ArrayList<String>(commandline.args());
        args.remove(0);

        ExternalProgramLoader programLoader;
        try {
            programLoader = new ExternalProgramLoader
                    (externalProgramName, EXTERNAL_PROGRAM_METHOD_NAME);
        } catch (Exception e) {
            String message = "program loading failed: "
                    + e.getClass().getName() + ": " + e.getMessage();
            getSession().log(message);
            throw new ShellCommandException(message);
        }

        Object externalProgram = programLoader.getExternalProgramObject();

        if (externalProgram instanceof ShellExtProgram) {
            ((ShellExtProgram) externalProgram).out = getPrintStream();
            ((ShellExtProgram) externalProgram).args = args.toArray(new String[0]);
            ((ShellExtProgram) externalProgram).session = getSession();
            ((ShellExtProgram) externalProgram).setCommand(this);
        }

        Method method = programLoader.getMethod();
        try {
            method.invoke(externalProgram, new Object[0]);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof ShellExtProgram.ShellExtProgramException) {
                throw new ShellCommandException(cause.getMessage());
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
