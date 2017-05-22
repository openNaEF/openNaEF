package tef.skelton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MvoList<T> extends MvoCollection<T, List<T>> {

    private final M1<Integer, T> values_ = new M1<Integer, T>();

    public MvoList(MvoId id) {
        super(id);
    }

    public MvoList() {
    }

    @Override public synchronized void add(T value) {
        if (value == null) {
            throw new IllegalArgumentException("null value is not allowed.");
        }

        values_.put(new Integer(getNextIndex()), value);
    }

    private int getNextIndex() {
        Integer maxIndex = getMaxIndex();
        return maxIndex == null ? 0 : maxIndex.intValue() + 1;
    }

    private Integer getMaxIndex() {
        Integer result = null;
        for (Integer index : values_.getKeys()) {
            result = result == null
                ? index
                : (index.intValue() > result.intValue()
                    ? index
                    : result);
        }
        return result;
    }

    @Override public synchronized void remove(T value) {
        if (value == null) {
            throw new IllegalArgumentException("null value is not allowed.");
        }

        Integer index = getFirstIndex(value);
        if (index != null) {
            values_.remove(index);
        }
    }

    private Integer getFirstIndex(T value) {
        if (value == null) {
            throw new IllegalArgumentException("null value is not allowed.");
        }

        for (Integer index : getIndexes()) {
            T indexValue = values_.get(index);
            if (indexValue == null) {
                continue;
            }

            if (value.equals(indexValue)) {
                return index;
            }
        }
        return null;
    }

    private List<Integer> getIndexes() {
        List<Integer> keys = new ArrayList<Integer>(values_.getKeys());
        Collections.<Integer>sort(keys);
        return keys;
    }

    @Override public synchronized void clear() {
        values_.clear();
    }

    @Override public synchronized List<T> get() {
        List<T> result = new ArrayList<T>();
        for (Integer index : getIndexes()) {
            T value = values_.get(index);
            if (value == null) {
                continue;
            }

            result.add(value);
        }
        return result;
    }

    @Override public synchronized boolean contains(T value) {
        return get().contains(value);
    }
}
