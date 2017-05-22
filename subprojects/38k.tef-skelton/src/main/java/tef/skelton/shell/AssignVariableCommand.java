package tef.skelton.shell;

import tef.skelton.Model;

public class AssignVariableCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "[variable name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 1);
        String variableName = args.arg(0);

        Model context = getContext();
        if (context == null) {
            throw new ShellCommandException("コンテキストが指定されていません.");
        }

        getSession().setVariable(variableName, context);
    }
}
