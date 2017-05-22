package tef.skelton;

import tef.MVO;

import java.util.HashSet;
import java.util.Set;

public class MvoMap<K, V> extends MVO {

    private final M1<K, V> map_ = new M1<K, V>();

    public MvoMap(MvoId id) {
        super(id);
    }

    public MvoMap() {
    }

    public void put(K key, V value) {
        map_.put(key, value);
    }

    public void remove(K key) {
        map_.remove(key);
    }

    public void clear() {
        map_.clear();
    }

    public Set<K> getKeys() {
        return new HashSet<K>(map_.getKeys());
    }

    public boolean containsKey(K key) {
        return map_.containsKey(key);
    }

    public V get(K key) {
        return map_.get(key);
    }
}
