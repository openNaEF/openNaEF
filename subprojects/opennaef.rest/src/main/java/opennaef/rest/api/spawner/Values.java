package opennaef.rest.api.spawner;

import opennaef.rest.api.spawner.converter.Converts;
import opennaef.rest.api.spawner.converter.TxValueConverter;
import opennaef.rest.api.spawner.converter.ValueConverter;
import tef.DateTime;
import tef.TransactionId;

/**
 * JSONで返すべき値へ変換する処理をまとめるクラス
 */
public class Values {
    /**
     * value の型からJSONで返すべき値へ変換する
     *
     * @param value
     * @return
     */
    public static Object toValue(Object value) {
        try {
            return Converts.getConverter(value).convert(value);
        } catch (ClassNotFoundException e) {
            return "!!!!!!!!!!!!!!!!!!!!!" + value.getClass().getCanonicalName();
        }
    }

    /**
     * value の型からJSONで返すべき値へ変換する
     *
     * @param value
     * @return
     */
    public static Object toValue(Object value, DateTime time, TransactionId.W tx) {
        try {
            ValueConverter<?> converter = Converts.getConverter(value);
            if (converter instanceof TxValueConverter) {
                return ((TxValueConverter) converter).convert(value, time, tx);
            } else {
                return converter.convert(value);
            }
        } catch (ClassNotFoundException e) {
            return "!!!!!!!!!!!!!!!!!!!!!" + value.getClass().getCanonicalName();
        }
    }
}
