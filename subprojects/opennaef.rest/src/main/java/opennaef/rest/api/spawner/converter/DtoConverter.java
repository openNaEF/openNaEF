package opennaef.rest.api.spawner.converter;

import opennaef.rest.api.spawner.ConvertVariable;
import opennaef.rest.api.spawner.DtoSpawner;
import tef.DateTime;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.dto.EntityDto;

import java.util.Map;

/**
 * Dtoを変換する
 */
@Converter
public class DtoConverter implements TxValueConverter<Map<?, ?>> {
    public static final DtoConverter INSTANCE = new DtoConverter();

    @Override
    public boolean accept(Object value) {
        return value instanceof EntityDto;
    }

    @Override
    public Map<?, ?> convert(Object value) {
        DateTime time = null;
        TransactionId.W tx = null;
        if (TransactionContext.isTransactionRunning()) {
            time = new DateTime(TransactionContext.getTargetTime());
            tx = TransactionContext.getTargetVersion();
        }
        return convert(value, time, tx);
    }

    @Override
    public Map<?, ?> convert(Object value, DateTime time, TransactionId.W tx) {
        ConvertVariable.depthIncrement();
        Map<?, ?> map = DtoSpawner.toMap((EntityDto) value);
        ConvertVariable.depthDecrement();
        return map;
    }
}
