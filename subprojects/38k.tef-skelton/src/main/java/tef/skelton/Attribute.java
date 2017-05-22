package tef.skelton;

import tef.DateTime;
import tef.MVO;
import tef.TransactionContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  属性のメタ定義です. クラシカルな {@code MVO} の属性は {@code F0, F1, F2, S0, S1, S2, M1, M2} 
 *  といった {@code MvoField} を使用して定義しますが, {@link AbstractModel} におけるモダンな
 *  属性定義は基本的に {@code Attribute} として定義します.
 *  <p>
 *  従来の {@code MvoField} を使用して定義した属性はそれを扱う getter/setter を {@code MVO} 
 *  に定義し, UI として shell コマンドを作成するなどの附随するコーディングが必要とされていました.
 *  それに対して, {@code Attribute} で宣言された属性は自動的に shell の attribute コマンドの対象
 *  となるなど, 宣言するだけで定義を完結させることが可能となります.
 *  <p>
 *  設定ファイル {@code configs/AttributeMetaDefinition.xml} で定義される属性は, 内部ではこの 
 *  {@code Attribute} オブジェクトとして表現されます.
 */
public abstract class Attribute<S, T extends Model> implements Serializable {

    public static class SingleAttr<S, T extends Model> extends Attribute<S, T> {

        public static interface PostProcessor<S, T extends Model> {

            public void set(T model, S oldValue, S newValue);
        }

