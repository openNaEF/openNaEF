package opennaef.rest.api.spawner.converter;

import opennaef.rest.api.spawner.Values;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapを変換する
 */
@Converter
public class MapConverter implements ValueConverter<Map<?, ?>> {
    @Override
    public boolean accept(Object value) {
        return value instanceof Map;
    }

    @Override
    public Map<?, ?> convert(Object value) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entity : ((Map<?, ?>) value).entrySet()) {
            Object key = Values.toValue(entity.getKey());
            Object vvv = Values.toValue(entity.getValue());
            map.put(key, vvv);
        }
        return map;
    }
}
