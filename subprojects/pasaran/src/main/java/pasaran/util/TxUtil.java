package pasaran.util;

import naef.NaefTefService;
import tef.*;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class TxUtil {
    private TxUtil() { }

    public static TransactionId.R beginReadTx(String timeString, String versionString) {
        TransactionId.R readTx = TransactionContext.beginReadTransaction();
        // トランザクションのversionを指定する
        TxUtil.setTargetVersion(versionString);

        // トランザクションの時間を指定する
        TxUtil.setTargetTime(timeString);

        return readTx;
    }

    public static void closeTx() {
        TransactionContext.close();
    }

    public static void setTargetTime(String timeString) {
        Long targetTime = null;
        if(timeString != null) {
            targetTime = Long.parseLong(timeString);
//            if (System.currentTimeMillis() > targetTime) {
//                // 過去時間の場合はバージョン軸へ変換する
//                TransactionId.W targetVersion = timeToVersion(targetTime);
//                TransactionContext.setTargetVersion(targetVersion);
//            } else {
//                TransactionContext.setTargetTime(Long.parseLong(timeString));
//            }

            TransactionContext.setTargetTime(targetTime);
        }
    }

    public static void setTargetVersion(String versionString) {
        TransactionId.W targetVersion = null;
        if(versionString != null) {
            targetVersion = (TransactionId.W) TransactionId.W.getInstance(versionString);
        }
        TransactionContext.setTargetVersion(targetVersion);
    }

    @Deprecated
    public static TransactionId.W timeToVersion(long time) {
        MvoRegistry mvoRegistry = NaefTefService.instance().getMvoRegistry();

        Set<TransactionId.W> versions = new TreeSet<>();
        for(MVO mvo : mvoRegistry.list()) {
            for (TransactionId.W w : TransactionIdAggregator.getTransactionIds(mvo)) {
                versions.add(w);
            }
        }

        Iterator<TransactionId.W> it = versions.iterator();
        TransactionId.W current = it.next();
        TransactionId.W prev = current;
        while(it.hasNext()) {
            current = it.next();
            long commitTime = TransactionContext.getTransactionCommittedTime(current);
            if(commitTime <= time) {
                prev = current;
            } else {
                return prev;
            }
        }

        return current;
    }
}
