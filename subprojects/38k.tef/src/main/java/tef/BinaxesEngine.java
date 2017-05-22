package tef;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

class BinaxesEngine<T> {

    static final class BinaxesArg {

        final long time;
        final Transaction transaction;

        BinaxesArg(long time, Transaction transaction) {
            this.time = time;
            this.transaction = transaction;
        }
    }

    static final class BinaxesArgsCache {

        private final Map<Transaction, BinaxesArg[]> map_ = new HashMap<Transaction, BinaxesArg[]>();

        private static final BinaxesArg[] ZERO_LENGTH_ARRAY_OF_BINAXESARG = new BinaxesArg[0];

        final BinaxesArg get(Transaction transaction, long targetTime) {
            BinaxesArg[] array = map_.get(transaction);
            if (array == null) {
                array = ZERO_LENGTH_ARRAY_OF_BINAXESARG;
            }

            for (int i = 0; i < array.length; i++) {
                if (array[i].time == targetTime) {
                    return array[i];
                }
            }

            BinaxesArg[] newArray = new BinaxesArg[array.length + 1];
            System.arraycopy(array, 0, newArray, 0, array.length);
            BinaxesArg binaxesArg = new BinaxesArg(targetTime, transaction);
            newArray[newArray.length - 1] = binaxesArg;
            map_.put(transaction, newArray);
            return binaxesArg;
        }
    }

    static final class HistoryElement<T> {

        final BinaxesArg binaxesArg;
        private final T value_;

        HistoryElement(BinaxesArg binaxesArg, T value) {
            this.binaxesArg = binaxesArg;
            value_ = TefUtils.mutableGuard(value);
        }

        final T getValue() {
            return TefUtils.mutableGuard(value_);
        }
    }

    static interface HistoryElementProcessor {

        public void process(
                Transaction historyElementTransaction, long historyElementTargetTime, Object historyElementValue);
    }

    private final List<HistoryElement<T>> history_;

    BinaxesEngine(TefService.RestoringMode restoringMode) {
        if (restoringMode == TefService.RestoringMode.RESTORING_BY_BULK) {
            history_ = null;
        } else if (restoringMode == null
                || restoringMode == TefService.RestoringMode.RESTORING_BY_JOURNAL) {
            history_ = new List<HistoryElement<T>>();
        } else {
            throw new IllegalArgumentException();
        }
    }

    final synchronized T getValue() {
        BinaxesArg binaxesArg = TransactionContext.getBinaxesArg();
        Transaction currentTransaction = binaxesArg.transaction;
        long currentTargetTime = binaxesArg.time;
        TransactionId.W targetVersion = TransactionContext.getTargetVersion();

        for (int i = history_.size() - 1; i >= 0; i--) {
            HistoryElement<T> historyElement = getHistoryElement(i);

            if (!isAccessibleFromCurrentTransaction(historyElement)) {
                continue;
            }

            Transaction historyElementTransaction = historyElement.binaxesArg.transaction;
            long historyElementTargetTime = historyElement.binaxesArg.time;

            if (currentTransaction == historyElementTransaction) {
                if (targetVersion.serial < currentTransaction.getId().serial) {
                    continue;
                }
            } else if (targetVersion.serial < historyElementTransaction.getId().serial) {
                continue;
            }

            if (currentTargetTime >= historyElementTargetTime) {
                return historyElement.getValue();
            }
        }
        return null;
    }

    static final class BulkSetInfo {
        final Transaction transaction;
        final long targetTime;
        final Object value;

        BulkSetInfo(Transaction transaction, long targetTime, Object value) {
            this.transaction = transaction;
            this.targetTime = targetTime;
            this.value = value;
        }
    }

    final synchronized void bulkSet(BulkSetInfo[] bulkSetInfos, BinaxesArgsCache binaxesArgsCache) {
        throw new RuntimeException();
    }

    final synchronized void setValue(T value) {
        Transaction.validateWriteContext();
        MVO.phantomCheck(value);

        addHistory(value);
    }

    private final synchronized void addHistory(T value) {
        BinaxesArg binaxesArg = TransactionContext.getBinaxesArg();
        long targetTime = binaxesArg.time;
        Transaction currentTransaction = TransactionContext.getContextTransaction();
        if (binaxesArg.transaction != currentTransaction) {
            throw new IllegalStateException();
        }

        HistoryElement<T> historyElement
                = (HistoryElement<T>) currentTransaction.getBinaxesHistoryElement(binaxesArg, value);

        if (history_.size() == 0) {
            history_.add(historyElement);
        } else if (getHistoryElement(history_.size() - 1).binaxesArg.transaction != currentTransaction) {
            if (getHistoryElement(history_.size() - 1).binaxesArg.transaction.getId().serial
                    > currentTransaction.getId().serial) {
                throw new SuperSerializableException();
            }

            history_.add(historyElement);
        } else {
            for (int i = history_.size() - 1; i >= 0; i--) {
                HistoryElement<T> element = getHistoryElement(i);
                if (element.binaxesArg.transaction != currentTransaction) {
                    break;
                }
                if (element.binaxesArg.time < targetTime) {
                    break;
                }

                history_.removeLast();
            }

            history_.add(historyElement);
        }
    }

