package tef;

import java.util.SortedMap;

public interface MonaxisField<T> {

    public T get();

    public void set(T value);

    public SortedMap<TransactionId.W, T> getVersions();
}
