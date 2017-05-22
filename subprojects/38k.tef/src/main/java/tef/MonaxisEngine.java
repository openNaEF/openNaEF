package tef;

class MonaxisEngine<T> {

    protected static final class HistoryElement<T> {

        final Transaction transaction;
        private final T value_;

        HistoryElement(Transaction transaction, T value) {
            this.transaction = transaction;
            value_ = TefUtils.mutableGuard(value);
        }

        final T getValue() {
            return TefUtils.mutableGuard(value_);
        }
    }

    static interface HistoryElementProcessor {

        public void process(Transaction historyElementTransaction, Object historyElementValue);
    }

    private final List<HistoryElement<T>> history_;

    MonaxisEngine(TefService.RestoringMode restoringMode) {
        if (restoringMode == TefService.RestoringMode.RESTORING_BY_BULK) {
            history_ = null;
        } else if (restoringMode == null || restoringMode == TefService.RestoringMode.RESTORING_BY_JOURNAL) {
            history_ = new List<HistoryElement<T>>();
        } else {
            throw new IllegalArgumentException();
        }
    }

    final synchronized T getValue() {
        int index = getElementIndex(TransactionContext.getTargetVersion());
        return isVoidIndex(index) ? null : getElement(index).getValue();
    }

    static final class BulkSetInfo {
        final Transaction transaction;
        final Object value;

        BulkSetInfo(Transaction transaction, Object value) {
            this.transaction = transaction;
            this.value = value;
        }
    }

    final synchronized void bulkSet(BulkSetInfo[] bulkSetInfos) {
        throw new RuntimeException();
    }

    final synchronized void setValue(T value) {
        Transaction currentTx = TransactionContext.getContextTransaction();
        if (!currentTx.isRestoredTransaction()) {
            Transaction.validateWriteContext();
            MVO.phantomCheck(value);

            rollbackCurrentTxElems();
        }

        history_.add((HistoryElement<T>) currentTx.getMonaxisHistoryElement(value));
    }

    final synchronized void setVoid() {
        setValue(null);
        history_.add(null);
    }

    synchronized final void rollbackCurrentTxElems() {
        if (!isChangingByCurrentTx()) {
            return;
        }

        Transaction currentTx = TransactionContext.getContextTransaction();
        int index = getElementIndex((TransactionId.W) currentTx.getId());
        if (isVoidIndex(index)) {
            history_.removeLast();
            history_.removeLast();
        } else {
            history_.removeLast();
        }
    }

    synchronized final boolean isChangingByCurrentTx() {
        Transaction currentTx = TransactionContext.getContextTransaction();
        int index = getElementIndex((TransactionId.W) currentTx.getId());
        return 0 <= index && getElement(index).transaction == currentTx;
    }

    private boolean isAccessibleFromCurrentTransaction(HistoryElement<T> elem) {
        Transaction currentTx = TransactionContext.getContextTransaction();
        Transaction elemTx = elem.transaction;

        if (elemTx == currentTx) {
            return true;
        }
        if (!currentTx.isReadOnly() && elemTx.isActive()) {
            throw new Error();
        }
        if (elemTx.getId().serial <= currentTx.getBaseTransaction().getId().serial) {
            return true;
        }

        return false;
    }

    synchronized TransactionId.W getFirstTransactionId() {
        if (size() == 0) {
            return null;
        }

        HistoryElement<T> firstElem = getElement(0);
        if (!isAccessibleFromCurrentTransaction(firstElem)) {
            return null;
        }
        return (TransactionId.W) firstElem.transaction.getId();
    }

    synchronized TransactionId.W getLatestVersion() {
        for (int i = size() - 1; i >= 0; i--) {
            HistoryElement<T> elem = getElement(i);
            if (elem != null && isAccessibleFromCurrentTransaction(elem)) {
                return (TransactionId.W) elem.transaction.getId();
            }
        }
        return null;
    }

    @TimeDimensioned(TimeDimension.INDEFINITE)
    protected final synchronized void traverseHistory(
            HistoryElementProcessor historyElementProcessor,
            TransactionHistoryTraverseMode traverseMode) {
        int targetVersion = TransactionContext.getTargetVersion().serial;
        for (int i = 0; i < size(); i++) {
            HistoryElement<T> elem = getElement(i);
            if (elem == null) {
                continue;
            }
            if (!isAccessibleFromCurrentTransaction(elem)) {
                continue;
            }
            if (traverseMode == TransactionHistoryTraverseMode.AFFECTED_BY_TARGET_VERSION
                    && (targetVersion < elem.transaction.getId().serial)) {
                continue;
            }

            historyElementProcessor.process(elem.transaction, elem.getValue());
        }
    }

    final synchronized boolean isHistoryInitialized() {
        return history_ != null;
    }

    final synchronized HistoryElement<T> getElement(int index) {
        return history_.get(index);
    }

    final synchronized HistoryElement getLastElement() {
        return size() == 0
                ? null
                : getElement(size() - 1);
    }

    final synchronized void checkStrictMonotonicIncreasing() {
        HistoryElement successorElem = null;
        for (int i = size() - 1; i >= 0; i--) {
            HistoryElement elem = getElement(i);
            if (elem == null) {
                continue;
            }
            if (successorElem != null) {
                if (elem.transaction.getId().serial >= successorElem.transaction.getId().serial) {
                    throw new IllegalStateException();
                }
            }
            successorElem = elem;
        }
    }

    synchronized final boolean isExistingAt() {
        return !isVoidIndex(getElementIndex(TransactionContext.getTargetVersion()));
    }

    synchronized final boolean isVoidIndex(int elemIndex) {
        return elemIndex < 0
                || (elemIndex < size() - 1 && getElement(elemIndex + 1) == null);
    }

    synchronized final int getElementIndex(TransactionId.W version) {
        for (int i = size() - 1; i >= 0; i--) {
            HistoryElement elem = getElement(i);
            if (elem == null) {
                continue;
            }
            if (!isAccessibleFromCurrentTransaction(elem)) {
                continue;
            }

            if (elem.transaction.getId().serial <= version.serial) {
                return i;
            }
        }
        return -1;
    }

    synchronized final int size() {
        return history_.size();
    }
}
