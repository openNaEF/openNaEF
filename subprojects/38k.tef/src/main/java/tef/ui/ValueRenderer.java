package tef.ui;

import lib38k.misc.BinaryStringUtils;
import tef.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

public class ValueRenderer {

    public String render(Object value) {
        if (value == null) {
            return renderAsNull();
        }

        if (value.getClass() == byte[].class) {
            return renderAsByteArray((byte[]) value);
        }

        if (value instanceof Collection) {
            return renderAsCollection((Collection) value);
        }

        if (value.getClass().isArray()) {
            return renderAsArray(value);
        }

        if (value instanceof AxislessField) {
            return renderAsAxislessField((AxislessField) value);
        }

        if (value instanceof MonaxisField) {
            return renderAsMonaxisField((MonaxisField) value);
        }

        if (value instanceof MonaxisSet) {
            return renderAsMonaxisSet((MonaxisSet) value);
        }

        if (value instanceof MonaxisMap) {
            return renderAsMonaxisMap((MonaxisMap<?, ?>) value);
        }

        if (value instanceof BinaxesField) {
            return renderAsBinaxesField((BinaxesField<?>) value);
        }

        if (value instanceof BinaxesSet) {
            return renderAsBinaxesSet((BinaxesSet<?>) value);
        }

        if (value instanceof BinaxesMap) {
            return renderAsBinaxesMap((BinaxesMap<?, ?>) value);
        }

        if (value instanceof MVO) {
            return renderAsMvo((MVO) value);
        }

        if (value instanceof String) {
            return renderAsString((String) value);
        }

        if (value instanceof DateTime) {
            return renderAsDateTime((DateTime) value);
        }

        if (value instanceof Date) {
            return renderAsDateTime(DateTime.valueOf((Date) value));
        }

        return renderAsOther(value);
    }

    protected String renderAsNull() {
        return "null";
    }

    protected String renderAsByteArray(byte[] value) {
        return BinaryStringUtils.getHexExpression(value, " ");
    }

    protected String renderAsCollection(Collection values) {
        StringBuffer result = new StringBuffer();
        for (Object value : values) {
            result.append(result.length() == 0 ? "" : ",");
            if (value != null && (value instanceof Collection || value.getClass().isArray())) {
                result.append(render(value));
            } else {
                result.append(escape(render(value)));
            }
        }
        return "{" + result.toString() + "}";
    }

    protected String renderAsArray(Object value) {
        StringBuffer result = new StringBuffer();

        int length = Array.getLength(value);
        result.append("{");
        for (int i = 0; i < length; i++) {
            result.append((i == 0 ? "" : ","));
            Object element = Array.get(value, i);
            if (element != null
                    && (element instanceof Collection || element.getClass().isArray())) {
                result.append(render(element));
            } else {
                result.append(escape(render(element)));
            }
        }
        result.append("}");

        return result.toString();
    }

    private String escape(String str) {
        return str.replaceAll(",", ",,")
                .replaceAll("\\{", "{{").replaceAll("\\}", "}}");
    }

    protected String renderAsAxislessField(AxislessField field) {
        Object value = field.get();
        return render(value);
    }

    protected String renderAsMonaxisField(MonaxisField value) {
        Object content = value.get();
        return render(content);
    }

    protected String renderAsMonaxisSet(MonaxisSet value) {
        StringBuffer result = new StringBuffer();

        for (Object content : value.get()) {
            result.append(result.length() == 0 ? "" : ", ");
            result.append(render(content));
        }

        return result.toString();
    }

    protected <K, V> String renderAsMonaxisMap(MonaxisMap<K, V> value) {
        StringBuffer result = new StringBuffer();
        for (K key : value.getKeys()) {
            result.append(result.length() == 0 ? "" : ", ");
            result.append(render(key));
            result.append(":");
            result.append(render(value.get(key)));
        }
        return result.toString();
    }

    protected <K, V> String renderAsBinaxesMap(BinaxesMap<K, V> value) {
        StringBuffer result = new StringBuffer();
        for (K key : value.getKeys()) {
            result.append(result.length() == 0 ? "" : ", ");
            result.append(render(key));
            result.append(":");
            result.append(renderAsBinaxesFieldChanges(value.getValueChanges(key)));
        }
        return result.toString();
    }

    protected <T> String renderAsBinaxesField(BinaxesField<T> value) {
        return renderAsBinaxesFieldChanges(value.getChanges());
    }

    protected <T> String renderAsBinaxesFieldChanges(SortedMap<Long, T> changes) {
        StringBuffer result = new StringBuffer();
        for (Map.Entry<Long, T> entry : changes.entrySet()) {
            Long time = entry.getKey();
            T value = entry.getValue();

            result.append(result.length() == 0 ? "" : ",");
            result.append(time);
            result.append(":");
            result.append(render(value));
        }

        return result.toString();
    }

    protected <T> String renderAsBinaxesSet(BinaxesSet<T> value) {
        StringBuffer result = new StringBuffer();

        SortedMap<Long, List<T>> changes = value.getChanges();
        for (Long time : changes.keySet()) {
            List<T> values = changes.get(time);

            result.append(time);
            result.append(":");
            result.append(render(values));
        }

        return result.toString();
    }

    protected String renderAsMvo(MVO mvo) {
        String result = ObjectRenderer.render(mvo);
        if (result == null || result.equals("")) {
            result = "&" + mvo.getMvoId().getLocalStringExpression();
        }
        return result;
    }

    protected String renderAsString(String value) {
        return value;
    }

    protected String renderAsDateTime(DateTime value) {
        return TefDateFormatter.formatWithRawValue(value.toJavaDate());
    }

    protected String renderAsOther(Object value) {
        String result = ObjectRenderer.render(value);
        return result != null
                ? result
                : value.toString();
    }
}
