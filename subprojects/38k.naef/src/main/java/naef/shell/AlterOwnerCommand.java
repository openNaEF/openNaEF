package naef.shell;

import naef.mvo.NodeElement;
import tef.skelton.ConstraintException;

public class AlterOwnerCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[new owner]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        NodeElement target = contextAsNodeElement();

        checkArgsSize(args, 1);
        String newParentStr = args.arg(0);

        beginWriteTransaction();

        NodeElement newOwner = resolve(NodeElement.class, newParentStr);
        try {
            target.setOwner(newOwner);
        } catch (ConstraintException ce) {
            throw new ShellCommandException(ce.getMessage());
        }

        commitTransaction();
    }
}
