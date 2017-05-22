package tef.skelton.shell;

import tef.skelton.ConstraintException;
import tef.skelton.NameConfigurableModel;

public class AlterNameCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "[new name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        NameConfigurableModel obj = contextAs(NameConfigurableModel.class, "名前変更可能オブジェクト");

        checkArgsSize(args, 1);
        String newName = args.arg(0);

        beginWriteTransaction();

        if (! newName.equals(obj.getName())) {
            try {
                obj.setName(newName);
            } catch (ConstraintException ce) {
                throw new ShellCommandException(ce.getMessage());
            }
        }

        setContext(obj, newName);

        commitTransaction();
    }
}
