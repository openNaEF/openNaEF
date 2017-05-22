package voss.model;

import java.util.*;

public abstract class Index<K extends Object, V> {

    private final transient String indexName_;

    private final transient Set<V> uninitializedEntities_ = new HashSet<V>();
    private transient Map<K, V> uniquesMap_ = null;
    private transient Map<K, Set<V>> nonUniquesMap_ = null;

    protected Index(String indexName) {
        indexName_ = indexName;
    }

    public synchronized String getIndexName() {
        return this.indexName_;
    }

    protected synchronized void addValue(V value) {
        uninitializedEntities_.add(value);
    }

    protected synchronized void removeValue(V value) {
        if (uninitializedEntities_.contains(value)) {
            uninitializedEntities_.remove(value);
        } else {
            if (isMultipleKeyEntity()) {
                for (K key : getKeys(value)) {
                    removeIndexEntry(key, value);
                }
            } else {
                K key = getKey(value);
                removeIndexEntry(key, value);
            }
        }

        if (getValues().contains(value)) {
            throw new IllegalStateException();
        }
    }

    private void removeIndexEntry(K key, V value) {
        if (isUniqueKey(key)) {
            if (uniquesMap_.get(key) != value) {
                throw new IllegalStateException();
            }
            uniquesMap_.remove(key);
        } else {
            Set<V> nonUniques = nonUniquesMap_.get(key);
            if (!nonUniques.contains(value)) {
                throw new IllegalStateException();
            }
            nonUniques.remove(value);

            if (nonUniques.size() == 0) {
                nonUniquesMap_.remove(key);
            }
        }
    }

    public synchronized V getUniqueValue(K key) {
        V result = gainUniqueMap().get(key);
        if (result == null) {
            return null;
        } else {
            checkIndexValue(key, result);
            return result;
        }
    }

    protected synchronized Set<K> getUniquesKeys() {
        return gainUniqueMap().keySet();
    }

    public synchronized Set<V> getNonUniqueValues(K key) {
        Set<V> nonUniques = gainNonUniquesMap().get(key);
        if (nonUniques == null) {
            return null;
        }

        Set<V> result = new HashSet<V>(nonUniques);
        for (V value : result) {
            checkIndexValue(key, value);
        }
        return result;
    }

    protected synchronized Set<K> getNonUniquesKeys() {
        return gainNonUniquesMap().keySet();
    }

    protected synchronized Set<V> getValues() {
        Set<V> result = new HashSet<V>();
        result.addAll(uninitializedEntities_);
        if (uniquesMap_ != null) {
            result.addAll(uniquesMap_.values());
        }
        if (nonUniquesMap_ != null) {
            for (Set<V> nonUniques : nonUniquesMap_.values()) {
                result.addAll(nonUniques);
            }
        }
        return result;
    }

    private synchronized void checkIndexValue(K expectedKey, V value) {
        if (isMultipleKeyEntity()) {
            for (K actualKey : getKeys(value)) {
                if (expectedKey == null ? actualKey == null : expectedKey.equals(actualKey)) {
                    return;
                }
            }
            throw new IllegalStateException("index broken.");
        } else {
            K actualKey = getKey(value);
            if (expectedKey == null ? actualKey == null : expectedKey.equals(actualKey)) {
                return;
            } else {
                throw new IllegalStateException("index broken.");
            }
        }
    }

    private synchronized Map<K, V> gainUniqueMap() {
        if (uniquesMap_ == null) {
            initializeMaps();
        }

        if (uninitializedEntities_.size() > 0) {
            processUninitializedEntities();
        }
        return uniquesMap_;
    }

    private synchronized Map<K, Set<V>> gainNonUniquesMap() {
        if (nonUniquesMap_ == null) {
            initializeMaps();
        }

        if (uninitializedEntities_.size() > 0) {
            processUninitializedEntities();
        }
        return nonUniquesMap_;
    }

    private synchronized void initializeMaps() {
        if (uniquesMap_ != null || nonUniquesMap_ != null) {
            throw new IllegalStateException();
        }

        uniquesMap_ = new HashMap<K, V>();
        nonUniquesMap_ = new HashMap<K, Set<V>>();

        uninitializedEntities_.addAll(getInitialValues());
    }

    private synchronized void processUninitializedEntities() {
        for (V uninitializedEntity : new HashSet<V>(uninitializedEntities_)) {
            if (!isInitializable(uninitializedEntity)) {
                continue;
            }
            if (isMultipleKeyEntity()) {
                List<K> keys = getKeys(uninitializedEntity);
                for (K key : keys) {
                    if (isUniqueKey(key)) {
                        mapAsUnique(key, uninitializedEntity);
                    } else {
                        mapAsNonUnique(key, uninitializedEntity);
                    }
                }
            } else {
                K key = getKey(uninitializedEntity);
                if (isUniqueKey(key)) {
                    mapAsUnique(key, uninitializedEntity);
                } else {
                    mapAsNonUnique(key, uninitializedEntity);
                }
            }
            uninitializedEntities_.remove(uninitializedEntity);
        }
    }

    private synchronized void mapAsUnique(K key, V value) {
        if (nonUniquesMap_.get(key) != null) {
            throw new IllegalStateException();
        }
        if (uniquesMap_.get(key) != null) {
            String keyString = getKeyString(key) == null ? "null" : "'" + getKeyString(key) + "'";
            String valueString = getValueString(value) == null ? "null" : "'" + getValueString(value) + "'";
            throw new IllegalStateException("unique index constranints violation:" + " [index]" + indexName_ + " [key]"
                    + keyString + " [value]" + valueString);
        }

        uniquesMap_.put(key, value);
    }

    private synchronized void mapAsNonUnique(K key, V value) {
        if (uniquesMap_.get(key) != null) {
            throw new IllegalStateException();
        }

        Set<V> nonUniques = nonUniquesMap_.get(key);
        if (nonUniques == null) {
            nonUniques = new HashSet<V>();
            nonUniquesMap_.put(key, nonUniques);
        }

        nonUniques.add(value);
    }

    protected abstract boolean isInitializable(V o);

    protected abstract boolean isUniqueKey(K key);

    protected abstract boolean isMultipleKeyEntity();

    protected abstract K getKey(V value);

    protected abstract List<K> getKeys(V Value);

    protected abstract Set<V> getInitialValues();

    protected abstract String getKeyString(K key);

    protected abstract String getValueString(V value);
}