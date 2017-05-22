package opennaef.rest.api.spawner.converter;

import java.util.Objects;

/**
 * primitive型をStringへ変換する
 */
@Converter
public class PrimitiveTypeConverter implements ValueConverter<String> {
    @Override
    public boolean accept(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Character
                || value instanceof Boolean;
    }

    @Override
    public String convert(Object value) {
        return Objects.toString(value, null);
    }
}
