package opennaef.rest.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.TransactionContext;
import tef.TransactionId;

import java.util.Date;

/**
 * naefのread-transactionをtry-with-resources文で使えるようにするクラス
 */
public class AutoCloseableTx implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AutoCloseableTx.class);
    private TransactionId.R _tx;

    private final Long _targetTime;
    private final TransactionId.W _targetVersion;


    /**
     * @param targetTime         ターゲットとなる時間
     * @param targetVersion      ターゲットとなるバージョン
     * @param beginTxImmediately trueの場合には自動でbeginTx()を実行する
     */
    private AutoCloseableTx(Long targetTime, TransactionId.W targetVersion, boolean beginTxImmediately) {
        _targetTime = targetTime;
        _targetVersion = targetVersion;
        if (beginTxImmediately) {
            begenTx();
        }
    }

    public static AutoCloseableTx beginTx(Long targetTime) {
        return beginTx(targetTime, null);
    }

    public static AutoCloseableTx beginTx(Long targetTime, TransactionId.W targetVersion) {
        return new AutoCloseableTx(targetTime, targetVersion, true);
    }

    public static AutoCloseableTx beginTx(Date targetTime) {
        return beginTx(targetTime, null);
    }

    public static AutoCloseableTx beginTx(Date targetTime, TransactionId.W targetVersion) {
        Long time = targetTime != null ? targetTime.getTime() : null;
        return beginTx(time, targetVersion);
    }

    private TransactionId.R begenTx() {
//        if (TransactionContext.isTransactionRunning()) {
//            throw new IllegalStateException("read-tx nested.");
//        }

        _tx = TransactionContext.beginReadTransaction();
        TransactionContext.setTargetTime(_targetTime);
        TransactionContext.setTargetVersion(_targetVersion);

        log.debug("[TX:{}] ____ begin.", _tx.toString());
        return _tx;
    }

    @Override
    public void close() {
        log.debug("[TX:{}] ^^^^ close.", _tx.toString());
        TransactionContext.close();
    }

    public Long targetTime() {
        return _targetTime;
    }

    public TransactionId.W targetVersion() {
        return _targetVersion;
    }
}
