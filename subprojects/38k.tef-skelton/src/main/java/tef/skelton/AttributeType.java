package tef.skelton;

import tef.DateTime;
import tef.DateTimeFormat;
import tef.MVO;

import java.io.Serializable;
import java.util.Date;

public abstract class AttributeType<T> implements Serializable {

    public static final class Adapter<T> extends AttributeType<T> {

        public Adapter() {
            super(null);
        }

        @Override public String format(T obj) {
            throw new UnsupportedOperationException();
        }

        @Override public T parse(String str) {
            throw new UnsupportedOperationException();
        }
    }

    public static final AttributeType<String> STRING = new AttributeType<String>(String.class) {

        @Override public String format(String obj) {
            return obj;
        }

        @Override public String parse(String str) {
            return str;
        }
    };

    public static final AttributeType<Boolean> BOOLEAN = new AttributeType<Boolean>(Boolean.class) {

        @Override public String format(Boolean obj) {
            return obj == null ? null : obj.toString().toLowerCase();
        }

        @Override public Boolean parse(String str) throws FormatException {
            return ValueResolver.parseBoolean(str, true);
        }
    };

    public static final AttributeType<Integer> INTEGER = new AttributeType<Integer>(Integer.class) {

        @Override public String format(Integer obj) {
            return obj == null ? null : Integer.toString(obj);
        }

        @Override public Integer parse(String str) throws FormatException {
            return ValueResolver.parseInteger(str, true);
        }
    };

    public static final AttributeType<Long> LONG = new AttributeType<Long>(Long.class) {

        @Override public String format(Long obj) {
            return obj == null ? null : Long.toString(obj);
        }

        @Override public Long parse(String str) throws FormatException {
            return ValueResolver.parseLong(str, true);
        }
    };

    public static final AttributeType<Float> FLOAT = new AttributeType<Float>(Float.class) {

        @Override public String format(Float obj) {
            return obj == null ? null : Float.toString(obj);
        }

        @Override public Float parse(String str) throws FormatException {
            return ValueResolver.parseFloat(str, true);
        }
    };

    public static final AttributeType<Double> DOUBLE = new AttributeType<Double>(Double.class) {

        @Override public String format(Double obj) {
            return obj == null ? null : Double.toString(obj);
        }

        @Override public Double parse(String str) throws FormatException {
            return ValueResolver.parseDouble(str, true);
        }
    };

    public static final AttributeType<DateTime> DATETIME = new AttributeType<DateTime>(DateTime.class) {

        @Override public synchronized String format(DateTime obj) {
            Date d = obj.toJavaDate();
            String ymdhms = DateTimeFormat.YMDHMSS_DOT.format(d);
            if (ymdhms.endsWith("00:00:00.000")) {
                return DateTimeFormat.YMD_DOT.format(d);
            } else {
                return ymdhms;
            }
        }

        @Override public synchronized DateTime parse(String str) throws FormatException {
            return ValueResolver.parseDateTime(str, true);
        }
    };

    public static class EnumType<T extends Enum<T>> extends AttributeType<T> {

        public EnumType(Class<T> type) {
            super(type);
        }

        @Override public String format(T obj) {
            return obj.name().replace('_', '-').toLowerCase();
        }

        @Override public T parse(String str) throws FormatException {
            try {
                return ValueResolver.isNullStr(str)
                    ? null
                    : Enum.valueOf(getJavaType(), str.replace('-', '_').toUpperCase());
            } catch (IllegalArgumentException ia) {
                throw new FormatException("形式を確認してください: " + str);
            }
        }
    }

    public static class ConstantsType extends AttributeType<String> {

        private final String name_;

        public ConstantsType(String name) {
            super(String.class);

            name_ = name;
        }

        @Override public String format(String obj) {
            return obj;
        }

        @Override public String parse(String str) throws ValueException {
            Constants constants = Constants.home.getByName(name_);
            if (constants == null) {
                throw new ValueException("定数が未定義です: " + name_);
            }
            if (! constants.getValues().contains(str)) {
                throw new ValueException("定義されていない値です: " + str);
            }

            return str;
        }
    }

    public static class ModelType<T> extends AttributeType<T> {

        public ModelType(Class<T> type) {
            super(type);

            if (type == null) {
                throw new IllegalArgumentException();
            }
        }

        @Override public String format(T obj) {
            throw new UnsupportedOperationException();
        }

        @Override public T parse(String str) {
            throw new UnsupportedOperationException();
        }
    }

    public static class UniqueNameModelType<T extends MVO & NamedModel> extends ModelType<T> {

        private final transient UniquelyNamedModelHome<T> home_;

        public UniqueNameModelType(UniquelyNamedModelHome<T> home) {
            super(home.getType());

            home_ = home;
        }

        @Override public String format(T obj) {
            return obj.getName();
        }

        @Override public T parse(String str) {
            T result = home_.getByName(str);
            if (result == null) {
                throw new ValueException("登録されていない値です: " + str);
            }
            return result;
        }
    }

    public static abstract class MvoCollectionType<T, U extends MvoCollection<T, ?>>
        extends AttributeType<U>
    {
        private final Class<?> type_;

        protected MvoCollectionType(Class<?> type) {
            super(null, Serializable.class.isAssignableFrom(type));

            type_ = type;
        }

        public Class<?> getCollectionType() {
            return type_;
        }

        @Override public String format(U obj) {
            return obj == null ? "" : obj.getMvoId().getLocalStringExpression();
        }

        @Override public U parse(String str) {
            throw new UnsupportedOperationException();
        }

        abstract public T parseElement(String valueStr) throws FormatException;
    }

