package tef;

import java.util.*;
import java.util.List;

abstract class IndexEngine<K, V extends MVO> {

    private static final lib38k.logger.Logger dumper__;

    static {
        String isVerboseLoggingStr = System.getProperty("index-dump");
        dumper__
                = (isVerboseLoggingStr != null
                && isVerboseLoggingStr.toLowerCase().matches("true|yes"))
                ? TefService.instance().createLogger("index-dump")
                : null;
    }

    private final Map<K, Set<V>> committedKeyValues_ = new HashMap<K, Set<V>>();
    private Transaction lock_;
    private Map<K, Set<V>> uncommittedKeyValues_;

    IndexEngine() {
        TefService.instance().getIndexes().addIndex(this);
    }

    synchronized List<V> get(K key) {
        if (TransactionContext.getContextTransaction().isRestoredTransaction()) {
            throw new RuntimeException();
        }

        if (!TransactionContext.getContextTransaction().isReadOnly()) {
            gainLock();
        }

        List<V> result = new ArrayList<V>();
        get(result, committedKeyValues_, key);

        if (lock_ == TransactionContext.getContextTransaction()
                && uncommittedKeyValues_ != null) {
            get(result, uncommittedKeyValues_, key);
        }
        return result;
    }

    private void get(List<V> result, Map<K, Set<V>> keyValues, K key) {
        Set<V> values = keyValues.get(key);
        if (values == null) {
            return;
        }

        for (V value : values) {
            if (!MvoUtils.isExistingAt(value)) {
                continue;
            }

            K valueKey = getKey(value);
            if (key == null ? valueKey == null : key.equals(valueKey)) {
                result.add(value);
            }
        }
    }

    synchronized void put(V value) {
        if (TransactionContext.getContextTransaction().isRestoredTransaction()) {
            map(committedKeyValues_, value);
        } else {
            Transaction.validateWriteContext();
            gainLock();
            if (uncommittedKeyValues_ == null) {
                uncommittedKeyValues_ = new HashMap<K, Set<V>>();
            }
            map(uncommittedKeyValues_, value);
        }
    }

    private synchronized void gainLock() {
        if (TransactionContext.getContextTransaction().isRestoredTransaction()) {
            throw new RuntimeException();
        }

        if (lock_ == null) {
            lock_ = TransactionContext.getContextTransaction();
        } else {
            if (lock_ != TransactionContext.getContextTransaction()) {
                throw new IllegalStateException();
            }
        }
    }

    private void map(Map<K, Set<V>> map, V value) {
        K key = getKey(value);

        Set<V> values = map.get(key);
        if (values == null) {
            values = new HashSet<V>();
            map.put(key, values);
        }
        values.add(value);

        if (dumper__ != null) {
            String message
                    = Integer.toString(System.identityHashCode(this), 16)
                    + "\t"
                    + TransactionContext.getContextTransaction().getId().getIdString()
                    + "\t"
                    + value.getMvoId()
                    + "\t"
                    + ValueEncoder.encode(key);
            dumper__.log(message);
        }
    }

    private synchronized void validateLock() {
        if (lock_ != null && lock_ != TransactionContext.getContextTransaction()) {
            throw new IllegalStateException();
        }
    }

    private synchronized void releaseLock() {
        lock_ = null;
        uncommittedKeyValues_ = null;
    }

    synchronized void commitTransaction() {
        if (TransactionContext.getContextTransaction().isRestoredTransaction()) {
            throw new RuntimeException();
        }

        validateLock();
        if (uncommittedKeyValues_ != null) {
            for (K key : uncommittedKeyValues_.keySet()) {
                Set<V> values = uncommittedKeyValues_.get(key);
                if (values == null) {
                    throw new IllegalStateException();
                }

                Set<V> committedValues = committedKeyValues_.get(key);
                if (committedValues == null) {
                    committedValues = new HashSet<V>();
                    committedKeyValues_.put(key, committedValues);
                }
                committedValues.addAll(values);
            }
        }
        releaseLock();
    }

    synchronized void rollbackTransaction() {
        validateLock();
        releaseLock();
    }

    protected abstract K getKey(V value);
}
