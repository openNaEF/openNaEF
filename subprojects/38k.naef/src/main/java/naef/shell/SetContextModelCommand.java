package naef.shell;

import naef.NaefTefService;
import naef.mvo.NodeElement;
import tef.skelton.AbstractHierarchicalModel;
import tef.skelton.Model;
import tef.skelton.NamedModel;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;
import tef.skelton.Resolver;

public class SetContextModelCommand extends NaefShellCommand {

    @Override public String getArgumentDescription() {
        return "[name]";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 0, 1);
        if (args.argsSize() == 0) {
            setContext(null, null);
            return;
        }

        beginReadTransaction();

        String firstArg = args.arg(0);

        if (firstArg.equals("..")) {
            processParent(args);
        } else if (args.argsSize() == 1) {
            resolveObject(firstArg);
        }
    }

    private void processParent(Commandline args) throws ShellCommandException {
        if (args.argsSize() != 1) {
            throw new ShellCommandException("引数の数が不正です.");
        }

        Model context = getContext();
        if (context instanceof NodeElement) {
            NodeElement owner = ((NodeElement) context).getOwner();
            setContext(owner, owner == null ? null : owner.getFqn());
        } else if (context instanceof AbstractHierarchicalModel) {
            AbstractHierarchicalModel owner = ((AbstractHierarchicalModel) context).getParent();
            setContext(
                owner,
                owner instanceof NamedModel
                    ? ((NamedModel) owner).getName()
                    : owner == null ? null : owner.getMvoId().getLocalStringExpression());
        } else {
            throw new ShellCommandException("コンテキストが階層型モデルではありません.");
        }
    }

    private void resolveObject(String name) throws ShellCommandException {
        try {
            Model obj = ObjectResolver.<Model>resolve(Model.class, getContext(), getSession(), name);

            Resolver<Model> resolver = (Resolver<Model>) NaefTefService.instance().getResolver(obj.getClass());
            String objName = resolver == null
                ? ((tef.MVO) obj).getMvoId().getLocalStringExpression()
                : resolver.getName(obj);

            setContext(obj, objName);
        } catch(ResolveException re) {
            throw new ShellCommandException(re.getMessage());
        }
    }
}
