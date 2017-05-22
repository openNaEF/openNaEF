package opennaef.rest.api.resource;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Objects;

/**
 * QueryParam の "time" を Date へ変換する
 * <p>
 * Epoch Time Millis を Date へ変換する
 * <p>
 * 値が null の場合は null を返す
 */
@Provider
public class ToDateTime implements ParamConverterProvider {
    private static final ParamConverter<Date> INSTANCE = new Converter();

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        return Date.class.isAssignableFrom(rawType) ? (ParamConverter<T>) INSTANCE : null;
    }

    private static class Converter implements ParamConverter<Date> {
        @Override
        public Date fromString(String value) {
            if (value == null) return null;
            try {
                Long time = Long.parseLong(value);
                return new Date(time);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String toString(Date value) {
            if (value == null) return null;
            return Objects.toString(value.getTime(), null);
        }
    }
}


