package tef.skelton.shell;

import tef.skelton.Constants;

public class EditConstantsCommand extends SkeltonShellCommand {

    private enum Operation {

        NEW_CONSTANTS {

            @Override void process(String constantsName, String value)
                throws ShellCommandException {
                if (Constants.home.getByName(constantsName) != null) {
                    throw new ShellCommandException("既に登録されている名前です.");
                }

                new Constants(constantsName);
            }
        },
        ADD_VALUE {

            @Override void process(String constantsName, String value)
                throws ShellCommandException {
                Constants constants = Constants.home.getByName(constantsName);
                if (constants == null) {
                    throw new ShellCommandException("定数名が見つかりません.");
                }
                if (constants.getValues().contains(value)) {
                    throw new ShellCommandException("既に登録されている値です: " + value);
                }

                constants.addValue(value);
            }
        },
        REMOVE_VALUE {

            @Override void process(String constantsName, String value)
                throws ShellCommandException {
                Constants constants = Constants.home.getByName(constantsName);
                if (constants == null) {
                    throw new ShellCommandException("定数名が見つかりません.");
                }
                if (! constants.getValues().contains(value)) {
                    throw new ShellCommandException("登録されていない値です.");
                }

                constants.removeValue(value);
            }
        };

        abstract void process(String constantsName, String value)
            throws ShellCommandException;
    }

    @Override public String getArgumentDescription() {
        return "[operation:new-constants,add-value,remove-value] [constants name] [value]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 2, 3);
        String operationStr = args.arg(0);
        String constantsName = args.arg(1);
        String value = args.argsSize() >= 3 ? args.arg(2) : null;

        Operation op;
        try {
            op = Operation.valueOf(operationStr.replace('-', '_').toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new ShellCommandException("operation を確認してください.");
        }

        beginWriteTransaction();

        op.process(constantsName, value);

        commitTransaction();
    }
}
