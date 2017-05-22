package tef.skelton.shell;

import tef.skelton.NamedModel;
import tef.skelton.UiTypeName;

import java.lang.reflect.Constructor;

/**
 * @deprecated tef.skelton.shell.NewObjectCommand で置き換え.
 */
@Deprecated public class NewNamedModelCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "[type name] [name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 2);
        String typename = args.arg(0);
        String name = args.arg(1);

        UiTypeName type = resolveTypeNameAs(NamedModel.class, typename);
        if (! NamedModel.class.isAssignableFrom(type.type())) {
            throw new ShellCommandException(type.name() + " は named model ではありません.");
        }

        validateTypeInstantiatable(type.type());

        beginWriteTransaction();

        try {
            Constructor<?> constructor;
            try {
                constructor = type.type().getDeclaredConstructor(String.class);
            } catch (NoSuchMethodException nsme) {
                throw new RuntimeException("constructor が未定義: " + typename);
            }

            NamedModel newModel = (NamedModel) constructor.newInstance(name);

            setContext(newModel, type.name() + ":" + name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        commitTransaction();
    }
}
