package opennaef.rest.api.spawner.converter;

import tef.DateTime;
import tef.TransactionId;

/**
 * naef のトランザクションの情報を必要とするValueConverter
 */
public interface TxValueConverter<R> extends ValueConverter<R> {
    /**
     * value からJSONで返すべき値へ変換する
     *
     * @param value
     * @return JSONで返すべき値
     */
    R convert(Object value, DateTime time, TransactionId.W tx);
}
