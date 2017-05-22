package tef.skelton;

import java.util.HashSet;
import java.util.Set;

public class MvoSet<T> extends MvoCollection<T, Set<T>> {

    private final S1<T> values_ = new S1<T>();

    public MvoSet(MvoId id) {
        super(id);
    }

    public MvoSet() {
    }

    @Override public void add(T value) {
        if (values_.contains(value)) {
            return;
        }

        values_.add(value);
    }

    @Override public void remove(T value) {
        values_.remove(value);
    }

    @Override public void clear() {
        values_.clear();
    }

    @Override public Set<T> get() {
        return new HashSet<T>(values_.get());
    }

    @Override public boolean contains(T value) {
        return values_.contains(value);
    }
}
