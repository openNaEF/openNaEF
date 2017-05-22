package tef.skelton.dto;

import tef.skelton.Attribute;
import tef.skelton.Model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class Dto implements Serializable, Model {

    public static class ListAttr<S, T extends Dto> extends Attribute<List<S>, T> {

        public ListAttr(String name) {
            super(name, null);
        }
    }

    public static class SetAttr<S, T extends Dto> extends Attribute<Set<S>, T> {

        public SetAttr(String name) {
            super(name, null);
        }
    }

    public static class MapAttr<K, V, T extends Dto> extends Attribute<Map<K, V>, T> {

        public MapAttr(String name) {
            super(name, null);
        }
    }

    private final Map<String, Object> attributes_ = new LinkedHashMap<String, Object>();

    protected Dto() {
    }

    @Override public synchronized SortedSet<String> getAttributeNames() {
        return new TreeSet<String>(attributes_.keySet());
    }

    @Override public <S, T extends Model> void set(Attribute<S, ? super T> attr, S value) {
        attr.set((T) this, value);
    }

    @Override public <S, T extends Model> S get(Attribute<S, ? super T> attr) {
        return attr.get((T) this);
    }

    @Override public synchronized void putValue(String key, Object value) {
        attributes_.put(key, value);
    }

    @Override public synchronized Object getValue(String key) {
        return attributes_.get(key);
    }

    synchronized void clearAttributes() {
        attributes_.clear();
    }
}
