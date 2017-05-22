package tef;

import java.util.Set;

public interface MonaxisMap<K, V> {

    public Set<K> getKeys();

    public V get(K key);
}
