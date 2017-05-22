package opennaef.rest.api.spawner.converter;

/**
 * null を変換する
 */
@Converter(priority = Integer.MAX_VALUE)
public class NullConverter implements ValueConverter<Object> {
    public static final NullConverter INSTANCE = new NullConverter();

    @Override
    public boolean accept(Object value) {
        return value == null;
    }

    @Override
    public Object convert(Object value) {
        return null;
    }
}
