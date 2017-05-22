package voss.core.common.diff;

public interface ValueResolver<T> {
    String getKey(T object);

    String getValue(T object);
}