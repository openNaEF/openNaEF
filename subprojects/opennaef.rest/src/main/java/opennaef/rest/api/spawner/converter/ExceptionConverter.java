package opennaef.rest.api.spawner.converter;

/**
 * Throwableを {simple-name}:{message} へ変換する
 */
@Converter
public class ExceptionConverter implements ValueConverter<String> {
    @Override
    public boolean accept(Object value) {
        return value instanceof Throwable;
    }

    @Override
    public String convert(Object value) {
        Throwable t = (Throwable) value;
        return t.getClass().getSimpleName() +
                ":" +
                t.getMessage();
    }
}
