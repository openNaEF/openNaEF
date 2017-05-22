package tef.skelton.dto;

import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.SkeltonTefService;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * トランザクションによるオブジェクトの変化の DTO 表現です. 更新トランザクションが commit 
 * される毎に生成され, DtoChangeListener に対して配信されます.
 * <p>
 * 以下, DtoChanges 生成対象トランザクションを「対象トランザクション」と表記します.
 */
public class DtoChanges implements Serializable {

    /**
     * 単数属性の変化を表します.
     */
    private static class AttributeChange implements Serializable {

        final String attributeName;
        final Object preValue;
        final Object postValue;

        AttributeChange(String attributeName, Object preValue, Object postValue) {
            this.attributeName = attributeName;
            this.preValue = preValue;
            this.postValue = postValue;
        }
    }

    /**
     * コレクション属性 (set, list) の変化を表します.
     */
    public static class CollectionChange implements Serializable {

        private final Set<?> removedValues_;
        private final Set<?> addedValues_;

        CollectionChange(Set<?> removedValues, Set<?> addedValues) {
            removedValues_ = removedValues;
            addedValues_ = addedValues;
        }

        /**
         * トランザクションによって追加された要素の集合を返します.
         * <p>
         * 要素が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         */
        public Set<?> getAddedValues() {
            return addedValues_;
        }

        /**
         * トランザクションによって削除された要素の集合を返します.
         * <p>
         * 要素が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         */
        public Set<?> getRemovedValues() {
            return removedValues_;
        }
    }

    /**
     * マップ属性の変化を表します.
     * <p>
     * マップ属性の変化は 1. エントリ追加, 2. エントリ削除, 3. 変更 の3種類です.
     * <p>
     * 変更はキーが同一でマップされている値に変化が生じた場合であるため, 変更前と変更後の値を
     * 取得できるようになっています.
     */
    public static class MapChange implements Serializable {

        private final Map<Object, Object> addedValues_;
        private final Map<Object, Object> removedValues_;
        private final Map<Object, Object> alteredPreValues_;
        private final Map<Object, Object> alteredPostValues_;

        MapChange(
            Map<Object, Object> addedValues,
            Map<Object, Object> removedValues,
            Map<Object, Object> alteredPreValues,
            Map<Object, Object> alteredPostValues)
        {
            addedValues_ = addedValues;
            removedValues_ = removedValues;
            alteredPreValues_ = alteredPreValues;
            alteredPostValues_ = alteredPostValues;
        }

        /**
         * 対象トランザクションによって追加されたエントリのキー集合を返します.
         * <p>
         * 値が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         */
        public Set<Object> getAddedKeys() {
            return addedValues_.keySet();
        }

        /**
         * 対象トランザクションによって追加されたエントリの値を返します.
         * <p>
         * 値が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         *
         * @param key エントリのキー
         */
        public Object getAddedValue(Object key) {
            return addedValues_.get(key);
        }

        /**
         * 対象トランザクションによって削除されたエントリのキー集合を返します.
         * <p>
         * 値が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         */
        public Set<Object> getRemovedKeys() {
            return removedValues_.keySet();
        }

        /**
         * 対象トランザクションによって削除されたエントリの値を返します.
         * <p>
         * 値が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         *
         * @param key エントリのキー
         */
        public Object getRemovedValue(Object key) {
            return removedValues_.get(key);
        }

        /**
         * 対象トランザクションによって変更されたエントリのキー集合を返します.
         * <p>
         * 値が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         */
        public Set<Object> getAlteredKeys() {
            return alteredPreValues_.keySet();
        }

        /**
         * 対象トランザクションによって変更されたエントリの変更前の値を返します.
         * <p>
         * 値が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         *
         * @param key エントリのキー
         */
        public Object getAlteredPreValue(Object key) {
            return alteredPreValues_.get(key);
        }

        /**
         * 対象トランザクションによって変更されたエントリの変更後の値を返します.
         * <p>
         * 値が MVO の場合, DTO が保持する属性値は {@link EntityDto.Desc 参照オブジェクト} 
         * ですが, その場合このメソッドは参照の解決を行った DTO を返すようになっています.
         *
         * @param key エントリのキー
         */
        public Object getAlteredPostValue(Object key) {
            return alteredPostValues_.get(key);
        }
    }

    private final TransactionId.W targetVersionLowerBound_;
    private final TransactionId.W targetVersionUpperBound_;
    private final long targetTime_;
    private final Set<EntityDto> newObjects_;
    private final Map<EntityDto, Map<String, AttributeChange>> changedAttributes_
        = new HashMap<EntityDto, Map<String, AttributeChange>>();

