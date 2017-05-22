package tef.skelton;

import tef.MVO;
import tef.TransactionId;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class AbstractModel extends MVO implements Model {

    private final M1<String, Object> attributes_ = new M1<String, Object>();

    public AbstractModel(MvoId id) {
        super(id);
    }

    public AbstractModel() {
    }

    @Override public SortedSet<String> getAttributeNames() {
        return new TreeSet<String>(attributes_.getKeys());
    }

    @Override public <S, T extends Model> void set(Attribute<S, ? super T> attr, S value) {
        attr.set((T) this, value);
    }

    @Override public <S, T extends Model> S get(Attribute<S, ? super T> attr) {
        return attr.get((T) this);
    }

    @Override public void putValue(String key, Object value) {
        key = key == null ? null : key.intern();
        if (value != null && value instanceof String) {
            value = ((String) value).intern();
        }

        Object currentValue = getValue(key);
        if (value == null ? currentValue != null : ! value.equals(currentValue)) {
            attributes_.put(key, value);
        }
    }

    @Override public Object getValue(String key) {
        return attributes_.get(key);
    }

    public List<TransactionId.W> getAttributeChangedVersions() {
        TransactionId.W[] result = tef.TransactionIdAggregator.getTransactionIds(attributes_);
        return Arrays.<TransactionId.W>asList(result);
    }

    public TransactionId.W getExtendedVersion() {
        TransactionId.W result = getLatestVersion();
        for (Attribute<?, ?> attr : Attribute.getAttributes(getClass())) {
            if (attr instanceof Attribute.CollectionAttr<?, ?, ?, ?>) {
                MvoCollection<?, ?> mvocollection = get((Attribute.CollectionAttr<?, Model, ?, ?>) attr);
                if (mvocollection != null) {
                    TransactionId.W mvocollectionVersion = mvocollection.getLatestVersion();
                    if (mvocollectionVersion.serial > result.serial) {
                        result = mvocollectionVersion;
                    }
                }
            }

            if (attr instanceof Attribute.MapAttr<?, ?, ?>) {
                MvoMap<?, ?> mvomap = (MvoMap<?, ?>) get((Attribute.MapAttr<?, ?, Model>) attr);
                if (mvomap != null) {
                    TransactionId.W mvomapVersion = mvomap.getLatestVersion();
                    if (mvomapVersion.serial > result.serial) {
                        result = mvomapVersion;
                    }
                }
            }
        }
        return result;
    }

    public String uiTypeName() {
        return SkeltonTefService.instance().uiTypeNames().getName(getClass());
    }
}
