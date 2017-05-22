package opennaef.rest.api.spawner;

import opennaef.rest.NaefRmiConnector;
import tef.DateTime;
import tef.TransactionId;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;

import java.rmi.RemoteException;
import java.util.*;

/**
 * 時間・バージョンからDtoChangesを再生成する。
 */
public class DtoChangesSpawner {

    /**
     * DtoChanges をJSONの元になるMapへ変換する
     * {
     * "new-objects": [
     * {...}
     * ],
     * "changed-objects": [
     * {
     * ...
     * "changed-attributes": {
     * "attr-name": {
     * "value": "現在の値",
     * "pre": "変更前の値"
     * },
     * "list-attr": {
     * "added": [{...}],
     * "removed": [{...}]
     * },
     * "map-attr": {
     * "added": {
     * "attr-name": "現在の値"
     * },
     * "removed": {
     * "attr-name": "削除前の値"
     * },
     * "updated": {
     * "attr-name": {
     * "value": "現在の値",
     * "pre": "変更前の値"
     * }
     * }
     * }
     * }
     * }
     * ]
     * }
     *
     * @param dtoChanges
     * @return JSONの元になるMap
     */
    public static Map<String, Object> toMap(DtoChanges dtoChanges) {
        DateTime time = new DateTime(dtoChanges.getTargetTime());
        TransactionId.W tx = dtoChanges.getTargetVersion();

        List<Map<String, Object>> news = new ArrayList<>();
        for (EntityDto dto : dtoChanges.getNewObjects()) {
            Map<String, Object> map = DtoSpawner.toMap(dto);
            news.add(map);
        }

        List<Map<String, Object>> changed = new ArrayList<>();
        for (EntityDto dto : dtoChanges.getChangedObjects()) {
            Map<String, Object> map = DtoSpawner.toMap(dto);
            changed.add(map);

            // 変更前後の値
            Map<String, Object> changedAttrs = new LinkedHashMap<>();
            Set<String> changedAttrNames = dtoChanges.getChangedAttributeNames(dto);
            for (String attrName : changedAttrNames) {
                Object pre = dtoChanges.getPreChangeValue(dto, attrName);
                Object post = dto.getValue(attrName);

                Map<String, Object> change = new LinkedHashMap<>();
                if (pre instanceof Set || post instanceof Set) {
                    DtoChanges.CollectionChange collectionChange = dtoChanges.getCollectionChange(dto, attrName);
                    change.put("added", Values.toValue(collectionChange.getAddedValues(), time, tx));
                    change.put("removed", Values.toValue(collectionChange.getRemovedValues(), time, tx));
                } else if (pre instanceof Map || post instanceof Map) {
                    DtoChanges.MapChange mapChange = dtoChanges.getMapChange(dto, attrName);

                    // added
                    Map<Object, Object> added = new LinkedHashMap<>();
                    for (Object key : mapChange.getAddedKeys()) {
                        Object value = mapChange.getAddedValue(key);
                        added.put(key, Values.toValue(value, time, tx));
                    }

                    // removed
                    Map<Object, Object> removed = new LinkedHashMap<>();
                    for (Object key : mapChange.getRemovedKeys()) {
                        Object value = mapChange.getRemovedValue(key);
                        removed.put(key, Values.toValue(value, time, tx));
                    }

                    // updated
                    Map<Object, Object> updated = new LinkedHashMap<>();
                    for (Object key : mapChange.getAlteredKeys()) {
                        Object preValue = mapChange.getAlteredPreValue(key);
                        Object postValue = mapChange.getAlteredPostValue(key);

                        Map<String, Object> values = new LinkedHashMap<>();
                        values.put("value", Values.toValue(postValue, time, tx));
                        values.put("pre", Values.toValue(preValue, time, tx));
                        updated.put(key, values);
                    }
                    change.put("added", added);
                    change.put("removed", removed);
                    change.put("updated", updated);
                } else {
                    change.put("value", Values.toValue(post, time, tx));
                    change.put("pre", Values.toValue(pre, time, tx));
                }
                changedAttrs.put(attrName, change);
            }
            map.put("changed-attributes", changedAttrs);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("@version", tx.toString());
        res.put("@time", time.getValue());
        res.put("new_objects", news);
        res.put("changed_objects", changed);
        return res;
    }

    /**
     * 指定された時間・バージョンからDtoChangesを再生成する。
     * <p>
     * 時間: 現在の時間
     * バージョン: 最新のバージョン
     *
     * @param targetTx
     * @return DtoChanges
     * @throws RemoteException
     */
    public static DtoChanges getDtoChanges(TransactionId.W targetTx) throws RemoteException {
        return NaefRmiConnector.instance().dtoFacade().getDtoChanges(targetTx, true, targetTx, true);
    }
}
