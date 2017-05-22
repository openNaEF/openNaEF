package tef;

import java.util.LinkedHashMap;
import java.util.Map;

class AccessOrderCache<K, V> extends LinkedHashMap<K, V> {

    private static final int CACHE_SIZE = 1000;

    AccessOrderCache() {
        super(16, 0.75f, true);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return CACHE_SIZE < size();
    }
}
