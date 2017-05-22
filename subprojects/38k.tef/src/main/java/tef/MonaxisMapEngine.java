package tef;

import java.util.*;
import java.util.List;

class MonaxisMapEngine<K, V> {

    static interface MonaxisMapHistoryElementProcessor {

        public void preprocessEntry(Object key);

        public void postprocessEntry(Object key);

        public MonaxisEngine.HistoryElementProcessor getElementHistoryProcessor();
    }

    static final class MonaxisMapBulkSetInfo {

        final Object key;
        final MonaxisEngine.BulkSetInfo[] valueBulkSetInfos;

        MonaxisMapBulkSetInfo(Object key, MonaxisEngine.BulkSetInfo[] valueBulkSetInfos) {
            this.key = key;
            this.valueBulkSetInfos = valueBulkSetInfos;
        }
    }

    private static final Comparator<Object> entryKeyComparator__ = new ObjectComparator();

    private SortedMap<K, MonaxisEngine<V>> entries_;

    protected MonaxisMapEngine() {
        TefService.RestoringMode restoringMode = TefService.instance().getRestoringMode();
        if (restoringMode == TefService.RestoringMode.RESTORING_BY_BULK) {
            entries_ = null;
        } else if (restoringMode == null || restoringMode == TefService.RestoringMode.RESTORING_BY_JOURNAL) {
            entries_ = new TreeMap<K, MonaxisEngine<V>>(entryKeyComparator__);
        } else {
            throw new IllegalArgumentException();
        }
    }

    protected final synchronized boolean isEntriesInitialized() {
        return entries_ != null;
    }

    final synchronized V getValue(K key) {
        MVO.phantomCheck(key);

        MonaxisEngine<V> engine = getEngine(key);
        if (engine == null || (!engine.isExistingAt())) {
            return null;
        }

        return engine.getValue();
    }

    final synchronized Set<K> getKeysSet() {
        Set<K> result = new LinkedHashSet<K>();
        for (Map.Entry<K, MonaxisEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            MonaxisEngine<V> engine = entry.getValue();
            if (!engine.isExistingAt()) {
                continue;
            }

            result.add(mutableGuard(key));
        }
        return result;
    }

    final synchronized Set<K> getKeysSetOf(V value) {
        Set<K> result = new LinkedHashSet<K>();
        for (Map.Entry<K, MonaxisEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            MonaxisEngine<V> engine = entry.getValue();
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

    private MonaxisEngine<V> addEntry(K key) {
        MonaxisEngine<V> engine = new MonaxisEngine<V>(null);
        entries_.put(key, engine);
        return engine;
    }

    final synchronized void putEntry(K key, V value) {
        Transaction.validateWriteContext();

        key = mutableGuard(key);
        value = mutableGuard(value);

        MVO.phantomCheck(key);
        MVO.phantomCheck(value);

        MonaxisEngine<V> engine = getEngine(key);
        if (engine == null) {
            engine = addEntry(key);
        }

        engine.setValue(value);
    }

    final synchronized void removeEntry(K key) {
        Transaction.validateWriteContext();

        key = mutableGuard(key);

        MonaxisEngine<V> engine = getEngine(key);
        if (engine == null) {
            if (TransactionContext.getContextTransaction().isRestoredTransaction()) {
                engine = addEntry(key);
            } else {
                throw new RuntimeException();
            }
        }

        engine.setVoid();
    }

    final synchronized void bulkSet(MonaxisMapBulkSetInfo[] bulkInfos) {
        throw new RuntimeException();
    }

    final synchronized void commit(MVO.MvoField field, JournalWriter.MonaxisMapLogger logger) {
        Transaction currentTx = TransactionContext.getContextTransaction();
        for (Map.Entry<K, MonaxisEngine<V>> entry : entries_.entrySet()) {
            MonaxisEngine<V> engine = entry.getValue();
            if (!engine.isChangingByCurrentTx()) {
                continue;
            }

            int index = engine.getElementIndex((TransactionId.W) currentTx.getId());
            K key = entry.getKey();
            if (engine.isVoidIndex(index)) {
                logger.writeKeyVoid(field, key);
            } else {
                logger.write(field, key, engine.getElement(index).getValue());
            }

            engine.checkStrictMonotonicIncreasing();
        }
    }

    final synchronized void rollback() {
        List<K> removes = new ArrayList<K>();
        for (Map.Entry<K, MonaxisEngine<V>> entry : entries_.entrySet()) {
            MonaxisEngine engine = entry.getValue();
            if (!engine.isChangingByCurrentTx()) {
                continue;
            }

            engine.rollbackCurrentTxElems();

            if (engine.size() == 0) {
                removes.add(entry.getKey());
            }

            engine.checkStrictMonotonicIncreasing();
        }
        for (K remove : removes) {
            entries_.remove(remove);
        }
    }

    synchronized TransactionId.W getLatestVersion() {
        TransactionId.W result = null;
        for (Map.Entry<K, MonaxisEngine<V>> entry : entries_.entrySet()) {
            MonaxisEngine<V> engine = entry.getValue();

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
            MonaxisMapHistoryElementProcessor processor,
            TransactionHistoryTraverseMode traverseMode) {
        for (Map.Entry<K, MonaxisEngine<V>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            MonaxisEngine<V> engine = entry.getValue();

            processor.preprocessEntry(key);
            engine.traverseHistory(processor.getElementHistoryProcessor(), traverseMode);
            processor.postprocessEntry(key);
        }
    }

    private MonaxisEngine<V> getEngine(K key) {
        return entries_.get(key);
    }

    final synchronized boolean hasEntry(K key) {
        MonaxisEngine<V> engine = getEngine(key);
        return engine != null && engine.isExistingAt();
    }

    private static <T> T mutableGuard(T o) {
        return TefUtils.mutableGuard(o);
    }
}
