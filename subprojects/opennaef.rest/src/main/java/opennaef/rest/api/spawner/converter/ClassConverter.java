package opennaef.rest.api.spawner.converter;

/**
 * Classをクラス名へ変換する
 */
public class ClassConverter implements ValueConverter<String> {
    @Override
    public boolean accept(Object value) {
        return value instanceof Class;
    }

    @Override
    public String convert(Object value) {
        return ((Class) value).getSimpleName();
    }
}
