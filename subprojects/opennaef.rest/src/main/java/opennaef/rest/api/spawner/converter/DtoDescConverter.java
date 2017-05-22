package opennaef.rest.api.spawner.converter;

import opennaef.rest.api.spawner.ConvertVariable;
import opennaef.rest.api.spawner.DtoSpawner;
import tef.DateTime;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.dto.MvoDtoDesc;

import java.util.Map;

/**
 * Dto参照を変換する
 */
@Converter
public class DtoDescConverter implements TxValueConverter<Map<?, ?>> {
    @Override
    public boolean accept(Object value) {
        return value instanceof MvoDtoDesc;
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
        // Dtoの子要素までDto化してしまうと無限ループするため、Dtoと同一階層のもののみをDtoへ変換する
        long depth = ConvertVariable.derefStart();
        if (ConvertVariable.deref() && depth == ConvertVariable.derefDepth()) {
            return DtoConverter.INSTANCE.convert(DtoSpawner.spawn((MvoDtoDesc) value), time, tx);
        }
        return DtoSpawner.createMvoLink((MvoDtoDesc) value, time, tx);
    }
}
