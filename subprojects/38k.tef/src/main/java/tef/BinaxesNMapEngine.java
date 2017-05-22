package tef;

import java.util.*;
import java.util.List;

class BinaxesNMapEngine<K, V> {

    static interface BinaxesNMapHistoryElementProcessor {

        public void preprocessKey(Object key);

        public void postprocessKey(Object key);

        public BinaxesMapEngine.BinaxesMapHistoryElementProcessor getElementHistoryProcessor();
    }

    private static final Comparator<Object> entryKeyComparator__ = new ObjectComparator();

    private final SortedMap<K, BinaxesMapEngine<V, Boolean>> entries_;

    protected BinaxesNMapEngine() {
        if (TefService.instance().getRestoringMode() == TefService.RestoringMode.RESTORING_BY_BULK) {
            throw new RuntimeException();
        }

        entries_ = new TreeMap<K, BinaxesMapEngine<V, Boolean>>(entryKeyComparator__);
    }

    protected final synchronized boolean isEntriesInitialized() {
        return entries_ != null;
    }

    final synchronized List<V> getValueList(K key) {
        MVO.phantomCheck(key);

        BinaxesMapEngine<V, Boolean> engine = getEngine(key);
        if (engine == null) {
            return null;
        }

        return engine.getKeysListOf(Boolean.TRUE);
    }

    final synchronized Set<V> getValuesImpl() {
        Set<V> result = new HashSet<V>();
        for (K key : getKeyList()) {
            List<V> values = getValueList(key);
            if (values != null) {
                result.addAll(values);
            }
        }
        return result;
    }

    final synchronized Set<V> getHereafterValuesImpl() {
        final Long time = TransactionContext.getTargetTime();

        final Set<V> result = new HashSet<V>();
        result.addAll(getValuesImpl());
        for (final K key : getKeyList()) {
            final BinaxesMapEngine<V, Boolean> engine = getEngine(key);
            if (engine == null) {
                continue;
            }

            for (final V engineKey : engine.getKeysList()) {
                final SortedMap<Long, Boolean> changes = engine.getChanges(engineKey);
                for (final Map.Entry<Long, Boolean> change : changes.tailMap(time).entrySet()) {
                    if (Boolean.TRUE.equals(change.getValue())) {
                        result.add(engineKey);
                        break;
                    }
                }
            }
        }
        return result;
    }

    final synchronized List<K> getKeyList() {
        List<K> result = new ArrayList<K>();
        for (Map.Entry<K, BinaxesMapEngine<V, Boolean>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesMapEngine<V, Boolean> engine = entry.getValue();
            if (!engine.isExistingAt()) {
                continue;
            }

            result.add(mutableGuard(key));
        }
        return result;
    }

    final synchronized void putEntry(K key, V value) {
        Transaction.validateWriteContext();

        key = mutableGuard(key);
        value = mutableGuard(value);

        MVO.phantomCheck(key);
        MVO.phantomCheck(value);

        BinaxesMapEngine<V, Boolean> engine = getEngine(key);
        if (engine == null) {
            engine = new BinaxesMapEngine<V, Boolean>();
            entries_.put(key, engine);
        }

        engine.putEntry(value, Boolean.TRUE);
    }

    synchronized void removeValue(K key, V value) {
        Transaction.validateWriteContext();

        key = mutableGuard(key);
        value = mutableGuard(value);

        BinaxesMapEngine<V, Boolean> engine = getEngine(key);
        if (engine != null) {
            engine.putEntry(value, Boolean.FALSE);
        }
    }

    final synchronized void commit(MVO.MvoField field, JournalWriter.BinaxesNMapLogger logger) {
        for (Map.Entry<K, BinaxesMapEngine<V, Boolean>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesMapEngine<V, Boolean> engine = entry.getValue();

            logger.preprocessKey(key);
            engine.commit(field, logger.getEntryLogger());
            logger.postprocessKey(key);
        }
    }

    final synchronized void rollback() {
        List<Map.Entry<K, BinaxesMapEngine<V, Boolean>>> entries
                = new ArrayList<Map.Entry<K, BinaxesMapEngine<V, Boolean>>>(entries_.entrySet());
        for (Map.Entry<K, BinaxesMapEngine<V, Boolean>> entry : entries) {
            K key = entry.getKey();
            BinaxesMapEngine<V, Boolean> engine = entry.getValue();
            engine.rollback();

            if (engine.getLatestVersion() == null) {
                entries_.remove(key);
            }
        }
    }

    synchronized TransactionId.W getLatestVersion() {
        TransactionId.W result = null;
        for (Map.Entry<K, BinaxesMapEngine<V, Boolean>> entry : entries_.entrySet()) {
            BinaxesMapEngine<V, Boolean> engine = entry.getValue();

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
            BinaxesNMapHistoryElementProcessor processor,
            TransactionHistoryTraverseMode traverseMode) {
        for (Map.Entry<K, BinaxesMapEngine<V, Boolean>> entry : entries_.entrySet()) {
            K key = entry.getKey();
            BinaxesMapEngine<V, Boolean> engine = entry.getValue();

            processor.preprocessKey(key);
            engine.traverseHistory(processor.getElementHistoryProcessor(), traverseMode);
            processor.postprocessKey(key);
        }
    }

    private synchronized BinaxesMapEngine<V, Boolean> getEngine(K key) {
        return entries_.get(key);
    }

    private static <T> T mutableGuard(T o) {
        return TefUtils.mutableGuard(o);
    }
}
