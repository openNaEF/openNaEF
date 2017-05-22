package tef.skelton.shell;

import tef.skelton.AbstractHierarchicalModel;
import tef.skelton.ConstraintException;
import tef.skelton.NameConfigurableModel;
import tef.skelton.UiTypeName;

import java.lang.reflect.Constructor;

public class NewHierarchicalModelCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "[type name] [name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        AbstractHierarchicalModel parent = getContext() == null 
            ? null 
            : contextAs(AbstractHierarchicalModel.class, "階層型モデル");

        checkArgsSize(args, 1, 2);
        String typename = args.arg(0);
        String name = args.argsSize() < 2 ? null : args.arg(1);

        UiTypeName type = resolveTypeNameAs(AbstractHierarchicalModel.class, typename);
        validateTypeInstantiatable(type.type());
        if (parent != null && parent.getClass() != type.type()) {
            throw new ShellCommandException("コンテキストのモデル型と引数の型が異なります.");
        }

        beginWriteTransaction();

        Constructor<? extends AbstractHierarchicalModel<?>> constructor 
            = getConstructor((Class<? extends AbstractHierarchicalModel<?>>) type.type());
        if (constructor == null) {
            throw new RuntimeException("no constructor found: " + typename);
        }

        AbstractHierarchicalModel newModel;
        try {
            newModel = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (name != null) {
            if (! (newModel instanceof NameConfigurableModel)) {
                throw new ShellCommandException(typename + " は名前指定できません.");
            }
            try {
                ((NameConfigurableModel) newModel).setName(name);
            } catch (ConstraintException ce) {
                throw new ShellCommandException(ce.getMessage());
            }
        }

        newModel.setParent(parent);

        setContext(newModel, typename + ":" + (name == null ? newModel.getMvoId().toString() : name));

        commitTransaction();
    }

    private Constructor<? extends AbstractHierarchicalModel<?>> getConstructor(
        Class<? extends AbstractHierarchicalModel<?>> klass)
    {
        for (Constructor c : klass.getDeclaredConstructors()) {
            if (c.getParameterTypes().length == 0) {
                return (Constructor<? extends AbstractHierarchicalModel<?>>) c;
            }
        }
        return null;
    }
}
