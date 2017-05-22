package opennaef.notifier;

import net.arnx.jsonic.JSONHint;
import tef.skelton.dto.DtoChanges;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * 通知用POJO
 */
public class Notify {
    private final Type _type;
    /**
     * 通知を出した時間
     */
    private final long _notifyTime;

    /**
     * トランザクション ID
     */
    private final String _targetVersion;

    /**
     * トランザクションに指定された時間
     */
    private final long _targetTime;

    /**
     * DtoChanges
     *
     * この属性はJSON化されない
     */
    private transient final DtoChanges _rawDtoChanges;

    /**
     * NaEF Restful API DtoChanges URI
     */
    private final String _uri;

    /**
     * DtoChanges JSON
     * <p>
     * NaEF Restful API から取得した DtoChanges JSON
     */
    private final Map<String, Object> _dtoChanges;

    public Notify(Type type, long notifyTime, DtoChanges dtoChanges, URL url, Map<String, Object> json) {
        _type = type;
        _notifyTime = notifyTime;
        _targetVersion = dtoChanges.getTargetVersion().toString();
        _targetTime = dtoChanges.getTargetTime();
        _dtoChanges = Collections.unmodifiableMap(json);
        _uri = url.toString();
        _rawDtoChanges = dtoChanges;
    }

    public Type getType() {
        return _type;
    }

    public long getNotifyTime() {
        return _notifyTime;
    }

    public String getTargetVersion() {
        return _targetVersion;
    }

    public long getTargetTime() {
        return _targetTime;
    }

    public String getUri() {
        return _uri;
    }

    public Map<String, Object> getDtoChanges() {
        return _dtoChanges;
    }

    @JSONHint(ignore=true)
    public DtoChanges getRawDtoChanges() {
        return _rawDtoChanges;
    }

    public String toString() {
        return "Notify{"
                + "type=" + _type
                + " notifyTime=" + _notifyTime
                + " targetVersion=" + _targetVersion
                + " targetTime=" + _targetTime
                + "}";
    }

    public enum Type {
        ping, commit, scheduled;
    }
}
