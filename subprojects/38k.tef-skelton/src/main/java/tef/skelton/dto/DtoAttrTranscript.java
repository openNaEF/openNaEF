package tef.skelton.dto;

import tef.skelton.Attribute;
import tef.skelton.Model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DTO の属性転写を定義します. 属性値の決定を DB の実装と分離するため, DTO のソースとなる 
 * DB の種類ごとの定義を必要とします.
 * <p>
 * 属性値の設定を DTO 生成時に行うか, それとも遅延評価するかを指定することができます.
 */
public abstract class DtoAttrTranscript<S, T> {

    public static enum EvalStrategy {

        EAGER,
        LAZY
    }

    public static class SingleAttr<V, T> extends DtoAttrTranscript<V, T> {

        private final Attribute<V, ? super T> modelAttr_;

        public SingleAttr(
            Class<? extends EntityDto> klass,
            Attribute<V, ?> dtoAttr,
            Attribute<V, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public V get(T model) {
            return modelAttr_.get(model);
        }
    }

    public static class ListAttr<E, T> extends DtoAttrTranscript<List<E>, T> {

        private final Attribute.ListAttr<E, ? super T> modelAttr_;

        public ListAttr(
            Class<? extends EntityDto> klass,
            Attribute<List<E>, ?> dtoAttr,
            Attribute.ListAttr<E, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public List<E> get(T model) {
            return modelAttr_.snapshot(model);
        }
    }

    public static class SetAttr<E, T> extends DtoAttrTranscript<Set<E>, T> {

        private final Attribute.SetAttr<E, ? super T> modelAttr_;

        public SetAttr(
            Class<? extends EntityDto> klass,
            Attribute<Set<E>, ?> dtoAttr,
            Attribute.SetAttr<E, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public Set<E> get(T model) {
            return modelAttr_.snapshot(model);
        }
    }

    public static class MapAttr<K, V, T> extends DtoAttrTranscript<Map<K, V>, T> {

        private final Attribute.MapAttr<K, V, ? super T> modelAttr_;

        public MapAttr(
            Class<? extends EntityDto> klass,
            Attribute<Map<K, V>, ?> dtoAttr,
            Attribute.MapAttr<K, V, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public Map<K, V> get(T model) {
            return modelAttr_.snapshot(model);
        }
    }

    public static abstract class SingleRef<R extends EntityDto, T> extends DtoAttrTranscript<EntityDto.Desc<R>, T> {

        protected SingleRef(
            Class<? extends EntityDto> klass,
            Attribute<EntityDto.Desc<R>, ?> attr,
            EvalStrategy evalStrategy)
        {
            super(klass, attr, evalStrategy);
        }

        @Override public EntityDto.Desc<R> get(T model) {
            return MvoDtoDesc.<R>build1(getValue(model));
        }

        abstract protected Model getValue(T model);
    }

    public static abstract class ListRef<R extends EntityDto, T> extends DtoAttrTranscript<List<EntityDto.Desc<R>>, T> {

        protected ListRef(
            Class<? extends EntityDto> klass,
            Attribute<List<EntityDto.Desc<R>>, ?> attr,
            EvalStrategy evalStrategy)
        {
            super(klass, attr, evalStrategy);
        }

        @Override public List<EntityDto.Desc<R>> get(T model) {
            return MvoDtoDesc.<R>buildL(getValues(model));
        }

        abstract protected List<? extends Model> getValues(T model);
    }

    public static abstract class SetRef<R extends EntityDto, T> extends DtoAttrTranscript<Set<EntityDto.Desc<R>>, T> {

        protected SetRef(
            Class<? extends EntityDto> klass,
            Attribute<Set<EntityDto.Desc<R>>, ?> attr,
            EvalStrategy evalStrategy)
        {
            super(klass, attr, evalStrategy);
        }

        @Override public Set<EntityDto.Desc<R>> get(T model) {
            return MvoDtoDesc.<R>buildS(getValues(model));
        }

        abstract protected Set<? extends Model> getValues(T model);
    }

    public static abstract class MapKeyRef<K extends EntityDto, V, T>
        extends DtoAttrTranscript<Map<EntityDto.Desc<K>, V>, T>
    {
        protected MapKeyRef(
            Class<? extends EntityDto> klass,
            Attribute<Map<EntityDto.Desc<K>, V>, ?> attr,
            EvalStrategy evalStrategy)
        {
            super(klass, attr, evalStrategy);
        }

        @Override public Map<EntityDto.Desc<K>, V> get(T model) {
            return MvoDtoDesc.<K, V>buildMk(getValues(model));
        }

        abstract protected Map<? extends Model, ?> getValues(T model);
    }

    public static abstract class MapValueRef<K, V extends EntityDto, T>
        extends DtoAttrTranscript<Map<K, EntityDto.Desc<V>>, T>
    {
        protected MapValueRef(
            Class<? extends EntityDto> klass,
            Attribute<Map<K, EntityDto.Desc<V>>, ?> attr,
            EvalStrategy evalStrategy)
        {
            super(klass, attr, evalStrategy);
        }

        @Override public Map<K, EntityDto.Desc<V>> get(T model) {
            return MvoDtoDesc.<K, V>buildMv(getValues(model));
        }

        abstract protected Map<?, ? extends Model> getValues(T model);
    }

