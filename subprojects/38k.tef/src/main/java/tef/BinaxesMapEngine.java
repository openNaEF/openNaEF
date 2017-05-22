package tef;

import java.util.*;
import java.util.List;

class BinaxesMapEngine<K, V> {

    static interface BinaxesMapHistoryElementProcessor {

        public void preprocessEntry(Object key);

        public void postprocessEntry(Object key);

        public BinaxesEngine.HistoryElementProcessor getElementHistoryProcessor();
    }

    static final class BinaxesMapBulkSetInfo {

        final Object key;
        final BinaxesEngine.BulkSetInfo[] valueBulkSetInfos;

        BinaxesMapBulkSetInfo(Object key, BinaxesEngine.BulkSetInfo[] valueBulkSetInfos) {
            this.key = key;
            this.valueBulkSetInfos = valueBulkSetInfos;
        }
    }

    private static final Comparator<Object> entryKeyComparator__ = new ObjectComparator();

    private final SortedMap<K, BinaxesEngine<V>> entries_;

    protected BinaxesMapEngine() {
        TefService.RestoringMode restoringMode = TefService.instance().getRestoringMode();
        if (restoringMode == TefService.RestoringMode.RESTORING_BY_BULK) {
            entries_ = null;
        } else if (restoringMode == null || restoringMode == TefService.RestoringMode.RESTORING_BY_JOURNAL) {
            entries_ = new TreeMap<K, BinaxesEngine<V>>(entryKeyComparator__);
        } else {
            throw new IllegalArgumentException();
        }
    }

    protected final synchronized boolean isEntriesInitialized() {
        return entries_ != null;
    }

    final synchronized V getValue(K key) {
        MVO.phantomCheck(key);

        BinaxesEngine<V> engine = getEngine(key);
        if (engine == null || (!engine.isExistingAt())) {
            return null;
        }

        return engine.getValue();
    }

    final synchronized List<K> getKeysList() {
        List<K> result = new ArrayList<K>();
        for (Map.Entry<K, BinaxesEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesEngine<V> engine = entry.getValue();
            if (!engine.isExistingAt()) {
                continue;
            }

            result.add(mutableGuard(key));
        }
        return result;
    }

    final synchronized List<K> getKeysListOf(Object value) {
        List<K> result = new ArrayList<K>();
        for (Map.Entry<K, BinaxesEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesEngine<V> engine = entry.getValue();
            if (!engine.isExistingAt()) {
                continue;
            }

            V entryValue = engine.getValue();
            if (TefUtils.equals(value, entryValue)) {
                result.add(mutableGuard(key));
            }
        }
        return result;
    }

    private synchronized BinaxesEngine<V> addEntry(K key) {
        BinaxesEngine<V> engine = new BinaxesEngine<V>(null);
        entries_.put(key, engine);
        return engine;
    }

    final synchronized void putEntry(K key, V value) {
        Transaction.validateWriteContext();

        key = mutableGuard(key);
        value = mutableGuard(value);

        MVO.phantomCheck(key);
        MVO.phantomCheck(value);

        BinaxesEngine<V> engine = getEngine(key);
        if (engine == null) {
            engine = addEntry(key);
        }

        engine.setValue(value);
    }

    final synchronized void removeEntry(K key) {
        Transaction.validateWriteContext();

        key = mutableGuard(key);

        BinaxesEngine<V> engine = getEngine(key);
        if (engine == null) {
            if (TransactionContext.getContextTransaction().isRestoredTransaction()) {
                engine = addEntry(key);
            } else {
                throw new RuntimeException();
            }
        }

        engine.setValue(null);
    }

    final synchronized void bulkSet(
            BinaxesMapBulkSetInfo[] bulkInfos,
            BinaxesEngine.BinaxesArgsCache binaxesArgsCache) {
        throw new RuntimeException();
    }

    final synchronized void commit(MVO.MvoField field, JournalWriter.BinaxesMapLogger logger) {
        for (Map.Entry<K, BinaxesEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesEngine<V> engine = entry.getValue();

            for (BinaxesEngine.HistoryElement commitTarget : engine.getCommitTargets()) {
                logger.write(field, commitTarget.binaxesArg.time, key, commitTarget.getValue());
            }
        }
    }

    final synchronized void rollback() {
        List<K> removes = new ArrayList<K>();
        for (Map.Entry<K, BinaxesEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesEngine<V> engine = entry.getValue();
            engine.rollback();

            if (engine.getLatestVersion() == null) {
                removes.add(key);
            }
        }
        for (K remove : removes) {
            entries_.remove(remove);
        }
    }

    synchronized TransactionId.W getFirstTransactionId() {
        TransactionId.W result = null;
        for (Map.Entry<K, BinaxesEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesEngine<V> engine = entry.getValue();

            TransactionId.W entriesFirstTransactionId = engine.getFirstTransactionId();
            if (entriesFirstTransactionId != null
                    && (result == null || entriesFirstTransactionId.serial < result.serial)) {
                result = entriesFirstTransactionId;
            }
        }
        return result;
    }

    synchronized TransactionId.W getLatestVersion() {
        TransactionId.W result = null;
        for (Map.Entry<K, BinaxesEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesEngine<V> engine = entry.getValue();

            TransactionId.W entriesLatestTransactionId = engine.getLatestVersion();
            if (entriesLatestTransactionId != null
                    && (result == null || entriesLatestTransactionId.serial > result.serial)) {
                result = entriesLatestTransactionId;
            }
        }
        return result;
    }

    @TimeDimensioned(TimeDimension.INDEFINITE)
    protected final synchronized void traverseHistory(
            BinaxesMapHistoryElementProcessor processor,
            TransactionHistoryTraverseMode traverseMode) {
        for (Map.Entry<K, BinaxesEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesEngine<V> engine = entry.getValue();

            processor.preprocessEntry(key);
            engine.traverseHistory(processor.getElementHistoryProcessor(), traverseMode);
            processor.postprocessEntry(key);
        }
    }

    protected synchronized List<Long> getChangeOccurrenceTimesSet() {
        SortedSet<Long> changeOccurrenceTimes = new TreeSet<Long>();
        for (Map.Entry<K, BinaxesEngine<V>> entry : entries_.entrySet()) {
            BinaxesEngine<V> engine = entry.getValue();

            for (Long time : engine.getChanges().keySet()) {
                changeOccurrenceTimes.add(time);
            }
        }
        return new ArrayList<Long>(changeOccurrenceTimes);
    }

    protected synchronized SortedMap<Long, V> getChanges(K key) {
        BinaxesEngine<V> engine = getEngine(key);
        return engine == null ? null : engine.getChanges();
    }

    private synchronized BinaxesEngine<V> getEngine(K key) {
        return entries_.get(key);
    }

    private static <T> T mutableGuard(T o) {
        return TefUtils.mutableGuard(o);
    }

    boolean isExistingAt() {
        TransactionId.W firstTransactionId = getFirstTransactionId();
        return firstTransactionId != null
                && firstTransactionId.serial <= TransactionContext.getTargetVersion().serial;
    }
}
