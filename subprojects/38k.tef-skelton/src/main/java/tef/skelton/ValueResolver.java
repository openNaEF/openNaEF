package tef.skelton;

import tef.DateTime;
import tef.DateTimeFormat;

import java.text.ParseException;

public abstract class ValueResolver<T> implements java.io.Serializable {

    public static boolean isNullStr(String str) {
        return str == null || str.equals("");
    }

    public static Boolean parseBoolean(String str, boolean allowNull) throws FormatException {
        str = str.toLowerCase();
        if (isNullStr(str)) {
            if (allowNull) {
                return null;
            } else {
                throw new FormatException("null は指定できません.");
            }
        } else if (str.equals("true")) {
            return Boolean.TRUE;
        } else if (str.equals("false")) {
            return Boolean.FALSE;
        }
        throw new FormatException("true/false のいずれかを指定してください.");
    }

    public static Integer parseInteger(String str, boolean allowNull) throws FormatException {
        if (isNullStr(str)) {
            if (allowNull) {
                return null;
            } else {
                throw new FormatException("null は指定できません.");
            }
        }

        try {
            return new Integer(str);
        } catch (NumberFormatException nfe) {
            throw new FormatException("数値形式が不正です: " + str);
        }
    }

    public static Long parseLong(String str, boolean allowNull) throws FormatException {
        if (isNullStr(str)) {
            if (allowNull) {
                return null;
            } else {
                throw new FormatException("null は指定できません.");
            }
        }

        try {
            return new Long(str);
        } catch (NumberFormatException nfe) {
            throw new FormatException("数値形式が不正です: " + str);
        }
    }

    public static Float parseFloat(String str, boolean allowNull) throws FormatException {
        if (isNullStr(str)) {
            if (allowNull) {
                return null;
            } else {
                throw new FormatException("null は指定できません.");
            }
        }

        try {
            return new Float(str);
        } catch (NumberFormatException nfe) {
            throw new FormatException("数値形式が不正です: " + str);
        }
    }

    public static Double parseDouble(String str, boolean allowNull) throws FormatException {
        if (isNullStr(str)) {
            if (allowNull) {
                return null;
            } else {
                throw new FormatException("null は指定できません.");
            }
        }

        try {
            return new Double(str);
        } catch (NumberFormatException nfe) {
            throw new FormatException("数値形式が不正です: " + str);
        }
    }

    public static DateTime parseDateTime(String str, boolean allowNull) throws FormatException {
        if (isNullStr(str)) {
            if (allowNull) {
                return null;
            } else {
                throw new FormatException("null は指定できません.");
            }
        }

        try {
            return DateTime.valueOf(DateTimeFormat.parse(str));
        } catch (ParseException pe) {
            throw new FormatException(pe.getMessage());
        }
    }

    public static <T extends java.lang.Enum<T>> T resolveEnum(Class<T> enumClass, String resolvee, boolean allowNull)
        throws FormatException
    {
        if (isNullStr(resolvee)) {
            if (allowNull) {
                return null;
            } else {
                throw new FormatException("null は指定できません.");
            }
        }

        try {
            return java.lang.Enum.valueOf(enumClass, resolvee.toUpperCase().replace('-', '_').replace('.', '_'));
        } catch (IllegalArgumentException iae) {
            throw new FormatException(enumClass.getClass().getName() + " に " + resolvee + " は定義されていません.");
        }
    }

    public static <T> T resolve(Class<? extends T> klass, VariableHolder variableHolder, String str)
        throws FormatException
    {
        try {
            return ObjectResolver.<T>resolve(klass, null, variableHolder, str);
        } catch (ResolveException re) {
            throw new FormatException(re.getMessage());
        }
    }

    public static ValueResolver<String> STRING = new ValueResolver<String>(String.class) {

        @Override public String resolve(VariableHolder context, String str) {
            return str;
        }
    };

    public static ValueResolver<Boolean> BOOLEAN = new ValueResolver<Boolean>(Boolean.class) {

        @Override public Boolean resolve(VariableHolder context, String str) {
            return parseBoolean(str, true);
        }
    };

    public static ValueResolver<Integer> INTEGER = new ValueResolver<Integer>(Integer.class) {

        @Override public Integer resolve(VariableHolder context, String str) {
            return parseInteger(str, true);
        }
    };

    public static ValueResolver<Long> LONG = new ValueResolver<Long>(Long.class) {

        @Override public Long resolve(VariableHolder context, String str) {
            return parseLong(str, true);
        }
    };

    public static ValueResolver<Float> FLOAT = new ValueResolver<Float>(Float.class) {

        @Override public Float resolve(VariableHolder context, String str) {
            return parseFloat(str, true);
        }
    };

    public static ValueResolver<Double> DOUBLE = new ValueResolver<Double>(Double.class) {

        @Override public Double resolve(VariableHolder context, String str) {
            return parseDouble(str, true);
        }
    };

    public static ValueResolver<DateTime> DATETIME = new ValueResolver<DateTime>(DateTime.class) {

        @Override public DateTime resolve(VariableHolder context, String str) {
            return parseDateTime(str, true);
        }
    };

    public static class Model<T> extends ValueResolver<T> {

        public Model(Class<? extends T> type) {
            super(type);
        }

        @Override public T resolve(VariableHolder context, String str) {
            return ValueResolver.<T>resolve(getType(), context, str);
        }
    }

    public static class Enum<T extends java.lang.Enum<T>> extends ValueResolver<T> {

        public Enum(Class<T> type) {
            super(type);
        }

        @Override public T resolve(VariableHolder context, String str) {
            return resolveEnum((Class<T>) getType(), str, true);
        }
    }

    private final Class<? extends T> type_;

    protected ValueResolver(Class<? extends T> type) {
        type_ = type;
    }

    public Class<? extends T> getType() {
        return type_;
    }

    abstract public T resolve(VariableHolder context, String str) throws FormatException;
}
