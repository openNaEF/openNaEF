package voss.model;

import java.io.Serializable;
import java.util.*;

public abstract class ExtInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<Object, Object> values_ = new HashMap<Object, Object>();

    ExtInfo() {
    }

    public synchronized void put(Object key, Object value) {
        values_.put(key, value);
    }

    public synchronized Object get(Object key) {
        return values_.get(key);
    }

    public synchronized Object[] getKeys() {
        return values_.keySet().toArray();
    }

    public synchronized boolean containsKey(Object key) {
        return values_.keySet().contains(key);
    }

    @Override
    public int hashCode() {
        int result = 0;
        Object[] keys = getKeys();
        for (int i = 0; i < keys.length; i++) {
            result += keys[i].hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ExtInfo another = (ExtInfo) obj;
        Set<?> keys = new HashSet<Object>(Arrays.asList(getKeys()));
        Set<?> anotherKeys = new HashSet<Object>(Arrays.asList(another.getKeys()));
        Set<?> keysComparingSet = new HashSet<Object>(anotherKeys);
        keysComparingSet.removeAll(keys);
        if (keysComparingSet.size() > 0) {
            return false;
        }
        for (Iterator<?> i = keys.iterator(); i.hasNext(); ) {
            Object key = i.next();
            Object value = get(key);
            Object anotherValue = another.get(key);
            if (!(value == null ? anotherValue == null : value.equals(anotherValue))) {
                return false;
            }
        }
        return true;
    }
}