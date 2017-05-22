package tef.skelton.shell;

import java.lang.reflect.Constructor;

import tef.MVO;
import tef.skelton.Model;
import tef.skelton.NamedModel;
import tef.skelton.UiTypeName;

public class NewObjectCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "[type name] [name (optional)]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 1, 2);
        String typename = args.arg(0);

        UiTypeName type = resolveTypeNameAs(Model.class, typename);
        if (type == null) {
            throw new ShellCommandException("型名を確認してください.");
        }

        validateTypeInstantiatable(type.type());

        beginWriteTransaction();
        try {
            if (args.argsSize() == 1) {
                if (NamedModel.class.isAssignableFrom(type.type())) {
                    throw new ShellCommandException("名前を指定してください.");
                }

                Constructor<?> constructor;
                try {
                    constructor = type.type().getDeclaredConstructor();
                } catch (NoSuchMethodException nsme) {
                    throw new RuntimeException("constructor が未定義: " + typename);
                }

                Model newObj = (Model) constructor.newInstance();

                setContext(newObj, type.name() + ": " + ((MVO) newObj).getMvoId());
            } else if (args.argsSize() == 2) {
                if (! NamedModel.class.isAssignableFrom(type.type())) {
                    throw new ShellCommandException(type.name() + " は named model ではありません.");
                }

                String name = args.arg(1);

                Constructor<?> constructor;
                try {
                    constructor = type.type().getDeclaredConstructor(String.class);
                } catch (NoSuchMethodException nsme) {
                    throw new RuntimeException("constructor が未定義: " + typename);
                }

                Model newObj = (NamedModel) constructor.newInstance(name);

                setContext(newObj, type.name() + ":" + name);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        commitTransaction();
    }
}
