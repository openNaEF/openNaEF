package tef.skelton.shell;

import lib38k.text.TextTable;
import tef.DateTime;
import tef.MVO;
import tef.skelton.Attribute;
import tef.skelton.AttributeType;
import tef.skelton.Model;
import tef.skelton.MvoCollection;
import tef.skelton.MvoMap;
import tef.skelton.NamedModel;
import tef.skelton.SkeltonTefService;
import tef.ui.ObjectRenderer;

import java.text.SimpleDateFormat;

public class ShowAttributesCommand extends SkeltonShellCommand {

    @Override public String getArgumentDescription() {
        return "";
    }

    @Override public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 0);
        Model context = getContext();
        if (context == null) {
            throw new ShellCommandException("コンテキストが設定されていません.");
        }

        Class<? extends Model> klass = context.getClass();

        beginReadTransaction();

        TextTable table = new TextTable(new String[] {"name", "type", "value"});
        for (String attrName : Attribute.getAttributeNames(klass)) {
            Attribute<?, ? extends Model> attr = Attribute.getAttribute(klass, attrName);
            table.addRow(attrName, getAttributeTypeName(attr), getValueString(context.getValue(attrName)));
        }
        printTable(table);
    }

    private String getAttributeTypeName(Attribute<?, ?> attr) {
        AttributeType<?> attrType = attr == null ? null : attr.getType();
        if (attrType == null) {
            return "";
        } else if (attrType instanceof AttributeType.MvoListType<?>) {
            return "list of " + getTypeName(((AttributeType.MvoListType<?>) attrType).getCollectionType());
        } else if (attrType instanceof AttributeType.MvoSetType<?>) {
            return "set of " + getTypeName(((AttributeType.MvoSetType<?>) attrType).getCollectionType());
        } else if (attrType instanceof AttributeType.MvoMapType<?, ?>) {
            AttributeType.MvoMapType<?, ?> mapType = (AttributeType.MvoMapType<?, ?>) attrType;
            return "map of " + getTypeName(mapType.getKeyType()) + ":" + getTypeName(mapType.getValueType());
        } else {
            return getTypeName(attrType.getJavaType());
        }
    }

    private String getTypeName(Class<?> klass) {
        return klass == null
            ? ""
            : SkeltonTefService.instance().uiTypeNames().getName(klass);
    }

    private String getValueString(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof NamedModel) {
            return ((NamedModel) value).getName();
        } else if (value instanceof MvoCollection<?, ?>) {
            StringBuilder result = new StringBuilder();
            for (Object elem : ((MvoCollection<?, ?>) value).get()) {
                result.append(result.length() == 0 ? "" : ", ");
                result.append(getValueString(elem));
            }
            return "{" + result.toString() + "}";
        } else if (value instanceof MvoMap<?, ?>) {
            MvoMap<Object, Object> map = (MvoMap<Object, Object>) value;
            StringBuilder result = new StringBuilder();
            for (Object k : map.getKeys()) {
                Object v = map.get(k);
                result.append(result.length() == 0 ? "" : ", ");
                result.append(getValueString(k) + ":" + getValueString(v));
            }
            return "{" + result.toString() + "}";
        } else if (value instanceof Enum) {
            return ((Enum) value).name().replace('_', '-').toLowerCase();
        } else if (value instanceof DateTime) {
            return new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss").format(((DateTime) value).toJavaDate());
        } else if (value instanceof MVO) { 
            return ((MVO) value).getMvoId().getLocalStringExpression();
        } else {
            return ObjectRenderer.render(value);
        }
    }
}
