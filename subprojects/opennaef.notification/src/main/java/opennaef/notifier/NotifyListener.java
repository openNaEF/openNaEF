package opennaef.notifier;

import tef.TransactionId;
import tef.skelton.dto.DtoChanges;

/**
 * NaefSubscriber から DtoChanges を受け取る Listener
 */
public interface NotifyListener {
    void transactionCommitted(final TransactionId txId, final DtoChanges dtoChanges);
}
