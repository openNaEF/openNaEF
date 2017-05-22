package naef.shell;

import naef.mvo.Node;
import tef.skelton.ConstraintException;

/**
 * @deprecated {@link tef.skelton.shell.NewObjectCommand} に置き換えられました.
 */
public class NewNodeCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 1);
        String name = args.arg(0);

        beginWriteTransaction();

        if (Node.home.getByName(name) != null) {
            throw new ShellCommandException("既に存在する装置名です: " + name);
        }

        Node node;
        try {
            node = new Node(name);
        } catch (ConstraintException ce) {
            throw new ShellCommandException(ce.getMessage());
        }

        setContext(node, name);

        commitTransaction();
    }
}
