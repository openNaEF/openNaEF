package naef.shell;

import naef.mvo.Network;
import naef.mvo.Port;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.KnownRuntimeException;
import tef.skelton.Model;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NavigateContextObjectCommand extends NaefShellCommand {

    private enum Operation {

        ATTRIBUTE {

            @Override Model process(NaefShellSession session, Model context, List<String> args)
                throws ShellCommandException
            {
                if (args.size() != 1) {
                    throw new ShellCommandException("args: [attribute name]");
                }

                String attrName = args.get(0);

                Attribute.SingleAttr<?, ?> attr = getSingleAttribute(context.getClass(), attrName);
                Object value = ((Attribute.SingleAttr<Object, Model>) attr).get(context);
                return asNavigatableModel(value);
            }

            private Attribute.SingleAttr<?, ?> getSingleAttribute(Class<? extends Model> klass, String attrName)
                throws ShellCommandException
            {
                Attribute<?, ?> attr = getAttribute(klass, attrName);
                if (! (attr instanceof Attribute.SingleAttr<?, ?>)) {
                    throw new ShellCommandException("指定された属性は単数値属性ではありません.");
                }
                return (Attribute.SingleAttr<?, ?>) attr;
            }
        },

        MAP_ATTRIBUTE {

            @Override Model process(NaefShellSession session, Model context, List<String> args)
                throws ShellCommandException {
                if (args.size() != 2) {
                    throw new ShellCommandException("args: [attribute name] [key]");
                }

                String attrName = args.get(0);
                String keyStr = args.get(1);

                Attribute.MapAttr<?, ?, ?> attr = getMappingAttribute(context.getClass(), attrName);
                AttributeType.MvoMapType<?, ?> attrType = attr.getType();
                Object key = attrType.parseKey(session, keyStr);
                Object value = ((Attribute.MapAttr<Object, Object, Model>) attr).get(context, key);
                return asNavigatableModel(value);
            }

            private Attribute.MapAttr<?, ?, ?> getMappingAttribute(Class<? extends Model> klass, String attrName)
                throws ShellCommandException
            {
                Attribute<?, ?> attr = getAttribute(klass, attrName);
                if (! (attr instanceof Attribute.MapAttr<?, ?, ?>)) {
                    throw new ShellCommandException("指定された属性はマップ属性ではありません.");
                }
                return (Attribute.MapAttr<?, ?, ?>) attr;
            }

            private <K, V, T extends Model> V getMapValue(Attribute.MapAttr<K, V, T> attr, T model, K key) {
                return attr.get(model, key);
            }
        },

        NETWORK_UPPER_LAYER {

            @Override Model process(NaefShellSession session, Model context, List<String> args)
                throws ShellCommandException {
                if (args.size() == 0) {
                    throw new ShellCommandException("args: [port]...");
                }
                if (! (context instanceof Network.LowerStackable)) {
                    throw new ShellCommandException("context は lower-stackable network/link ではありません.");
                }

                List<Port> ports = new ArrayList<Port>();
                for (Model arg : resolveAsModels(session, context, args)) {
                    if (! (arg instanceof Port)) {
                        throw new ShellCommandException("引数が不正です, port ではありません.");
                    }
                    ports.add((Port) arg);
                }

                Network.UpperStackable result
                    = select(((Network.LowerStackable) context).getCurrentUpperLayers(false), ports);
                if (result == null) {
                    throw new ShellCommandException("適合する upper-layer network/link が見つかりませんでした.");
                }

                return result;
            }

            private Network.UpperStackable select(Set<? extends Network.UpperStackable> selectees, List<Port> args)
                throws ShellCommandException
            {
                Network.UpperStackable result = null;
                for (Network.UpperStackable selectee : selectees) {
                    if (selectee.getCurrentMemberPorts().containsAll(args)) {
                        if (result == null) {
                            result = selectee;
                        } else {
                            throw new ShellCommandException("複数の適合する network/link あり, 一意に決定できません.");
                        }
                    }
                }
                return result;
            }
        };

        abstract Model process(NaefShellSession session, Model context, List<String> args)
        throws ShellCommandException;

        private static List<Model> resolveAsModels(NaefShellSession session, Model context, List<String> objStrs)
            throws ShellCommandException
        {
            List<Model> objs = new ArrayList<Model>();
            for (String objStr : objStrs) {
                try {
                    objs.add(ObjectResolver.<Model>resolve(Model.class, context, session, objStr));
                } catch (ResolveException re) {
                    throw new ShellCommandException(re.getMessage());
                }
            }
            return objs;
        }

        private static Attribute<?, ?> getAttribute(Class<? extends Model> klass, String attrName)
            throws ShellCommandException
        {
            Attribute<?, ?> result = Attribute.getAttribute(klass, attrName);
            if (result == null) {
                throw new ShellCommandException("属性定義がありません.");
            }
            return result;
        }

        private static Model asNavigatableModel(Object o) throws ShellCommandException {
            if (o == null) {
                throw new ShellCommandException("値が設定されていません.");
            }
            if (! (o instanceof Model)) {
                throw new ShellCommandException("属性はコンテキスト設定できない型のオブジェクトです.");
            }

            return (Model) o;
        }
    }

    @Override public String getArgumentDescription() {
        return "[operation] [args]...";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 1, Integer.MAX_VALUE);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));
        List<String> objStrs = args.args().subList(1, args.args().size());

        beginReadTransaction();

        try {
            Model obj = op.process(getSession(), contextAs(Model.class, "model"), objStrs);
            setContext(obj, ((tef.MVO) obj).getMvoId().getLocalStringExpression());
        } catch (KnownRuntimeException kre) {
            if (kre.getMessage() != null && kre.getCause() == null) {
                throw new ShellCommandException(kre.getMessage());
            } else {
                throw kre;
            }
        }
    }
}
