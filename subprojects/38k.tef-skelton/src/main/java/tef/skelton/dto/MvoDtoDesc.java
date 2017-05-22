package tef.skelton.dto;

import tef.MVO;
import tef.TefService;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.AbstractModel;
import tef.skelton.Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MvoDtoDesc<T extends EntityDto> implements EntityDto.Desc<T> {

    public static <T extends EntityDto> MvoDtoDesc<T> build1(Object mvo) {
        return mvo == null ? null : new MvoDtoDesc<T>((MVO) mvo);
    }

    public static <T extends EntityDto> Set<EntityDto.Desc<T>> buildS(Set<?> objects) {
        Set<EntityDto.Desc<T>> result = new HashSet<EntityDto.Desc<T>>();
        if (objects != null) {
            for (Object obj : objects) {
                result.add(MvoDtoDesc.<T>build1(obj));
            }
        }
        return result;
    }

    public static <T extends EntityDto> List<EntityDto.Desc<T>> buildL(List<?> objects) {
        List<EntityDto.Desc<T>> result = new ArrayList<EntityDto.Desc<T>>();
        if (objects != null) {
            for (Object obj : objects) {
                result.add(MvoDtoDesc.<T>build1(obj));
            }
        }
        return result;
    }

    /**
     * メソッド名の 'Mk' は 'Map の Key' が entity であることを意味します.
     * 
     * @see #buildMv(Map)
     * @see #buildMkv(Map)
     */
    public static <K extends EntityDto, V> Map<EntityDto.Desc<K>, V> buildMk(Map<? extends Model, ?> map) {
        Map<EntityDto.Desc<K>, V> result = new LinkedHashMap<EntityDto.Desc<K>, V>();
        for (Map.Entry<? extends Model, ?> entry : map.entrySet()) {
            result.put(MvoDtoDesc.<K>build1(entry.getKey()), (V) entry.getValue());
        }
        return result;
    }

    /**
     * メソッド名の 'Mv' は 'Map の Value' が entity であることを意味します.
     * 
     * @see #buildMk(Map)
     * @see #buildMkv(Map)
     */
    public static <K, V extends EntityDto> Map<K, EntityDto.Desc<V>> buildMv(Map<?, ? extends Model> map) {
        Map<K, EntityDto.Desc<V>> result = new LinkedHashMap<K, EntityDto.Desc<V>>();
        for (Map.Entry<?, ? extends Model> entry : map.entrySet()) {
            result.put((K) entry.getKey(), MvoDtoDesc.<V>build1(entry.getValue()));
        }
        return result;
    }

    /**
     * メソッド名の 'Mkv' は 'Map の Key と Value' が entity であることを意味します.
     * 
     * @see #buildMk(Map)
     * @see #buildMv(Map)
     */
    public static <K extends EntityDto, V extends EntityDto>
        Map<EntityDto.Desc<K>, EntityDto.Desc<V>> buildMkv(Map<? extends Model, ? extends Model> map)
    {
        Map<EntityDto.Desc<K>, EntityDto.Desc<V>> result = new LinkedHashMap<EntityDto.Desc<K>, EntityDto.Desc<V>>();
        for (Map.Entry<? extends Model, ? extends Model> entry : map.entrySet()) {
            result.put(MvoDtoDesc.<K>build1(entry.getKey()), MvoDtoDesc.<V>build1(entry.getValue()));
        }
        return result;
    }

    private static final long serialVersionUID = 0L;

    private final String tefServiceName_;
    private final TransactionId.W timestamp_;
    private final long time_;
    private final MvoOid mvoid_;
    private final TransactionId.W version_;

    private MvoDtoDesc(MVO mvo) {
        tefServiceName_ = TefService.instance().getServiceName();
        mvoid_ = mvo == null ? null : new MvoOid(mvo.getMvoId());
        version_ = computeMvoVersion(mvo);
        timestamp_ = TransactionContext.getTargetVersion();
        time_ = TransactionContext.getTargetTime();
    }

    @Override public MvoOid oid() {
        return mvoid_;
    }

    static TransactionId.W computeMvoVersion(MVO mvo) {
        return mvo == null
            ? null
            : (mvo instanceof AbstractModel
                ? ((AbstractModel) mvo).getExtendedVersion()
                : mvo.getLatestVersion());
    }

    public String getTefServiceName() {
        return tefServiceName_;
    }

    /**
     * MvoDto 生成時の transaction context target version です.
     */
    public TransactionId.W getTimestamp() {
        return timestamp_;
    }

    /**
     * MvoDto 生成時の transaction context target time です.
     */
    public long getTime() {
        return time_;
    }

    public MVO.MvoId getMvoId() {
        return mvoid_.oid;
    }

    /**
     * MVO の最終更新 version です.
     */
    public TransactionId.W getVersion() {
        return version_;
    }

    @Override public String toString() {
        return mvoid_.oid + ":" + version_ + ":" + time_+ ":" + timestamp_;
    }

    @Override public int hashCode() {
        return getMvoId().hashCode() + getVersion().hashCode();
    }

    @Override public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        MvoDtoDesc<?> another = getClass().cast(o);
        return (tefServiceName_ == null
                 ? another.tefServiceName_ == null
                 : tefServiceName_.equals(another.tefServiceName_))
            && (mvoid_ == null
                 ? another.mvoid_ == null
                 : mvoid_.oid.equals(another.mvoid_.oid))
            && (version_ == null
                 ? another.version_ == null
                 : version_.equals(another.version_))
            && time_ == another.time_;
    }
}
