package tef;

import lib38k.misc.BinaryStringUtils;

import java.lang.reflect.Array;

final class ValueEncoder {

    static String encode(Object o) {
        if (o == null) {
            return "#null";
        }

        Class<?> type = o.getClass();
        if (type.equals(Boolean.class)) {
            return "#" + (((Boolean) o).booleanValue() ? "t" : "f");
        }
        if (type.equals(Byte.class)) {
            return "#" + Integer.toString((Byte) o, 16) + "B";
        }
        if (type.equals(Character.class)) {
            return "#" + Integer.toString((Character) o, 16) + "C";
        }
        if (type.equals(Double.class)) {
            return "#" + Long.toString(Double.doubleToRawLongBits((Double) o), 16) + "D";
        }
        if (type.equals(Float.class)) {
            return "#" + Integer.toString(Float.floatToRawIntBits((Float) o), 16) + "F";
        }
        if (type.equals(Integer.class)) {
            return "#" + Integer.toString((Integer) o, 16) + "I";
        }
        if (type.equals(Long.class)) {
            return "#" + Long.toString((Long) o, 16) + "L";
        }
        if (type.equals(Short.class)) {
            return "#" + Integer.toString((Short) o, 16) + "S";
        }
        if (type.isArray()) {
            if (type.getComponentType().isArray()) {
                throw new RuntimeException("nested-array is not supported.");
            }

            int length = Array.getLength(o);
            StringBuffer result = new StringBuffer();
            result.append("@");
            result.append(o.getClass().getName());
            result.append("{");
            if (type.getComponentType() == byte.class) {
                byte[] byteArray = new byte[length];
                for (int i = 0; i < byteArray.length; i++) {
                    byteArray[i] = ((Byte) Array.get(o, i)).byteValue();
                }
                result.append(BinaryStringUtils.getHexExpression(byteArray));
            } else {
                for (int i = 0; i < length; i++) {
                    if (i != 0) {
                        result.append(",");
                    }
                    Object element = Array.get(o, i);
                    String valueStr = element == null
                            ? "null"
                            : encode(element);
                    result.append(valueStr);
                }
            }
            result.append("}");
            return result.toString();
        }
        if (type == String.class) {
            return "$" + BinaryStringUtils.encodeToUtf16HexStr((String) o);
        }
        if (type == DateTime.class) {
            return "!" + Long.toString(((DateTime) o).getValue(), 16);
        }
        if (MVO.class.isAssignableFrom(type)) {
            return "&" + ((MVO) o).getMvoId().getLocalStringExpression();
        }
        if (Enum.class.isAssignableFrom(type)) {
            return "%" + ((Enum) o).getDeclaringClass().getName()
                    + "-$" + BinaryStringUtils.encodeToUtf16HexStr(((Enum) o).name());
        }
        if (TransactionId.class.isAssignableFrom(type)) {
            TransactionId transactionId = (TransactionId) o;
            return "~" + Integer.toString(transactionId.serial, 16);
        }

        ExtraObjectCoder<?> eoc = TefService.instance().getExtraObjectCoders().getByType(type);
        if (eoc != null) {
            String encodedValue = ((ExtraObjectCoder<Object>) eoc).encode(o);
            if (0 <= encodedValue.indexOf(" ")) {
                throw new RuntimeException("invalid extra object encoding.");
            }
            return "?" + eoc.getId() + ExtraObjectCoder.ID_CONTENTS_DELIMITER + encodedValue;
        }

        throw new RuntimeException(type.getName() + " is not supported.");
    }
}
