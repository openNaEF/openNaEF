package opennaef.rest.api.resource;

import tef.TransactionId;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * QueryParam の "version" を TransactionId.W へ変換する
 * <p>
 * 形式は ^x[0-9A-Fa-f].+$
 * <p>
 * 値が null の場合は null を返す
 */
@Provider
public class ToTransactionId implements ParamConverterProvider {
    private static final ParamConverter<TransactionId.W> INSTANCE = new Converter();

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        return TransactionId.W.class.isAssignableFrom(rawType) ? (ParamConverter<T>) INSTANCE : null;
    }

    private static class Converter implements ParamConverter<TransactionId.W> {
        @Override
        public TransactionId.W fromString(String value) {
            if (value == null) return null;
            try {
                TransactionId version = TransactionId.getInstance(value);
                return version instanceof TransactionId.W ? (TransactionId.W) version : null;
            } catch (RuntimeException e) {
                // 変換失敗 不正な文字列が含まれている
                return null;
            }
        }

        @Override
        public String toString(TransactionId.W value) {
            if (value == null) return null;
            return value.getIdString();
        }
    }
}

