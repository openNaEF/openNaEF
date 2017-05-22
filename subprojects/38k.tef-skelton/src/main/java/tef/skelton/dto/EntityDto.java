package tef.skelton.dto;

import tef.skelton.Attribute;
import tef.skelton.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * ドメイン モデルを表す DTO です. 特定の DB の実装から独立した抽象化が行われるようにしてください.
 * <p>
 * 特定の DB に固有の情報は {@link Oid} および {@link Desc} のサブタイプを設けて表現します.
 */
public abstract class EntityDto extends Dto {

    /**
     * モデルの Object ID (DB においてオブジェクトを一意に特定するための情報) を保持します.
     * DB の実装ごとにサブタイプを定義します.
     */
    public interface Oid extends Serializable {
    }

    /**
     * {@link EntityDto} に関するメタ情報を表すオブジェクトです. DB の実装ごとにサブタイプを定義し,
     * DB 固有の情報を保持します.
     */
    public interface Desc<T extends EntityDto> extends Serializable {

        public Oid oid();
    }

    public static class SingleRefAttr<S extends EntityDto, T extends EntityDto> 
        extends Attribute.SingleAttr<Desc<S>, T> 
    {
        public SingleRefAttr(java.lang.String name) {
            super(name, null);
        }

        public S deref(T owner) {
            return owner.toDto(this);
        }
    }

    public static class ListRefAttr<S extends EntityDto, T extends EntityDto>
        extends ListAttr<Desc<S>, T>
    {
        public ListRefAttr(String name) {
            super(name);
        }

        public List<S> deref(T owner) {
            return owner.toDtosList(this);
        }
    }

    public static class SetRefAttr<S extends EntityDto, T extends EntityDto> 
        extends SetAttr<Desc<S>, T> 
    {
        public SetRefAttr(String name) {
            super(name);
        }

        public Set<S> deref(T owner) {
            return owner.toDtosSet(this);
        }
    }

    /**
     * key が EntityDto の Desc であるマップ属性です. 
     */
    public static class MapKeyRefAttr<K extends EntityDto, V, T extends EntityDto>
        extends MapAttr<Desc<K>, V, T>
    {
        public MapKeyRefAttr(String name) {
            super(name);
        }

        /**
         * key の Desc を DTO に実体化した Map を返すヘルパー メソッドです.
         */
        public Map<K, V> deref(T owner) {
            return owner.toKeyDtosMap(this);
        }
    }

    /**
     * value が EntityDto の Desc であるマップ属性です.
     */
    public static class MapValueRefAttr<K, V extends EntityDto, T extends EntityDto>
        extends MapAttr<K, Desc<V>, T>
    {
        public MapValueRefAttr(String name) {
            super(name);
        }

        /**
         * value の Desc を DTO に実体化した Map を返すヘルパー メソッドです.
         */
        public Map<K, V> deref(T owner) {
            return owner.toValueDtosMap(this);
        }
    }

    /**
     * key と value が EntityDto の Desc であるマップ属性です.
     */
    public static class MapKeyValueRefAttr<K extends EntityDto, V extends EntityDto, T extends EntityDto>
        extends MapAttr<Desc<K>, Desc<V>, T>
    {
        public MapKeyValueRefAttr(String name) {
            super(name);
        }

        /**
         * key と value の Desc を DTO に実体化した Map を返すヘルパー メソッドです.
         */
        public Map<K, V> deref(T owner) {
            return owner.toKeyValueDtosMap(this);
        }
    }

    private DtoOriginator originator_;
    private Desc<?> desc_;

    private final Set<String> initializedAttrNames_ = new HashSet<String>();

    private final Set<String> lazyInitAttrNames_ = new HashSet<String>();

    protected EntityDto() {
    }

    synchronized void setOriginator(DtoOriginator originator) {
        originator_ = originator;
    }

    public synchronized DtoOriginator originator() {
        return originator_;
    }

    synchronized void setDescriptor(Desc<?> desc) {
        desc_ = desc;
    }

    public synchronized final Desc<?> getDescriptor() {
        return desc_;
    }

    public synchronized final Oid getOid() {
        return desc_.oid();
    }

    synchronized void addLazyInitAttrName(String attrname) {
        lazyInitAttrNames_.add(attrname);
    }

    @Override public SortedSet<String> getAttributeNames() {
        TreeSet<String> result = new TreeSet<String>();
        result.addAll(initializedAttrNames_);
        result.addAll(lazyInitAttrNames_);
        return result;
    }

    @Override public <S, T extends Model> void set(Attribute<S, ? super T> attr, S value) {
        initializedAttrNames_.add(attr.getName());

        super.set(attr, value);
    }

