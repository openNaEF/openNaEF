package opennaef.rest.api.spawner.converter;

import naef.mvo.PortMode;

/**
 * PortModeをStringへ変換する
 */
@Converter
public class PortModeConverter implements ValueConverter<String> {
    @Override
    public boolean accept(Object value) {
        return value instanceof PortMode;
    }

    @Override
    public String convert(Object value) {
        return ((PortMode) value).name();
    }
}
