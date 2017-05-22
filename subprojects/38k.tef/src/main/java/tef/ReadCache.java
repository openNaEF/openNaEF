package tef;

import java.util.Collections;
import java.util.Map;

public abstract class ReadCache<K, V> {

    private static int cacheObjectsCount__ = 0;
    private static int cacheCallCount__ = 0;
    private static int cacheHitCount__ = 0;
    private static int cacheUpdatedCount__ = 0;

    public static synchronized int getCacheObjectsCount() {
        return cacheObjectsCount__;
    }

    public static synchronized int getCacheCallCount() {
        return cacheCallCount__;
    }

    public static synchronized int getCacheHitCount() {
        return cacheHitCount__;
    }

    public static synchronized int getCacheUpdatedCount() {
        return cacheUpdatedCount__;
    }

    public static class InvalidCacheAccessException extends RuntimeException {

        InvalidCacheAccessException(String message) {
            super(message);
        }
    }

    private TransactionId.W cacheVersion_;
    private Map<K, V> cachedMap_;

    protected ReadCache() {
        synchronized (ReadCache.class) {
            cacheObjectsCount__++;
        }
    }

    public V get(K key) {
        if (!(TransactionContext.getTransactionId() instanceof TransactionId.R)) {
            return createCacheMap().get(key);
        }

        synchronized (this) {
            synchronized (ReadCache.class) {
                cacheCallCount__++;
            }

            if (isCacheValid()) {
                synchronized (ReadCache.class) {
                    cacheHitCount__++;
                }
                return cachedMap_.get(key);
            }

            if (isToUseCache()) {
                updateCache();

                return cachedMap_.get(key);
            }
        }

        return createCacheMap().get(key);
    }

    private synchronized boolean isCacheValid() {
        return cacheVersion_ != null
                && TransactionContext.getTargetVersion().serial == cacheVersion_.serial;
    }

    private synchronized boolean isToUseCache() {
        if (TransactionContext.getTargetVersion().serial
                != TransactionContext.getBaseTransactionId().serial) {
            return false;
        }

        if (cacheVersion_ == null) {
            return true;
        }

        if (cacheVersion_.serial < TransactionContext.getBaseTransactionId().serial) {
            return true;
        }

        return false;
    }

    private synchronized void updateCache() {
        synchronized (ReadCache.class) {
            cacheUpdatedCount__++;
        }

        cachedMap_ = Collections.unmodifiableMap(createCacheMap());
        cacheVersion_ = TransactionContext.getTargetVersion();
    }

    protected abstract Map<K, V> createCacheMap();
}
