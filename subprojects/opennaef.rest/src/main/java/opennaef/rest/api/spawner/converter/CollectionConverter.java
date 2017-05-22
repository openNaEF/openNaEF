package opennaef.rest.api.spawner.converter;

import opennaef.rest.api.spawner.Values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collectionを変換する
 */
@Converter
public class CollectionConverter implements ValueConverter<List<?>> {
    @Override
    public boolean accept(Object value) {
        return value instanceof Collection;
    }

    @Override
    public List<?> convert(Object value) {
        List<Object> result = new ArrayList<>();
        for (Object element : (Collection) value) {
            result.add(Values.toValue(element));
        }
        return result;
    }
}