    public static abstract class MapKeyValueRef<K extends EntityDto, V extends EntityDto, T>
        extends DtoAttrTranscript<Map<EntityDto.Desc<K>, EntityDto.Desc<V>>, T>
    {
        protected MapKeyValueRef(
            Class<? extends EntityDto> klass,
            Attribute<Map<EntityDto.Desc<K>, EntityDto.Desc<V>>, ?> attr,
            EvalStrategy evalStrategy)
        {
            super(klass, attr, evalStrategy);
        }

        @Override public Map<EntityDto.Desc<K>, EntityDto.Desc<V>> get(T model) {
            return MvoDtoDesc.<K, V>buildMkv(getValues(model));
        }

        abstract protected Map<? extends Model, ? extends Model> getValues(T model);
    }

    public static class SingleRefAttr<R extends EntityDto, T> extends DtoAttrTranscript<EntityDto.Desc<R>, T> {

        private final Attribute<? extends Model, ? super T> modelAttr_;

        public SingleRefAttr(
            Class<? extends EntityDto> klass,
            Attribute<EntityDto.Desc<R>, ?> dtoAttr,
            Attribute<? extends Model, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public EntityDto.Desc<R> get(T model) {
            return MvoDtoDesc.<R>build1(modelAttr_.get(model));
        }
    }

    public static class ListRefAttr<R extends EntityDto, T extends Model>
        extends DtoAttrTranscript<List<EntityDto.Desc<R>>, T>
    {
        private final Attribute.ListAttr<? extends Model, ? super T> modelAttr_;

        public ListRefAttr(
            Class<? extends EntityDto> klass,
            Attribute<List<EntityDto.Desc<R>>, ?> dtoAttr,
            Attribute.ListAttr<? extends Model, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public List<EntityDto.Desc<R>> get(T model) {
            return MvoDtoDesc.<R>buildL(modelAttr_.snapshot(model));
        }
    }

    public static class SetRefAttr<R extends EntityDto, T extends Model>
        extends DtoAttrTranscript<Set<EntityDto.Desc<R>>, T>
    {
        private final Attribute.SetAttr<? extends Model, ? super T> modelAttr_;

        public SetRefAttr(
            Class<? extends EntityDto> klass,
            Attribute<Set<EntityDto.Desc<R>>, ?> dtoAttr,
            Attribute.SetAttr<? extends Model, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public Set<EntityDto.Desc<R>> get(T model) {
            return MvoDtoDesc.<R>buildS(modelAttr_.snapshot(model));
        }
    }

    public static class MapKeyRefAttr<K extends EntityDto, V, T>
        extends DtoAttrTranscript<Map<EntityDto.Desc<K>, V>, T>
    {
        private final Attribute.MapAttr<? extends Model, ?, ? super T> modelAttr_;

        public MapKeyRefAttr(
            Class<? extends EntityDto> klass,
            Attribute<Map<EntityDto.Desc<K>, V>, ?> dtoAttr,
            Attribute.MapAttr<? extends Model, ?, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public Map<EntityDto.Desc<K>, V> get(T model) {
            return MvoDtoDesc.<K, V>buildMk(modelAttr_.snapshot(model));
        }
    }

    public static class MapValueRefAttr<K, V extends EntityDto, T>
        extends DtoAttrTranscript<Map<K, EntityDto.Desc<V>>, T>
    {
        private final Attribute.MapAttr<?, ? extends Model, ? super T> modelAttr_;

        public MapValueRefAttr(
            Class<? extends EntityDto> klass,
            Attribute<Map<K, EntityDto.Desc<V>>, ?> dtoAttr,
            Attribute.MapAttr<?, ? extends Model, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public Map<K, EntityDto.Desc<V>> get(T model) {
            return MvoDtoDesc.<K, V>buildMv(modelAttr_.snapshot(model));
        }
    }

    public static class MapKeyValueRefAttr<K extends EntityDto, V extends EntityDto, T>
        extends DtoAttrTranscript<Map<EntityDto.Desc<K>, EntityDto.Desc<V>>, T>
    {
        private final Attribute.MapAttr<? extends Model, ? extends Model, ? super T> modelAttr_;

        public MapKeyValueRefAttr(
            Class<? extends EntityDto> klass,
            Attribute<Map<EntityDto.Desc<K>, EntityDto.Desc<V>>, ?> dtoAttr,
            Attribute.MapAttr<? extends Model, ? extends Model, ? super T> modelAttr,
            EvalStrategy evalStrategy)
        {
            super(klass, dtoAttr, evalStrategy);

            modelAttr_ = modelAttr;
        }

        @Override public Map<EntityDto.Desc<K>, EntityDto.Desc<V>> get(T model) {
            return MvoDtoDesc.<K, V>buildMkv(modelAttr_.snapshot(model));
        }
    }

    private final Class<? extends EntityDto> dtoClass_;
    private final Attribute<?, ?> attribute_;
    private final EvalStrategy evalStrategy_;

    /**
     * @param evalStrategy この属性値を遅延評価するかどうかを指定します. 遅延評価する場合は LAZY,
     *        DTO 生成時に属性値を設定する場合は EAGER を与えます.
     */
    protected DtoAttrTranscript(
        Class<? extends EntityDto> dtoClass,
        Attribute<S, ?> attribute,
        EvalStrategy evalStrategy)
    {
        if (dtoClass == null || attribute == null || evalStrategy == null) {
            throw new IllegalArgumentException();
        }

        dtoClass_ = dtoClass;
        attribute_ = attribute;
        evalStrategy_ = evalStrategy;
    }

    Class<? extends EntityDto> getDtoClass() {
        return dtoClass_;
    }

    Attribute<?, ?> getAttribute() {
        return attribute_;
    }

    boolean isLazyInit() {
        return evalStrategy_ == EvalStrategy.LAZY;
    }

    public abstract S get(T model);
}