    final synchronized java.util.List<HistoryElement> getCommitTargets() {
        Transaction currentTransaction = TransactionContext.getContextTransaction();
        boolean isUncommittedHistorySequence = false;
        java.util.List<HistoryElement> result = new java.util.ArrayList<HistoryElement>();
        for (int i = 0; i < history_.size(); i++) {
            HistoryElement<T> historyElement = getHistoryElement(i);
            if (historyElement.binaxesArg.transaction != currentTransaction) {
                if (isUncommittedHistorySequence) {
                    throw new IllegalStateException();
                }
            } else {
                isUncommittedHistorySequence = true;
                result.add(historyElement);
            }
        }
        return result;
    }

    synchronized void rollback() {
        Transaction currentTransaction = TransactionContext.getContextTransaction();
        for (int i = history_.size() - 1; i >= 0; i--) {
            HistoryElement<T> element = getHistoryElement(i);
            if (element.binaxesArg.transaction.getId().serial
                    > currentTransaction.getId().serial) {
                throw new IllegalStateException();
            }
            if (element.binaxesArg.transaction.getId().serial
                    < currentTransaction.getId().serial) {
                break;
            }

            if (element.binaxesArg.transaction.getId().serial
                    != currentTransaction.getId().serial) {
                throw new IllegalStateException();
            }

            history_.removeLast();
        }
    }

    private boolean isAccessibleFromCurrentTransaction(HistoryElement<T> historyElement) {
        Transaction currentTransaction = TransactionContext.getContextTransaction();
        Transaction historyElementTransaction = historyElement.binaxesArg.transaction;

        if (historyElementTransaction == currentTransaction) {
            return true;
        }
        if (!currentTransaction.isReadOnly()
                && historyElementTransaction.isActive()) {
            throw new Error();
        }
        if (historyElementTransaction.getId().serial
                <= currentTransaction.getBaseTransaction().getId().serial) {
            return true;
        }

        return false;
    }

    synchronized TransactionId.W getFirstTransactionId() {
        if (history_.size() == 0) {
            return null;
        }

        HistoryElement<T> firstHistoryElement = getHistoryElement(0);
        if (!isAccessibleFromCurrentTransaction(firstHistoryElement)) {
            return null;
        }
        return (TransactionId.W) firstHistoryElement.binaxesArg.transaction.getId();
    }

    synchronized TransactionId.W getLatestVersion() {
        for (int i = history_.size() - 1; i >= 0; i--) {
            HistoryElement<T> historyElement = getHistoryElement(i);
            if (isAccessibleFromCurrentTransaction(historyElement)) {
                return (TransactionId.W) historyElement.binaxesArg.transaction.getId();
            }
        }
        return null;
    }

    synchronized SortedMap<Long, T> getChanges() {
        SortedMap<Long, T> rawChanges = new TreeMap<Long, T>();
        long oldest = Long.MAX_VALUE;
        Transaction currentTransaction = TransactionContext.getContextTransaction();
        TransactionId.W targetVersion = TransactionContext.getTargetVersion();

        for (int i = history_.size() - 1; i >= 0; i--) {
            HistoryElement<T> historyElement = getHistoryElement(i);
            if (!isAccessibleFromCurrentTransaction(historyElement)) {
                continue;
            }

            Transaction historyElementTransaction = historyElement.binaxesArg.transaction;
            long historyElementTargetTime = historyElement.binaxesArg.time;

            if (currentTransaction == historyElementTransaction) {
                if (targetVersion.serial < currentTransaction.getId().serial) {
                    continue;
                }
            } else if (targetVersion.serial < historyElementTransaction.getId().serial) {
                continue;
            }

            if (historyElementTargetTime < oldest) {
                rawChanges.put(historyElementTargetTime, historyElement.getValue());
                oldest = historyElementTargetTime;
            }
        }

        SortedMap<Long, T> result = new TreeMap<Long, T>();
        T previousValue = null;
        for (Map.Entry<Long, T> entry : rawChanges.entrySet()) {
            Long time = entry.getKey();
            T value = entry.getValue();
            if (TefUtils.equals(previousValue, value)) {
                continue;
            }

            result.put(time, value);

            previousValue = value;
        }

        return result;
    }

    @TimeDimensioned(TimeDimension.INDEFINITE)
    protected final synchronized void traverseHistory(
            HistoryElementProcessor historyElementProcessor, TransactionHistoryTraverseMode traverseMode) {
        int targetVersion = TransactionContext.getTargetVersion().serial;
        for (int i = 0; i < history_.size(); i++) {
            HistoryElement<T> historyElement = getHistoryElement(i);
            if (!isAccessibleFromCurrentTransaction(historyElement)) {
                continue;
            }
            if (traverseMode == TransactionHistoryTraverseMode.AFFECTED_BY_TARGET_VERSION
                    && (targetVersion < historyElement.binaxesArg.transaction.getId().serial)) {
                continue;
            }

            historyElementProcessor.process(
                    historyElement.binaxesArg.transaction,
                    historyElement.binaxesArg.time,
                    historyElement.getValue());
        }
    }

    final synchronized boolean isHistoryInitialized() {
        return history_ != null;
    }

    private synchronized HistoryElement<T> getHistoryElement(int index) {
        return history_.get(index);
    }

    boolean isExistingAt() {
        TransactionId.W firstTransactionId = getFirstTransactionId();
        return firstTransactionId != null
                && (firstTransactionId.serial <= TransactionContext.getTargetVersion().serial);
    }
}