    public DtoChanges(
        DtoOriginator originator,
        TransactionId.W targetVersionLowerBound,
        TransactionId.W targetVersionUpperBound,
        long targetTime,
        Set<EntityDto> newObjects,
        Set<EntityDto> changedObjects)
    {
        targetVersionLowerBound_ = targetVersionLowerBound;
        targetVersionUpperBound_ = targetVersionUpperBound;
        targetTime_ = targetTime;

        TransactionId.W savedVersion = TransactionContext.getTargetVersion();
        long savedTime = TransactionContext.getTargetTime();
        try {
            TransactionContext.setTargetVersion(targetVersionUpperBound_);
            TransactionContext.setTargetTime(targetTime_);

            newObjects_ = new HashSet<EntityDto>(newObjects);

            for (EntityDto obj : changedObjects) {
                changedAttributes_.put(obj, getChangedAttributes(originator, obj));
            }
        } finally {
            TransactionContext.setTargetTime(savedTime);
            TransactionContext.setTargetVersion(savedVersion);
        }
    }

    private Map<String, AttributeChange> getChangedAttributes(DtoOriginator originator, EntityDto obj) {
        TransactionId.W savedVersion = TransactionContext.getTargetVersion();

        EntityDto oldVersionObj;
        try {
            TransactionId.W preVersion = new TransactionId.W(targetVersionLowerBound_.serial - 1);
            TransactionContext.setTargetVersion(preVersion);

            oldVersionObj
                = SkeltonTefService.instance().getMvoDtoFactory().<EntityDto>build(originator, Dto2Mvo.toMvo(obj));
        } finally {
            TransactionContext.setTargetVersion(savedVersion);
        }

        Set<String> attributeNames = new HashSet<String>();
        attributeNames.addAll(oldVersionObj.getAttributeNames());
        attributeNames.addAll(obj.getAttributeNames());

        Map<String, AttributeChange> result = new HashMap<String, AttributeChange>();
        for (String attrName : attributeNames) {
            Object pre = oldVersionObj.getValue(attrName);
            Object post = obj.getValue(attrName);

            if (! equals(pre, post)) {
                result.put(attrName, new AttributeChange(attrName, pre, post));
            }
        }
        return result;
    }

    private boolean equals(Object o1, Object o2) {
        if (o1 instanceof Map<?, ?> && o2 instanceof Map<?, ?>) {
            return mapEquals((Map<?, ?>) o1, (Map<?, ?>) o2);
        } else {
            return o1 == null ? o2 == null : o1.equals(o2);
        }
    }

    private boolean mapEquals(Map<?, ?> map1, Map<?, ?> map2) {
        Map<?, ?> m1 = desc2oidMap(map1);
        Map<?, ?> m2 = desc2oidMap(map2);
        if (m1.size() != m2.size()) {
            return false;
        }

        for (Object key : m1.keySet()) {
            if (! m2.keySet().contains(key) || ! equals(m1.get(key), m2.get(key))) {
                return false;
            }
        }
        return true;
    }

    public TransactionId.W getBaseVersion() {
        return targetVersionLowerBound_;
    }

    /**
     * 対象トランザクションの version を返します.
     */
    public TransactionId.W getTargetVersion() {
        return targetVersionUpperBound_;
    }

    /**
     * 対象トランザクションの time を返します.
     */
    public long getTargetTime() {
        return targetTime_;
    }

    /**
     * 対象トランザクションで新たに作成されたオブジェクトの集合を返します.
     */
    public Set<EntityDto> getNewObjects() {
        return newObjects_;
    }

    /**
     * 対象トランザクションで変更されたオブジェクトの集合を返します.
     */
    public Set<EntityDto> getChangedObjects() {
        return changedAttributes_.keySet();
    }

    /**
     * 対象トランザクションで変更された属性の名前の集合を返します.
     *
     * @param obj 変更が生じたオブジェクト
     */
    public Set<String> getChangedAttributeNames(EntityDto obj) {
        return changedAttributes_.get(obj) == null
            ? Collections.<String>emptySet()
            : changedAttributes_.get(obj).keySet();
    }

    /**
     * 対象トランザクションで変更される前の値を返します.
     *
     * @param obj 変更が生じたオブジェクト
     * @param attributeName 属性名
     */
    public Object getPreChangeValue(EntityDto obj, String attributeName) {
        return (changedAttributes_.get(obj) == null || changedAttributes_.get(obj).get(attributeName) == null)
            ? null
            : changedAttributes_.get(obj).get(attributeName).preValue;
    }

    /**
     * コレクション属性の変化を表す {@link CollectionChange} を返します.
     *
     * @param obj 対象とするDTO
     * @param attributeName 属性名
     */
    public CollectionChange getCollectionChange(EntityDto obj, String attributeName) {
        Map<String, AttributeChange> changedAttribute = changedAttributes_.get(obj);
        if (changedAttribute == null) {
            return new CollectionChange(emptySet(), emptySet());
        }
        if (! getChangedAttributeNames(obj).contains(attributeName)) {
            return new CollectionChange(emptySet(), emptySet());
        }

        Object pre = getPreChangeValue(obj, attributeName);
        Object post = obj.getValue(attributeName);

        Set<?> preValues = pre instanceof Set<?> ? (Set<?>) pre : emptySet();
        Set<?> postValues = post instanceof Set<?> ? (Set<?>) post : emptySet();

        Set<Object> removedValues = new HashSet<Object>(preValues);
        removedValues.removeAll(postValues);

        Set<Object> addedValues = new HashSet<Object>(postValues);
        addedValues.removeAll(preValues);

        return new CollectionChange(deref(obj.originator(), removedValues), deref(obj.originator(), addedValues));
    }