    @Override public synchronized void putValue(String key, Object value) {
        initializedAttrNames_.add(key);
        super.putValue(key, value);
    }

    @Override public synchronized Object getValue(String key) {
        if (! initializedAttrNames_.contains(key) && lazyInitAttrNames_.contains(key)) {
            putValue(key, originator_.getAttributeValue(getDescriptor(), key));
        }

        return super.getValue(key);
    }

    @Override synchronized void clearAttributes() {
        super.clearAttributes();
        initializedAttrNames_.clear();
    }

    public <T extends EntityDto> T toDto(Desc<T> desc) {
        return originator_.getDto(desc);
    }

    public <T extends EntityDto> List<T> toDtosList(List<? extends Desc<T>> descs) {
        return originator_.getDtosList(descs instanceof Serializable ? descs : new ArrayList<Desc<T>>(descs));
    }

    public <T extends EntityDto> Set<T> toDtosSet(Set<? extends Desc<T>> descs) {
        return originator_.getDtosSet(descs instanceof Serializable ? descs : new HashSet<Desc<T>>(descs));
    }

    synchronized <X extends EntityDto, Y extends EntityDto> X toDto(Attribute<? extends Desc<X>, Y> attr) {
        Desc<X> desc = attr.get((Y) this);
        return toDto(desc);
    }

    synchronized <X extends EntityDto, Y extends EntityDto>
        List<X> toDtosList(Attribute<? extends List<? extends Desc<X>>, Y> attr)
    {
        return toDtosList(attr.get((Y) this));
    }

    synchronized <X extends EntityDto, Y extends EntityDto>
        Set<X> toDtosSet(Attribute<? extends Set<? extends Desc<X>>, Y> attr)
    {
        return toDtosSet(attr.get((Y) this));
    }

    synchronized <K extends EntityDto, V, T extends EntityDto>
        Map<K, V> toKeyDtosMap(Attribute<Map<Desc<K>, V>, T> attr)
    {
        Map<Desc<K>, V> source = attr.get((T) this);
        Map<K, V> result = new HashMap<K, V>();
        for (K keyDto : toDtosSet(source.keySet())) {
            result.put(keyDto, source.get(keyDto == null ? null : keyDto.getDescriptor()));
        }
        return result;
    }

    synchronized <K, V extends EntityDto, T extends EntityDto>
        Map<K, V> toValueDtosMap(Attribute<Map<K, Desc<V>>, T> attr)
    {
        Map<K, Desc<V>> source = attr.get((T) this);
        Map<Desc<V>, V> descToDtoMap = descToDtoMap(source.values());
        Map<K, V> result = new HashMap<K, V>();
        for (K key : source.keySet()) {
            result.put(key, descToDtoMap.get(source.get(key)));
        }
        return result;
    }

    synchronized <K extends EntityDto, V extends EntityDto, T extends EntityDto>
        Map<K, V> toKeyValueDtosMap(Attribute<Map<Desc<K>, Desc<V>>, T> attr)
    {
        Map<Desc<K>, Desc<V>> source = attr.get((T) this);
        Map<Desc<V>, V> descToDtoMap = descToDtoMap(source.values());
        Map<K, V> result = new HashMap<K, V>();
        for (K keyDto : toDtosSet(source.keySet())) {
            Desc<V> valueDesc = source.get(keyDto == null ? null : keyDto.getDescriptor());
            result.put(keyDto, descToDtoMap.get(valueDesc));
        }
        return result;
    }

    private <T extends EntityDto> Map<Desc<T>, T> descToDtoMap(Collection<? extends Desc<T>> descs) {
        Map<Desc<T>, T> result = new HashMap<Desc<T>, T>();
        for (T dto : toDtosSet(new HashSet<Desc<T>>(descs))) {
            result.put((Desc<T>) dto.getDescriptor(), dto);
        }
        return result;
    }

    public synchronized void renew() {
        EntityDto newOne = originator_.getDto(getOid());

        desc_ = newOne.desc_;

        clearAttributes();
        for (String key : newOne.getAttributeNames()) {
            if (lazyInitAttrNames_.contains(key)) {
                continue;
            }

            putValue(key, newOne.getValue(key));
        }
    }

    @Override public int hashCode() {
        return getOid().hashCode();
    }

    @Override public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        EntityDto another = (EntityDto) o;
        return getDescriptor().equals(another.getDescriptor());
    }

    public <T> Set<T> unnull(Set<T> obj) {
        return obj == null ? new HashSet<T>() : obj;
    }
}