        /**
         *  Attribute.Single をグルーピングし, 同時に値を設定できるのはそのうちの一つだけとする
         *  制約を定義します.
         */
        public static class OptionGroup
            <R extends Attribute.SingleAttr<S, T>,
             S extends AbstractModel,
             T extends AbstractModel>
        {
            private static final Map<Attribute.SingleAttr<?, ?>, OptionGroup<?, ?, ?>> mapping__
                = new HashMap<Attribute.SingleAttr<?, ?>, OptionGroup<?, ?, ?>>();

            public static synchronized OptionGroup get(Attribute.SingleAttr<?, ?> attr) {
                return mapping__.get(attr);
            }

            private final Set<R> options_;

            public OptionGroup(R... options) {
                synchronized(OptionGroup.class) {
                    for (R option : options) {
                        if (option == null) {
                            throw new IllegalArgumentException();
                        }

                        if (get(option) != null) {
                            throw new IllegalStateException();
                        }
                    }

                    Set<R> optionsSet = new HashSet<R>();
                    for (R option : options) {
                        optionsSet.add(option);
                        mapping__.put(option, this);
                    }
                    options_ = Collections.<R>unmodifiableSet(optionsSet);
                }
            }

            public Set<R> getOptions() {
                return options_;
            }

            public S getValue(T model) {
                R option = selectOption(model);
                return option == null ? null : option.get(model);
            }

            public R selectOption(T model) {
                R result = null;
                for (R option : getOptions()) {
                    if (option.get(model) != null) {
                        if (result == null) {
                            result = option;
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                }
                return result;
            }
        }

        private final transient List<PostProcessor<S, T>> postProcessors_ = new ArrayList<PostProcessor<S, T>>();

        public SingleAttr(String name, AttributeType<S> type) {
            super(name, type);
        }

        @Override public void set(T model, S newValue)
            throws ValueException, ConfigurationException
        {
            S oldValue = get(model);
            if (oldValue == newValue) {
                return;
            }

            OptionGroup<?, ?, ?> options = OptionGroup.get(this);
            if (options != null) {
                for (Attribute.SingleAttr<?, ?> otherAttr : options.getOptions()) {
                    if (otherAttr != this
                        && ((Attribute.SingleAttr<?, T>) otherAttr).get(model) != null)
                    {
                        throw new IllegalStateException();
                    }
                }
            }

            validateValue(model, newValue);

            super.set(model, newValue);

            for (PostProcessor<S, T> postProcessor : postProcessors_) {
                postProcessor.set(model, oldValue, newValue);
            }
        }

        public void validateValue(T model, S value)
            throws ValueException, ConfigurationException 
        {
            return;
        }

        public void addPostProcessor(PostProcessor<S, T> postProcessor) {
            postProcessors_.add(postProcessor);
        }
    }

    public static class SingleString<T extends Model> extends SingleAttr<String, T> {

        public SingleString(String name) {
            super(name, AttributeType.STRING);
        }
    }

    public static class SingleBoolean<T extends Model> extends SingleAttr<Boolean, T> {

        public SingleBoolean(String name) {
            super(name, AttributeType.BOOLEAN);
        }

        public boolean booleanValue(T model) {
            Boolean value = super.get(model);
            return value == null ? false : value.booleanValue();
        }
    }

    public static class SingleInteger<T extends Model> extends SingleAttr<Integer, T> {

        public SingleInteger(String name) {
            super(name, AttributeType.INTEGER);
        }

        public int intValue(T model) {
            Integer value = super.get(model);
            return value == null ? 0 : value.intValue();
        }
    }

    public static class SingleLong<T extends Model> extends SingleAttr<Long, T> {

        public SingleLong(String name) {
            super(name, AttributeType.LONG);
        }

        public long longValue(T model) {
            Long value = super.get(model);
            return value == null ? 0l : value.longValue();
        }
    }

    public static class SingleDouble<T extends Model> extends SingleAttr<Double, T> {

        public SingleDouble(String name) {
            super(name, AttributeType.DOUBLE);
        }

        public double doubleValue(T model) {
            Double value = super.get(model);
            return value == null ? 0.0d : value.doubleValue();
        }
    }

    public static class SingleDateTime<T extends Model> extends SingleAttr<DateTime, T> {

        public SingleDateTime(String name) {
            super(name, AttributeType.DATETIME);
        }
    }

    public static class SingleEnum<E extends Enum<E>, T extends Model> extends SingleAttr<E, T> {

        public SingleEnum(String name, Class<E> type) {
            super(name, new AttributeType.EnumType<E>(type));
        }
    }

    public static class SingleModel<E extends Model, T extends Model> extends SingleAttr<E, T> {

        public SingleModel(String name, Class<? extends Model> type) {
            super(name, new AttributeType.ModelType<E>((Class<E>) type));
        }
    }

    public static class SingleUniqueNameModel<E extends MVO & NamedModel, T extends Model>
        extends SingleAttr<E, T>
    {
        public SingleUniqueNameModel(String name, UniquelyNamedModelHome<E> home) {
            super(name, new AttributeType.UniqueNameModelType<E>(home));
        }
    }

    public static abstract class CollectionAttr
        <S, T extends Model, U extends Collection<S>, V extends MvoCollection<S, U>>
        extends Attribute<V, T> 
    {
        public static interface PostProcessor<S, T extends Model> {

            public void add(T model, S value);
            public void remove(T model, S value);
        }

        private final transient List<PostProcessor<S, T>> postprocessors_ = new ArrayList<PostProcessor<S, T>>();

        protected CollectionAttr(String name, AttributeType<V> type) {
            super(name, type);
        }

        @Override public AttributeType<V> getType() {
            return (AttributeType<V>) super.getType();
        }

        public U snapshot(T model) {
            V mvocollection = super.get(model);
            return mvocollection == null
                ? emptyJavaCollection()
                : mvocollection.get();
        }

        public boolean containsValue(T model, S value) {
            V mvocollection = super.get(model);
            return mvocollection != null && mvocollection.contains(value);
        }

        public void addValue(T model, S value) throws ValueException, ConfigurationException {
            validateAddValue(model, value);

            V mvocollection = super.get(model);
            if (mvocollection == null) {
                mvocollection = newMvoCollection();
                super.set(model, mvocollection);
            }
            mvocollection.add(value);

            for (PostProcessor<S, T> postprocessor : postprocessors()) {
                postprocessor.add(model, value);
            }

            putMvolistsetmap2Model(mvocollection, model);
        }

        public void removeValue(T model, S value) throws ValueException, ConfigurationException {
            validateRemoveValue(model, value);

            V mvocollection = super.get(model);
            if (mvocollection != null) {
                mvocollection.remove(value);
            }

            for (PostProcessor<S, T> postprocessor : postprocessors()) {
                postprocessor.remove(model, value);
            }

            putMvolistsetmap2Model(mvocollection, model);
        }

        abstract public U emptyJavaCollection();
        abstract public V newMvoCollection();

        public void validateAddValue(T model, S value)
            throws ValueException, ConfigurationException
        {
            return;
        }

        public void validateRemoveValue(T model, S value)
            throws ValueException, ConfigurationException
        {
            return;
        }

        public void addPostProcessor(PostProcessor<S, T> postprocessor) {
            postprocessors_.add(postprocessor);
        }

        protected List<PostProcessor<S, T>> postprocessors() {
            return postprocessors_;
        }
    }

    public static class ListAttr<S, T extends Model>
        extends CollectionAttr<S, T, List<S>, MvoList<S>>
    {
        public ListAttr(String name, AttributeType.MvoListType<S> type) {
            super(name, type);
        }

        @Override public AttributeType.MvoListType<S> getType() {
            return (AttributeType.MvoListType<S>) super.getType();
        }

        @Override  public List<S> emptyJavaCollection() {
            return Collections.<S>emptyList();
        }

        @Override public MvoList<S> newMvoCollection() {
            return new MvoList<S>();
        }
    }

    public static class ListStringAttr<T extends Model> extends ListAttr<String, T> {

        public ListStringAttr(String name) {
            super(name, AttributeType.MvoListType.STRING);
        }
    }

    public static class SetAttr<S, T extends Model>
        extends CollectionAttr<S, T, Set<S>, MvoSet<S>>
    {
        public SetAttr(String name, AttributeType.MvoSetType<S> type) {
            super(name, type);
        }

        @Override public AttributeType.MvoSetType<S> getType() {
            return (AttributeType.MvoSetType<S>) super.getType();
        }

        @Override public Set<S> emptyJavaCollection() {
            return Collections.<S>emptySet();
        }

        @Override public MvoSet<S> newMvoCollection() {
            return new MvoSet<S>();
        }
    }

    public static class SetStringAttr<T extends Model> extends SetAttr<String, T> {

        public SetStringAttr(String name) {
            super(name, AttributeType.MvoSetType.STRING);
        }
    }

    public static class SetIntegerAttr<T extends Model> extends SetAttr<Integer, T> {

        public SetIntegerAttr(String name) {
            super(name, AttributeType.MvoSetType.INTEGER);
        }
    }

    public static class SetLongAttr<T extends Model> extends SetAttr<Long, T> {

        public SetLongAttr(String name) {
            super(name, AttributeType.MvoSetType.LONG);
        }
    }

    public static class SetDoubleAttr<T extends Model> extends SetAttr<Double, T> {

        public SetDoubleAttr(String name) {
            super(name, AttributeType.MvoSetType.DOUBLE);
        }
    }

    public static class MapAttr<K, V, T extends Model> extends Attribute<MvoMap<K, V>, T> {

        public static interface PostProcessor<K, V, T extends Model> {

            public void put(T model, K key, V oldValue, V newValue);
            public void remove(T model, K key, V oldValue);
        }

        private final transient List<PostProcessor<K, V, T>> postProcessors_ = new ArrayList<PostProcessor<K, V, T>>();

        public MapAttr(String name, AttributeType.MvoMapType<K, V> type) {
            super(name, type);
        }

        @Override public AttributeType.MvoMapType<K, V> getType() {
            return (AttributeType.MvoMapType<K, V>) super.getType();
        }

        public V get(T model, K key) {
            MvoMap<K, V> mvomap = super.get(model);
            return mvomap == null ? null : mvomap.get(key);
        }

        public Map<K, V> snapshot(T model) {
            Map<K, V> result = new LinkedHashMap<K, V>();
            for (K key : getKeys(model)) {
                result.put(key, get(model, key));
            }
            return result;
        }

        public Set<K> getKeys(T model) {
            MvoMap<K, V> mvomap = super.get(model);
            return mvomap == null ? new HashSet<K>() : mvomap.getKeys();
        }

        public boolean containsKey(T model, K key) {
            MvoMap<K, V> mvomap = super.get(model);
            return mvomap != null && mvomap.containsKey(key);
        }

        public void put(T model, K key, V newValue)
            throws FormatException, ValueException, ConfigurationException
        {
            MvoMap<K, V> mvomap = super.get(model);

            V oldValue = mvomap == null ? null : mvomap.get(key);
            if (oldValue == newValue) {
                return;
            }

            validatePut(model, key, newValue);

            if (mvomap == null) {
                mvomap = new MvoMap<K, V>();
                super.set(model, mvomap);
            }

            mvomap.put(key, newValue);

            for (PostProcessor<K, V, T> postProcessor : postProcessors_) {
                postProcessor.put(model, key, oldValue, newValue);
            }

            putMvolistsetmap2Model(mvomap, model);
        }

        public void remove(T model, K key)
            throws FormatException, ValueException, ConfigurationException
        {
            MvoMap<K, V> mvomap = super.get(model);
            if (mvomap == null || ! mvomap.containsKey(key)) {
                return;
            }

            V oldValue = mvomap.get(key);

            validateRemove(model, key);

            if (mvomap != null) {
                mvomap.remove(key);
            }

            for (PostProcessor<K, V, T> postProcessor : postProcessors_) {
                postProcessor.remove(model, key, oldValue);
            }

            putMvolistsetmap2Model(mvomap, model);
        }

        protected void validatePut(T model, K key, V value)
            throws ValueException, ConfigurationException
        {
            return;
        }

        protected void validateRemove(T model, K key)
            throws ValueException, ConfigurationException
        {
            return;
        }

        public void addPostProcessor(PostProcessor<K, V, T> postProcessor) {
            postProcessors_.add(postProcessor);
        }
    }

    /**
     *  他属性に対する値制約を表現する属性です.
     *  <p>
     *  典型的な {@code <CONSTRAINT>} は型オブジェクトです (例: naef.mvo.NaefObjectType).
     *  <p>
     *  この ConstraintAttr が保持する値が whitelist/blacklist のどちらとして解釈されるかは実装依存
     *  です.
     *
     *  @param <T> 他属性の値制約の判断に使用する値の型です.
     *  @param <CONSTRAINT> この ValueTypeConstraintAttr を定義する制約オブジェクトの型です.
     *  @param <VALUE> 制約対象の属性の値の型です.
     */
    public static abstract
        class ConstraintAttr<T, CONSTRAINT extends Model, VALUE extends Model>
        extends Attribute.SetAttr<T, CONSTRAINT>
    {
        public ConstraintAttr(String name, AttributeType.MvoSetType<T> type) {
            super(name, type);
        }

        public abstract boolean isValueAcceptable(CONSTRAINT constraint, VALUE validatee);
    }

    /**
     *  他属性に対する値制約のうち, 値の型制約の whitelist に特化した属性です.
     *
     *  @param <CONSTRAINT> この ValueTypeConstraintAttr を定義する制約オブジェクトの型です.
     *  @param <VALUE> 制約対象の属性の値の型 (MODEL の属性の型) です.
     *  @param <MODEL> 制約対象の属性を保持するオブジェクトの型です.
     */
    public static abstract
        class ValueTypeConstraintAttr<CONSTRAINT extends Model, VALUE extends Model, MODEL extends Model>
        extends ConstraintAttr<UiTypeName, CONSTRAINT, VALUE>
    {
        /**
         *  単数値属性を対象とする値型制約.
         */
        public abstract static
            class Single<CONSTRAINT extends Model, VALUE extends Model, MODEL extends Model>
            extends ValueTypeConstraintAttr<CONSTRAINT, VALUE, MODEL>
        {
            public Single(String name) {
                super(name);
            }

            @Override public void validateConstraint(MODEL model, CONSTRAINT validatee) {
                if (! isValueAcceptable(validatee, getExistingValue(model))) {
                    throw new ConfigurationException("既に設定されている値の型が " + getName() + " に適合しません.");
                }
            }

            public abstract VALUE getExistingValue(MODEL model);
        }

        /**
         *  複数値属性を対象とする値型制約.
         */
        public abstract static
            class Multi<CONSTRAINT extends Model, VALUE extends Model, MODEL extends Model>
            extends ValueTypeConstraintAttr<CONSTRAINT, VALUE, MODEL>
        {
            public Multi(String name) {
                super(name);
            }

            @Override public void validateConstraint(MODEL model, CONSTRAINT validatee) {
                for (VALUE value : getExistingValues(model)) {
                    if (! isValueAcceptable(validatee, value)) {
                        throw new ConfigurationException("既存の値の型が " + getName() + " に適合しません.");
                    }
                }
            }

            public abstract Collection<? extends VALUE> getExistingValues(MODEL model);
        }

        public ValueTypeConstraintAttr(String name) {
            super(name, AttributeType.MvoSetType.TYPE);
        }

        public abstract void validateConstraint(MODEL model, CONSTRAINT validatee);

        public void validateValue(MODEL model, VALUE validatee) {
            if (! isValueAcceptable(getConstraintAttr().get(model), validatee)) {
                throw new ValueException(getConstraintAttr().getName() + " の型制約 " + getName() + " に適合しません.");
            }
        }

        @Override public boolean isValueAcceptable(CONSTRAINT constraint, VALUE validatee) {
            if (constraint == null || validatee == null) {
                return true;
            }

            for (UiTypeName acceptableType : snapshot(constraint)) {
                if (acceptableType.type().isInstance(validatee)) {
                    return true;
                }
            }

            return false;
        }

        /**
         *  この ValueTypeConstraintAttr を定義する制約オブジェクトの属性メタを返す.
         */
        public abstract Attribute.SingleAttr<CONSTRAINT, MODEL> getConstraintAttr();
    }

    public static synchronized List<Attribute<?, ?>> getDeclaredAttributes(Class<?> attributeClass) {
        List<Attribute<?, ?>> attributes = new ArrayList<Attribute<?, ?>>();
        for (Object o : ReflectionUtils.initializeStaticFinalFields(attributeClass)) {
            if (o instanceof Attribute<?, ?>) {
                attributes.add((Attribute<?, ?>) o);
            }
        }
        return attributes;
    }

    public static List<String> getAttributeNames(Class<? extends Model> type) {
        return new ArrayList<String>(SkeltonTefService.instance().getTypesAttributes().gainInstance(type).names());
    }

    public static List<Attribute<?, ?>> getAttributes(Class<? extends Model> type) {
        return new ArrayList<Attribute<?, ?>>(
            SkeltonTefService.instance().getTypesAttributes().gainInstance(type).attributes());
    }

    public static Attribute<?, ?> getAttribute(Class<? extends Model> type, String name) {
        return SkeltonTefService.instance().getTypesAttributes().gainInstance(type).get(name);
    }

    private static final Map<Class<? extends Model>, List<Attribute<?, ?>>> 
        serializableAttributes__ = new HashMap<Class<? extends Model>, List<Attribute<?, ?>>>();

    public static List<Attribute<?, ?>> getSerializableAttributes(Class<? extends Model> type) {
        List<Attribute<?, ?>> result = serializableAttributes__.get(type);
        if (result == null) {
            result = new ArrayList<Attribute<?, ?>>();
            for (Attribute<?, ?> attr : Attribute.getAttributes(type)) {
                if (attr.getType() != null && attr.getType().isValueSerializable()) {
                    result.add(attr);
                }
            }
            Collections.sort(
                result,
                new Comparator<Attribute<?, ?>>() {

                    @Override public int compare(Attribute<?, ?> o1, Attribute<?, ?> o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
            result = Collections.unmodifiableList(result);
            serializableAttributes__.put(type, result);
        }
        return result;
    }

    public static final Attribute.SingleString<Model> NAME
        = new Attribute.SingleString<Model>("tef.app-skelton.name");

    private final String name_;
    private final AttributeType<S> type_;
    private boolean isShellConfigurable_ = true; 

    public Attribute(String name, AttributeType<S> type) {
        if (name == null) {
            throw new IllegalArgumentException();
        }

        name_ = name;
        type_ = type;
    }

    public String getName() {
        return name_;
    }

    public AttributeType<S> getType() {
        return type_;
    }

    public void set(T model, S newValue) throws ValueException, ConfigurationException {
        model.putValue(name_, newValue);
    }

    public S get(T model) {
        Object value = model.getValue(name_);

        if (getType() == null || getType().getJavaType() == null) {
            return (S) value;
        } else {
            try {
                return getType().getJavaType().cast(value);
            } catch (ClassCastException cce) {
                UiTypeName.Instances uitypenames = SkeltonTefService.instance().uiTypeNames();
                throw new ClassCastException(
                    uitypenames.getName(model.getClass()) + " の " + getName() + " の値の型は "
                    + uitypenames.getName(value.getClass()) + " であり "
                    + uitypenames.getName(getType().getJavaType()) + " には変換できません.");
            }
        }
    }

    public void setString(T model, String valueStr)
        throws FormatException, ValueException, ConfigurationException 
    {
        if (type_ == null) {
            throw new UnsupportedOperationException("type is undefined.");
        }
        if (type_ instanceof AttributeType.MvoCollectionType<?, ?>) {
            throw new ConfigurationException("type is collection type.");
        }

        S value = valueStr == null ? null : type_.parse(valueStr);

        set(model, value);
    }

    public String getString(T model) {
        S value = get(model);
        if (value == null) {
            return null;
        }

        return type_ == null ? value.toString() : type_.format(value);
    }

    public boolean isShellConfigurable() {
        return isShellConfigurable_;
    }

    public void setShellConfigurable(boolean value) {
        isShellConfigurable_ = value;
    }

    public static final Object MVOLISTSETMAP2MODEL_KEY = new Object();

    private static void putMvolistsetmap2Model(MVO listsetmap, Model model) {
        Map<MVO, MVO> mapping = (Map<MVO, MVO>) TransactionContext.getPostProcessorExtInfo(MVOLISTSETMAP2MODEL_KEY);
        if (mapping == null) {
            mapping = new HashMap<MVO, MVO>();
            TransactionContext.putPostProcessorExtInfo(MVOLISTSETMAP2MODEL_KEY, mapping);
        }
        mapping.put(listsetmap, (MVO) model);
    }
}
