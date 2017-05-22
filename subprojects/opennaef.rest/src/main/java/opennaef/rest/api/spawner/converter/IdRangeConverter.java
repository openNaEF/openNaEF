package opennaef.rest.api.spawner.converter;

import naef.dto.IdRange;

import java.util.Objects;

/**
 * IdRangeをStringへ変換する
 */
@Converter
public class IdRangeConverter implements ValueConverter<String> {
    @Override
    public boolean accept(Object value) {
        return value instanceof IdRange;
    }

    @Override
    public String convert(Object value) {
        IdRange range = (IdRange) value;
        StringBuilder sb = new StringBuilder();
        sb.append(Objects.toString(range.lowerBound, "?"));
        sb.append("-");
        sb.append(Objects.toString(range.upperBound, "?"));
        return sb.toString();
    }
}
