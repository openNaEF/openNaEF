package tef.skelton;

import tef.MVO;

public abstract class MvoCollection<T, U extends java.util.Collection<T>> extends MVO {

    protected MvoCollection(MvoId id) {
        super(id);
    }

    protected MvoCollection() {
    }

    abstract public void add(T value);
    abstract public void remove(T value);
    abstract public void clear();
    abstract public U get();
    abstract public boolean contains(T value);
}
