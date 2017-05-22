package naef.shell;

import tef.skelton.AbstractHierarchicalModel;
import tef.skelton.KnownRuntimeException;

public class AlterHierarchicalModelParentCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[new parent name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        AbstractHierarchicalModel<?> child = contextAs(AbstractHierarchicalModel.class, "階層型モデル");

        checkArgsSize(args, 1);
        String newParentName = args.arg(0);

        beginWriteTransaction();

        setParent(child, newParentName);

        commitTransaction();
    }

    private <T extends AbstractHierarchicalModel<?>> void setParent(
        AbstractHierarchicalModel<T> child, String newParentName)
        throws ShellCommandException
    {
        T newParent = (T) resolve(child.getClass(), newParentName);
        if (newParent == null) {
            throw new ShellCommandException("オブジェクトが見つかりません: " + newParentName);
        }

        try {
            child.setParent(newParent);
        } catch (KnownRuntimeException kre) {
            throw new ShellCommandException(kre.getMessage());
        }
    }
}
