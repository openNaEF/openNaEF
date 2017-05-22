package tef;

import java.util.Set;
import java.util.SortedMap;

public interface BinaxesField<T> {

    public T get();

    public void set(T value);

    public Set<T> getAllFuture();

    public Set<T> getAllHereafter();

    public SortedMap<Long, T> getChanges();

    public SortedMap<Long, T> getHereafterChanges();

    public SortedMap<Long, T> getFutureChanges();
}
