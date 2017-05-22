package naef.shell;

import naef.mvo.NodeElement;
import naef.mvo.OperationType;
import tef.skelton.ConstraintException;
import tef.skelton.UiTypeName;

public class RemoveElementCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[type name] [name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        NodeElement context = contextAsNodeElement();

        checkArgsSize(args, 2);
        UiTypeName type = resolveTypeNameAs(NodeElement.class, args.arg(0));
        String name = args.arg(1);

        beginWriteTransaction();

        NodeElement elem = context.getHereafterSubElement((Class<? extends NodeElement>) type.type(), name);
        if (elem == null) {
            throw new ShellCommandException("指定されたオブジェクトが見つかりません.");
        }

        try {
            context.removeSubElement(elem, OperationType.FINAL);
        } catch (ConstraintException ce) {
            throw new ShellCommandException(ce.getMessage());
        }

        commitTransaction();
    }
}