    /**
     * マップ属性の変化を表す {@link MapChange} を返します.
     *
     * @param obj 対象とするDTO
     * @param attributeName 属性名
     */
    public MapChange getMapChange(EntityDto obj, String attributeName) {
        Map<String, AttributeChange> changedAttribute = changedAttributes_.get(obj);
        if (changedAttribute == null) {
            return new MapChange(emptyMap(), emptyMap(), emptyMap(), emptyMap());
        }
        if (! getChangedAttributeNames(obj).contains(attributeName)) {
            return new MapChange(emptyMap(), emptyMap(), emptyMap(), emptyMap());
        }

        Object pre = getPreChangeValue(obj, attributeName);
        Object post = obj.getValue(attributeName);

        Map<?, ?> preMap = pre instanceof Map<?, ?> ? (Map<?, ?>) pre : emptyMap();
        Map<?, ?> postMap = post instanceof Map<?, ?> ? (Map<?, ?>) post : emptyMap();

        Map<Object, Object> addedValues = new HashMap<Object, Object>();
        Set<?> oidPreMapKeySet = desc2oidSet(preMap.keySet());
        for (Object postMapKey : postMap.keySet()) {
            if (! oidPreMapKeySet.contains(desc2oid(postMapKey))) {
                addedValues.put(postMapKey, postMap.get(postMapKey));
            }
        }

        Map<Object, Object> removedValues = new HashMap<Object, Object>();
        Set<?> oidPostMapKeySet = desc2oidSet(postMap.keySet());
        for (Object preMapKey : preMap.keySet()) {
            if (! oidPostMapKeySet.contains(desc2oid(preMapKey))) {
                removedValues.put(preMapKey, preMap.get(preMapKey));
            }
        }

        Map<Object, Object> alteredPreValues = new HashMap<Object, Object>();
        Map<Object, Object> alteredPostValues = new HashMap<Object, Object>();
        for (Object postMapKey : postMap.keySet()) {
            if (oidPreMapKeySet.contains(desc2oid(postMapKey))) {
                Object preValue = null;
                for (Object preMapKey : preMap.keySet()) {
                    if (desc2oid(preMapKey).equals(desc2oid(postMapKey))) {
                        preValue = preMap.get(preMapKey);
                    }
                }

                Object postValue = postMap.get(postMapKey);
                if (! equals(preValue, postValue)) {
                    alteredPreValues.put(postMapKey, preValue);
                    alteredPostValues.put(postMapKey, postValue);
                }
            }
        }

        return new MapChange(
            deref(obj.originator(), addedValues),
            deref(obj.originator(), removedValues),
            deref(obj.originator(), alteredPreValues),
            deref(obj.originator(), alteredPostValues));
    }

    private Set<Object> deref(DtoOriginator originator, Set<Object> objs) {
        Set<Object> result = new HashSet<Object>();
        Set<EntityDto.Desc<EntityDto>> refs = new HashSet<EntityDto.Desc<EntityDto>>();
        for (Object obj : objs) {
            if (obj instanceof EntityDto.Desc<?>) {
                refs.add((EntityDto.Desc<EntityDto>) obj);
            } else {
                result.add(obj);
            }
        }
        result.addAll(originator.getDtosSet(refs));
        return result;
    }

    private Map<Object, Object> deref(DtoOriginator originator, Map<Object, Object> map) {
        Set<EntityDto.Desc<EntityDto>> refs = new HashSet<EntityDto.Desc<EntityDto>>();
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (key instanceof EntityDto.Desc<?>) {
                refs.add((EntityDto.Desc<EntityDto>) key);
            }
            if (value instanceof EntityDto.Desc<?>) {
                refs.add((EntityDto.Desc<EntityDto>) value);
            }
        }

        Set<EntityDto> dtos = originator.getDtosSet(refs);
        Map<EntityDto.Desc<?>, EntityDto> desc2dto = new HashMap<EntityDto.Desc<?>, EntityDto>();
        for (EntityDto dto : dtos) {
            desc2dto.put(dto.getDescriptor(), dto);
        }

        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            key = desc2dto.get(key) == null ? key : desc2dto.get(key);
            value = desc2dto.get(value) == null ? value : desc2dto.get(value);

            result.put(key, value);
        }
        return result;
    }

    private Set<Object> emptySet() {
        return Collections.<Object>emptySet();
    }

    private Map<Object, Object> emptyMap() {
        return Collections.<Object, Object>emptyMap();
    }

    private Map<?, ?> desc2oidMap(Map<?, ?> map) {
        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            result.put(desc2oid(key), desc2oid(value));
        }
        return result;
    }

    private Set<?> desc2oidSet(Set<?> values) {
        Set<Object> result = new HashSet<Object>();
        for (Object o : values) {
            result.add(desc2oid(o));
        }
        return result;
    }

    private Object desc2oid(Object o) {
        return o instanceof EntityDto.Desc<?>
            ? ((EntityDto.Desc<?>) o).oid()
            : o;
    }
}
