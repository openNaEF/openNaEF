package opennaef.rest.api.spawner;

import opennaef.rest.DEBUG;
import opennaef.rest.NaefRmiConnector;
import opennaef.rest.api.config.AttrNameMapping;
import opennaef.rest.api.config.NaefApiConfig;
import opennaef.rest.api.response.BadRequest;
import opennaef.rest.api.response.Responses;
import naef.dto.NaefDto;
import naef.dto.PortDto;
import tef.DateTime;
import tef.MVO;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.NamedModel;
import tef.skelton.RmiDtoFacade;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoDesc;
import tef.skelton.dto.MvoDtoOriginator;
import tef.skelton.dto.MvoOid;
import voss.core.server.util.DtoUtil;

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dtoの生成、DtoからJSONへの変換を行う
 * <p>
 * Dto-JSON変換は、AttrNameMappingを元に属性名の変換を行う
 */
public class DtoSpawner {
    /**
     * MvoIdからDtoを生成する
     *
     * @param mvoId
     * @return Dto
     */
    public static EntityDto spawn(MVO.MvoId mvoId) {
        long start = System.nanoTime();
        RmiDtoFacade dtoFacade;
        try {
            dtoFacade = NaefRmiConnector.instance().dtoFacade();
        } catch (RemoteException e) {
            throw new IllegalStateException("naef 接続に失敗.");
        }

        long dtoFacadeEnd = System.nanoTime();
        Tuning.facade(dtoFacadeEnd - start);

        MvoDtoOriginator originator = new MvoDtoOriginator(dtoFacade);
        long originatorEnd = System.nanoTime();
        Tuning.originator(originatorEnd - dtoFacadeEnd);

        EntityDto dto = originator.getDto(new MvoOid(mvoId));
        long dtoEnd = System.nanoTime();
        Tuning.dto(dtoEnd - originatorEnd);

        return dto;
    }

    public static EntityDto spawn(MvoDtoDesc<?> desc) {
        return spawn(desc.getMvoId());
    }

    /**
     * DtoからJSONのもとになるMapを生成する
     * Dtoの属性名をそのままJSONで返すと使いづらいため、AttrNameMappingで属性名の変換を行う
     * <p>
     * "$time": Dto読み出し時の時間
     * "$raw": Dto読み出し時のTransactionId
     * "$raw": 属性名変換前のDtoの属性名とDtoの属性値
     *
     * @param dto
     * @return JSONのもとになるMap
     */
    public static Map<String, Object> toMap(EntityDto dto) {
        if (dto == null) throw new IllegalArgumentException("dto is null.");

        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("id", dto.getOid().toString());
        attrs.put("absolute_name", DtoUtil.getAbsoluteName(dto));
        if (dto instanceof NaefDto) {
            attrs.put("object_type_name", ((NaefDto) dto).getObjectTypeName());
        }

        DateTime time = new DateTime(TransactionContext.getTargetTime());
        TransactionId.W tx = TransactionContext.getTargetVersion();
        attrs.put("$time", time.getValue());
        attrs.put("$version", tx.getIdString());

        // すべての属性値を取り出す
        Map<String, Object> dtoAttrs = new TreeMap<>();
        for (String attr : dto.getAttributeNames()) {
            Object value = dto.getValue(attr);
            if (value != null) {
                dtoAttrs.put(attr, Values.toValue(value));
            }
        }

        // AttrNameMapping がある場合は設定に書かれている属性を出力する
        // dtoAttrs は "$raw" へ入れる(debug用)
        AttrNameMapping.Mapping attrNameMapping = AttrNameMapping.instance().mapping(((NaefDto) dto).getObjectTypeName());
        if (attrNameMapping != null) {
            for (Map.Entry<String, AttrNameMapping.Attr> entry : attrNameMapping.attrs().entrySet()) {
                AttrNameMapping.Attr attr = entry.getValue();
                if (dtoAttrs.containsKey(attr.dtoName())) {
                    Object value = dtoAttrs.get(attr.dtoName());
                    attrs.put(attr.name(), value);
                }
            }
            if (DEBUG.isEnabled()) {
                attrs.put("$raw", dtoAttrs);
            }
        } else {
            attrs.putAll(dtoAttrs);
        }
        return attrs;
    }

    public static Map<String, String> createMvoLink(MvoDtoDesc desc, DateTime time, TransactionId.W tx) {
        return createMvoLink(desc.getMvoId(), time, tx);
    }

    public static Map<String, String> createMvoLink(MVO.MvoId mvoId, DateTime time, TransactionId.W tx) {
        return createMvoLink(DtoSpawner.spawn(mvoId), time, tx);
    }

    /**
     * あるオブジェクトへのuriを持つMvoLinkを生成する
     * {
     * "id": "mvo-id",
     * "object_type_name": "NaefDto#getObjectTypeName",
     * "name": "NodeElementDto#getName",
     * "if-name": "PortDto#getIfname",
     * "href": "http://path/to/resources/mvo-id",
     * "rel": "relation"
     * }
     *
     * @param dto
     * @param time トランザクションに設定した時間
     * @param tx   トランザクションに設定したバージョン
     * @return MvoLink
     */
    public static Map<String, String> createMvoLink(EntityDto dto, DateTime time, TransactionId.W tx) {
        Map<String, String> link = new LinkedHashMap<>();
        String type = ((NaefDto) dto).getObjectTypeName();
        link.put("id", dto.getOid().toString());
        link.put("object_type_name", type);

        if (dto instanceof NamedModel) {
            link.put("name", ((NamedModel) dto).getName());
        }
        if (dto instanceof PortDto) {
            link.put("if_name", ((PortDto) dto).getIfname());
        }

        NaefApiConfig conf = NaefApiConfig.instance();
        String location = "http://" +
                conf.apiIpAddr() + ":" + conf.apiPort() +
                Responses.getLocation(dto, time, tx);
        link.put("href", location);
        link.put("rel", type);
        return link;
    }

    /**
     * mvo-idのフォーマットであるかをチェックする
     *
     * @param mvoIdStr
     * @return
     */
    public static MVO.MvoId getMvoId(String mvoIdStr) throws BadRequest {
        try {
            return MVO.MvoId.getInstanceByLocalId(mvoIdStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequest("NAEF-00410", "不正なIDです. " + mvoIdStr);
        }
    }
}
