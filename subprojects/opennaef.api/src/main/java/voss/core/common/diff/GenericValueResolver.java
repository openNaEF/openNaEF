package voss.core.common.diff;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenericValueResolver<T> implements ValueResolver<T> {
    private final String keyMethodName;
    private final String valueMethodName;

    public GenericValueResolver(String keyMethodName) {
        this(keyMethodName, "toString");
    }

    public GenericValueResolver(String keyMethodName, String valueMethodName) {
        if (keyMethodName == null) {
            throw new IllegalArgumentException("keyMethodName is null.");
        } else if (valueMethodName == null) {
            throw new IllegalArgumentException("valueMethodName is null.");
        }
        this.keyMethodName = keyMethodName;
        this.valueMethodName = valueMethodName;
    }

    @Override
    public String getKey(T object) {
        return getResult(this.keyMethodName, object);
    }

    public String getValue(T object) {
        return getResult(this.valueMethodName, object);
    }

    private String getResult(String methodName, T object) {
        if (object == null) {
            return null;
        }
        try {
            Method method = object.getClass().getMethod(methodName, new Class<?>[0]);
            method.setAccessible(true);
            Object value = method.invoke(object, new Object[0]);
            if (value == null) {
                return null;
            } else if (value instanceof String) {
                return (String) value;
            } else {
                return value.toString();
            }
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

}