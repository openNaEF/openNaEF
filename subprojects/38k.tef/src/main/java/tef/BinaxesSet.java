package tef;

import java.util.List;
import java.util.SortedMap;

public interface BinaxesSet<T> {

    public List<T> get();

    public List<Long> getChangeTimes();

    public SortedMap<Long, List<T>> getChanges();
}
