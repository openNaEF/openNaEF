package tef;

import java.util.List;
import java.util.SortedMap;

public interface BinaxesMap<K, V> {

    public List<K> getKeys();

    public V get(K key);

    public SortedMap<Long, V> getValueChanges(K key);
}
