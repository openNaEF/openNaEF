package naef.shell;

import naef.mvo.Hardware;
import naef.mvo.NodeElement;

import java.lang.reflect.Constructor;

public class NewHardwareCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[type name] [name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        NodeElement owner = contextAsNodeElement();

        checkArgsSize(args, 2);
        String typeName = args.arg(0);
        String name = args.arg(1);

        beginWriteTransaction();

        Class<? extends Hardware> type = resolveHardwareType(typeName);
        validateTypeInstantiatable(type);

        if (owner.getHereafterSubElement(type, name) != null) {
            throw new ShellCommandException("既に登録されています: " + name);
        }

        Hardware newHardware = newHardware(owner, type, name);

        setContext(newHardware, newHardware.getFqn());

        commitTransaction();
    }

    private Hardware newHardware(NodeElement owner, Class<? extends Hardware> klass, String name) {
        Constructor<? extends Hardware> constructor = getConstructor(klass, owner.getClass());
        if (constructor == null) {
            throw new RuntimeException(
                "constructor が未定義: " + klass.getName() + " owner:" + owner.getClass().getName());
        }

        try {
            return constructor.newInstance(owner, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Constructor<? extends Hardware> getConstructor(
        Class<? extends Hardware> klass, Class<? extends NodeElement> ownerClass)
    {
        for (Constructor c : klass.getDeclaredConstructors()) {
            Class[] paramTypes = c.getParameterTypes();
            if (paramTypes.length == 2
                && paramTypes[ 0 ].isAssignableFrom(ownerClass)
                && paramTypes[ 1 ] == String.class)
            {
                return (Constructor<Hardware>) c;
            }
        }
        return null;
    }
}
