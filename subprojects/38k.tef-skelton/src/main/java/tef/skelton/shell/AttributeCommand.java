package tef.skelton.shell;

import tef.DateTime;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.KnownRuntimeException;
import tef.skelton.Model;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;

public class AttributeCommand extends SkeltonShellCommand {

    private enum Operation {

        SET, RESET, UPDATE_TIMESTAMP, ADD, REMOVE, PUT_MAP, REMOVE_MAP
    }

    @Override public String getArgumentDescription() {
        return "[operation: set,reset,update-timestamp,add,remove,put-map,remove-map]"
            + " [attribute name] ...";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        Model object = getContext();
        if (object == null) {
            throw new ShellCommandException("コンテキストが指定されていません.");
        }

        checkArgsSize(args, 2, 4);
        Operation op = resolveEnum("operation", Operation.class, args.arg(0));
        String attrName = args.arg(1);
        String valueStr;
        String keyStr; 
        switch(op) {
            case SET:
            case ADD:
            case REMOVE:
                checkArgsSize(args, 3);
                valueStr = args.arg(2);
                keyStr = null;
                break;
            case RESET:
                checkArgsSize(args, 2);
                valueStr = null;
                keyStr = null;
                break;
            case UPDATE_TIMESTAMP:
                checkArgsSize(args, 2);
                valueStr = null;
                keyStr = null;
                break;
            case PUT_MAP:
                checkArgsSize(args, 4);
                keyStr = args.arg(2);
                valueStr = args.arg(3);
                break;
            case REMOVE_MAP:
                checkArgsSize(args, 3);
                keyStr = args.arg(2);
                valueStr = null;
                break;
            default:
                throw new ShellCommandException(getCommandUsage());
        }

        Attribute<?, ?> attr = Attribute.getAttribute(object.getClass(), attrName);
        if (attr == null) {
            throw new ShellCommandException("属性名を確認してください: " + attrName);
        }
        if (! attr.isShellConfigurable()) {
            throw new ShellCommandException("設定不可属性です.");
        }

        beginWriteTransaction();

        try {
            switch(op) {
            case SET:
                asSingle(attr).set(object, resolve(attr, valueStr));
                break;
            case RESET:
                asSingle(attr).set(object, null);
                break;
            case UPDATE_TIMESTAMP:
                asSingleDatetime(attr).set(object, DateTime.valueOf(System.currentTimeMillis()));
                break;
            case ADD:
                asCollection(attr).addValue(object, resolve(attr, valueStr));
                break;
            case REMOVE:
                asCollection(attr).removeValue(object, resolve(attr, valueStr));
                break;
            case PUT_MAP:
                putMapping(asMapping(attr), object, keyStr, valueStr);
                break;
            case REMOVE_MAP:
                removeMapping(asMapping(attr), object, keyStr);
                break;
            default:
                throw new RuntimeException();
            }
        } catch (KnownRuntimeException kre) {
            if (kre.getMessage() != null && kre.getCause() == null) {
                throw new ShellCommandException(kre.getMessage());
            } else {
                throw kre;
            }
        }

        commitTransaction();
    }

    private <S, T extends Model> S resolve(Attribute<S, T> attr, String str)
        throws ShellCommandException
    {
        if (attr.getType() instanceof AttributeType.ModelType<?>) {
            try {
                return ObjectResolver.<S>resolve(attr.getType().getJavaType(), getContext(), getSession(), str);
            } catch (ResolveException re) {
                throw new ShellCommandException(re.getMessage());
            }
        } else {
            if (attr.getType() == null) {
                throw new UnsupportedOperationException("属性に型が設定されていません: " + attr.getName());
            }

            if (attr instanceof Attribute.SingleAttr<?, ?>) {
                return (S) ((Attribute.SingleAttr<?, ?>) attr).getType().parse(str);
            } else if (attr instanceof Attribute.CollectionAttr<?, ?, ?, ?>) {
                return (S) ((AttributeType.MvoCollectionType<?, ?>) attr.getType()).parseElement(str);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private <K, V, T extends Model> void putMapping(
        Attribute.MapAttr<K, V, T> attr,
        T obj,
        String keyStr,
        String valueStr)
    {
        attr.put(obj, attr.getType().parseKey(getSession(), keyStr), attr.getType().parseValue(getSession(), valueStr));
    }

    private <K, V, T extends Model> void removeMapping(Attribute.MapAttr<K, V, T> attr, T obj, String keyStr) {
        attr.remove(obj, attr.getType().parseKey(getSession(), keyStr));
    }

    private <S, T extends Model> Attribute.SingleAttr<S, T> asSingle(Attribute<?, ? extends Model> attr)
        throws ShellCommandException
    {
        if (! (attr instanceof Attribute.SingleAttr<?, ?>)) {
            throw new ShellCommandException("指定された属性は単数値属性ではありません.");
        }
        return (Attribute.SingleAttr<S, T>) attr;
    }

    private <T extends Model> Attribute.SingleAttr<DateTime, T> asSingleDatetime(Attribute<?, ? extends Model> attr)
        throws ShellCommandException
    {
        if (attr.getType() == null || attr.getType().getJavaType() != DateTime.class) {
            throw new ShellCommandException("指定された属性は単数 date-time 属性ではありません.");
        }

        return (Attribute.SingleAttr<DateTime, T>) attr;
    }

    private <S, T extends Model> Attribute.CollectionAttr<S, T, ?, ?> asCollection(Attribute<?, ? extends Model> attr)
        throws ShellCommandException
    {
        if (! (attr instanceof Attribute.CollectionAttr<?, ?, ?, ?>)) {
            throw new ShellCommandException("指定された属性は複数値属性ではありません.");
        }
        return (Attribute.CollectionAttr<S, T, ?, ?>) attr;
    }

    private <K, V, T extends Model> Attribute.MapAttr<K, V, T> asMapping(Attribute<?, ? extends Model> attr)
        throws ShellCommandException
    {
        if (! (attr instanceof Attribute.MapAttr<?, ?, ?>)) {
            throw new ShellCommandException("指定された属性はマップ属性ではありません.");
        }
        return (Attribute.MapAttr<K, V, T>) attr;
    }
}