    public static abstract class MvoListType<T> extends MvoCollectionType<T, MvoList<T>> {

        public static final MvoListType<String> STRING = new MvoListType<String>(String.class) {

            @Override public String parseElement(String valueStr) {
                return valueStr;
            }
        };

        public static final MvoListType<Boolean> BOOLEAN = new MvoListType<Boolean>(Boolean.class) {

            @Override public Boolean parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseBoolean(valueStr, true);
            }
        };

        public static final MvoListType<Integer> INTEGER = new MvoListType<Integer>(Integer.class) {

            @Override public Integer parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseInteger(valueStr, true);
            }
        };

        public static final MvoListType<Long> LONG = new MvoListType<Long>(Long.class) {

            @Override public Long parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseLong(valueStr, true);
            }
        };

        public static final MvoListType<Float> FLOAT = new MvoListType<Float>(Float.class) {

            @Override public Float parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseFloat(valueStr, true);
            }
        };

        public static final MvoListType<Double> DOUBLE = new MvoListType<Double>(Double.class) {

            @Override public Double parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseDouble(valueStr, true);
            }
        };

        public static final MvoListType<DateTime> DATETIME = new MvoListType<DateTime>(DateTime.class) {

            @Override public DateTime parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseDateTime(valueStr, true);
            }
        };

        protected MvoListType(Class<?> type) {
            super(type);
        }
    }

    public static abstract class MvoSetType<T> extends MvoCollectionType<T, MvoSet<T>> {

        public static final MvoSetType<String> STRING = new MvoSetType<String>(String.class) {

            @Override public String parseElement(String valueStr) {
                return valueStr;
            }
        };

        public static final MvoSetType<Boolean> BOOLEAN = new MvoSetType<Boolean>(Boolean.class) {

            @Override public Boolean parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseBoolean(valueStr, true);
            }
        };

        public static final MvoSetType<Integer> INTEGER = new MvoSetType<Integer>(Integer.class) {

            @Override public Integer parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseInteger(valueStr, true);
            }
        };

        public static final MvoSetType<Long> LONG = new MvoSetType<Long>(Long.class) {

            @Override public Long parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseLong(valueStr, true);
            }
        };

        public static final MvoSetType<Float> FLOAT = new MvoSetType<Float>(Float.class) {

            @Override public Float parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseFloat(valueStr, true);
            }
        };

        public static final MvoSetType<Double> DOUBLE = new MvoSetType<Double>(Double.class) {

            @Override public Double parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseDouble(valueStr, true);
            }
        };

        public static final MvoSetType<DateTime> DATETIME = new MvoSetType<DateTime>(DateTime.class) {

            @Override public DateTime parseElement(String valueStr) throws FormatException {
                return ValueResolver.parseDateTime(valueStr, true);
            }
        };

        public static final MvoSetType<UiTypeName> TYPE = new MvoSetType<UiTypeName>(UiTypeName.class) {

            @Override public UiTypeName parseElement(String str) {
                UiTypeName result = SkeltonTefService.instance().uiTypeNames().getByName(str);
                if (result == null) {
                    throw new ValueException("no such type: " + str);
                }
                return result;
            }
        };

        protected MvoSetType(Class<?> type) {
            super(type);
        }
    }

    public static class MvoMapType<K, V> extends AttributeType<MvoMap<K, V>> {

        private final ValueResolver<K> keyResolver_;
        private final ValueResolver<V> valueResolver_;

        public MvoMapType(ValueResolver<K> keyResolver, ValueResolver<V> valueResolver) {
            super(null, false);

            if (keyResolver == null || valueResolver == null) {
                throw new IllegalArgumentException();
            }

            keyResolver_ = keyResolver;
            valueResolver_ = valueResolver;
        }

        public ValueResolver<K> getKeyResolver() {
            return keyResolver_;
        }

        public Class<?> getKeyType() {
            return keyResolver_.getType();
        }

        public ValueResolver<V> getValueResolver() {
            return valueResolver_;
        }

        public Class<?> getValueType() {
            return valueResolver_.getType();
        }

        @Override public String format(MvoMap<K, V> obj) {
            return obj == null ? "" : obj.toString();
        }

        @Override public MvoMap<K, V> parse(String str) {
            throw new UnsupportedOperationException();
        }

        public K parseKey(VariableHolder context, String keyStr) throws FormatException {
            return keyResolver_.resolve(context, keyStr);
        }

        public V parseValue(VariableHolder context, String valueStr) throws FormatException {
            return valueResolver_.resolve(context, valueStr);
        }
    }

    private final boolean isValueSerializable_;
    private final Class<T> javaType_;

    protected AttributeType(Class<T> javaType) {
        this(javaType, javaType != null && Serializable.class.isAssignableFrom(javaType));
    }

    protected AttributeType(Class<T> javaType, boolean isValueSerializable) {
        isValueSerializable_ = isValueSerializable;
        javaType_ = javaType;
    }

    public boolean isValueSerializable() {
        return isValueSerializable_;
    }

    public Class<T> getJavaType() {
        return javaType_;
    }

    public abstract String format(T obj);
    public abstract T parse(String str);
}
