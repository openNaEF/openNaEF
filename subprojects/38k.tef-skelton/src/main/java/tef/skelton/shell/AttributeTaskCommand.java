package tef.skelton.shell;

import tef.skelton.Attribute;
import tef.skelton.AttributeTask;
import tef.skelton.AttributeType;
import tef.skelton.Model;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;
import tef.skelton.Task.TaskException;

public class AttributeTaskCommand extends SkeltonShellCommand {

    private enum Operation {

        NEW {

            @Override void process(SkeltonShellSession session, Model context, Commandline args)
                throws ShellCommandException, TaskException
            {
                checkExtraArgsSize(args, 1, "extra-args: [task name]");
                String taskName = args.arg(1);

                new AttributeTask(taskName, context);
            }
        },

        CANCEL {

            @Override void process(SkeltonShellSession session, Model context, Commandline args)
                throws ShellCommandException, TaskException
            {
                if (getTask(context) == null) {
                    throw new TaskException("タスクは設定されていません.");
                }

                checkExtraArgsSize(args, 0, "no extra args expected.");

                getTask(context).cancel();
            }
        },

        ADD_ENTRY {

            @Override void process(SkeltonShellSession session, Model context, Commandline args)
                throws ShellCommandException, TaskException
            {
                if (getTask(context) == null) {
                    throw new TaskException("タスクは設定されていません.");
                }

                checkExtraArgsSize(args, 1, 2, "extra-args: [attribute name] [value]");

                String attrName = args.arg(1);
                Attribute attr = Attribute.getAttribute(context.getClass(), attrName);
                if (attr == null) {
                    throw new ShellCommandException("属性が見つかりません.");
                }
                if (attr.getType() == null) {
                    throw new ShellCommandException("適用対象外の型定義です.");
                }
                if (attr.getType() instanceof AttributeType.MvoCollectionType<?, ?>) {
                    throw new ShellCommandException("指定された属性はコレクション属性です.");
                }

                Object value;
                if (args.argsSize() == 2) { 
                    value = null;
                } else {
                    String valueStr = args.arg(2);
                    if (attr.getType() instanceof AttributeType.ModelType) {
                        try {
                            value = ObjectResolver.<Object>resolve(
                                attr.getType().getJavaType(), context, session, valueStr);
                        } catch (ResolveException re) {
                            throw new ShellCommandException(re.getMessage());
                        }
                    } else {
                        value = attr.getType().parse(valueStr);
                    }
                }

                getTask(context).putConfiguration(attrName, value);
            }
        },

        REMOVE_ENTRY {

            @Override void process(SkeltonShellSession session, Model context, Commandline args)
                throws ShellCommandException, TaskException
            {
                if (getTask(context) == null) {
                    throw new TaskException("タスクは設定されていません.");
                }

                checkExtraArgsSize(args, 1, "extra-args: [attribute name]");
                String attrName = args.arg(1);

                getTask(context).resetConfiguration(attrName);
            }
        },

        ACTIVATE {

            @Override void process(SkeltonShellSession session, Model context, Commandline args)
                throws ShellCommandException, TaskException
            {
                if (getTask(context) == null) {
                    throw new TaskException("タスクは設定されていません.");
                }

                checkExtraArgsSize(args, 0, "no extra args expected.");

                getTask(context).activate();
            }
        };

        abstract void process(SkeltonShellSession session, Model context, Commandline args)
            throws ShellCommandException, TaskException;

        private static AttributeTask getTask(Model context) {
            return context.get(AttributeTask.ATTRIBUTE);
        }

        private static void checkExtraArgsSize(Commandline args, int expectedSize, String errorMessage)
            throws ShellCommandException
        {
            checkExtraArgsSize(args, expectedSize, expectedSize, errorMessage);
        }

        private static void checkExtraArgsSize(
            Commandline args, int expectedMinSize, int expectedMaxSize, String errorMessage)
            throws ShellCommandException
        {
            int actualSize = args.argsSize() - 1;
            if (actualSize < expectedMinSize || expectedMaxSize < actualSize) {
                throw new ShellCommandException(errorMessage);
            }
        }
    }

    @Override public String getArgumentDescription() {
        return "[operation: new, cancel, add-entry, remove-entry, activate]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 1, Integer.MAX_VALUE);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));
        Model context = getContext();
        if (context == null) {
            throw new ShellCommandException("コンテキストを指定してください.");
        }

        beginWriteTransaction();

        try {
            op.process(getSession(), context, args);
        } catch (TaskException te) {
            throw new ShellCommandException(te.getMessage());
        }

        commitTransaction();
    }
}
