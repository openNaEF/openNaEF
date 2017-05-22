package opennaef.rest.api.resource;

import naef.mvo.of.OfPatchLink;
import opennaef.rest.SchedulableShellConnector;
import opennaef.rest.api.AutoCloseableTx;
import opennaef.rest.api.CRUD;
import opennaef.rest.api.MVOs;
import opennaef.rest.api.Requests;
import opennaef.rest.api.config.CmdBuilderMapping;
import opennaef.rest.api.config.AttrNameMapping;
import opennaef.rest.api.config.ConfigurationException;
import opennaef.rest.api.config.api.ApiConfig;
import opennaef.rest.api.helper.CmdBuilderProcessor;
import opennaef.rest.api.response.*;
import opennaef.rest.api.spawner.ConvertVariable;
import opennaef.rest.api.spawner.DtoSpawner;
import opennaef.rest.api.spawner.Tuning;
import opennaef.rest.api.spawner.Values;
import naef.dto.NaefDto;
import naef.mvo.Network;
import naef.mvo.P2pLink;
import naef.mvo.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pasaran.naef.PasaranNaefService;
import tef.DateTime;
import tef.MVO;
import tef.TransactionId;
import tef.skelton.dto.DtoChanges;
import tef.skelton.dto.EntityDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.exception.InventoryException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * naef restful api
 * ApiConfig から設定を読み込み、処理を行う
 */
@Path("/")
public class ConfigurableMvoApi {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableMvoApi.class);

    /* FIXME link の port1, port2 属性が時間を持っていないため link -> port の関連を切ることができない
    // そのため、link から port をたどり、port の networks に link が存在しない場合、
    // その link は存在しないものとして扱う
    */
    public static final Predicate<MVO> workaroundFilter = mvo -> {
        if (mvo instanceof P2pLink) {
            Port port = ((P2pLink) mvo).getPort1();
            if (port == null) {
                port = ((P2pLink) mvo).getPort2();
            }
            if (port == null) {
                return false;
            }
            Optional find = port.getCurrentNetworks(Network.class).stream()
                    .filter(network -> network == mvo)
                    .findFirst();
            return find.isPresent();
        }
        if (mvo instanceof OfPatchLink) {
            return ((OfPatchLink) mvo).getCurrentAttachedPorts().size() > 0;
        }
        return true;
    };

    /**
     * 指定された type のDtoをすべて返す
     *
     * @param uri
     * @param type    MVOの型
     * @param time    トランザクションに指定する時間(Epoch time millis)
     * @param version トランザクションに指定するバージョン
     * @return json
     * @throws ApiException
     */
    @GET
    @Path("/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response getAll(
            @Context UriInfo uri,
//            @Context HttpHeaders headers,
            @PathParam("type") String type,
            @QueryParam("deref") boolean deref,
            @QueryParam("time") Date time,
            @QueryParam("version") TransactionId.W version
    ) throws ApiException {
        log.debug("[handle] GET --- " + uri.getPath());
        ApiConfig content = Requests.getContextConf(type);

        try (AutoCloseableTx tx = AutoCloseableTx.beginTx(time, version)) {
            List<Map<String, ?>> list = MVOs.findMVOs(content.mvoClasses).stream()
                    .filter(workaroundFilter)
                    .map(mvo -> {
                        if (deref) {
                            return DtoSpawner.toMap(DtoSpawner.spawn(mvo.getMvoId()));
                        } else {
                            return DtoSpawner.createMvoLink(mvo.getMvoId(), null, null);
                        }
                    })
                    .collect(Collectors.toList());
            log.info("[result] GET --- " + content.context + ": " + list.size());
            Tuning.dump();
            return Response.ok(Responses.json.format(list), MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    /**
     * 指定された id のDtoを返す
     *
     * @param uri
     * @param type    MVOの型
     * @param id      MVO ID
     * @param time    トランザクションに指定する時間(Epoch time millis)
     * @param version トランザクションに指定するバージョン
     * @return json
     * @throws ApiException
     */
    @GET
    @Path("/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response get(
            @Context UriInfo uri,
//            @Context HttpHeaders headers,
            @PathParam("type") String type,
            @PathParam("id") String id,
            @QueryParam("time") Date time,
            @QueryParam("version") TransactionId.W version
    ) throws ApiException {
        log.debug("[handle] GET --- " + uri.getPath());
        ApiConfig content = Requests.getContextConf(type);

        try (AutoCloseableTx tx = AutoCloseableTx.beginTx(time, version)) {
            MVO.MvoId mvoId = DtoSpawner.getMvoId(id);
            MVO targetMvo = PasaranNaefService.instance().getMvoRegistry().get(mvoId);
            if (!workaroundFilter.test(targetMvo)) {
                // FIXME ワークアラウンド
                throw new NotFound();
            }
            EntityDto dto = DtoSpawner.spawn(mvoId);
            if (dto == null || !content.isAllowedType(dto)) {
                // mvo が存在しない -> 404
                // この uri で許可されないオブジェクトタイプ -> 404
                throw new NotFound();
            }

            log.debug("target: " + dto.getOid());
            Map<String, Object> attrs = DtoSpawner.toMap(dto);
            return Response.ok(Responses.json.format(attrs), MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    /**
     * 指定された id のDtoの属性を返す
     *
     * @param uri
     * @param type     MVOの型
     * @param id       MVO ID
     * @param attrName Dto の属性名
     * @param deref    MvoLink を Dto化する場合に true
     * @param time     トランザクションに指定する時間(Epoch time millis)
     * @param version  トランザクションに指定するバージョン
     * @return json
     * @throws ApiException
     */
    @GET
    @Path("/{type}/{id}/{attr-name}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response getChildlen(
            @Context UriInfo uri,
//            @Context HttpHeaders headers,
            @PathParam("type") String type,
            @PathParam("id") String id,
            @PathParam("attr-name") String attrName,
            @QueryParam("deref") boolean deref,
            @QueryParam("time") Date time,
            @QueryParam("version") TransactionId.W version
    ) throws ApiException {
        log.debug("[handle] GET --- " + uri.getPath());
        ApiConfig content = Requests.getContextConf(type);

        try (AutoCloseableTx tx = AutoCloseableTx.beginTx(time, version)) {
            MVO.MvoId mvoId = DtoSpawner.getMvoId(id);
            MVO targetMvo = PasaranNaefService.instance().getMvoRegistry().get(mvoId);
            if (!workaroundFilter.test(targetMvo)) {
                // FIXME ワークアラウンド
                throw new NotFound();
            }
            EntityDto dto = DtoSpawner.spawn(mvoId);
            if (dto == null || !content.isAllowedType(dto)) {
                // mvo が存在しない -> 404
                // この uri で許可されないオブジェクトタイプ -> 404
                throw new NotFound();
            }

            // 属性名マッピングから Dto の属性名を引き当てる
            String dtoAttrName;
            Optional<AttrNameMapping.Mapping> attrMapping = Optional.ofNullable(AttrNameMapping.instance().mapping(((NaefDto) dto).getObjectTypeName()));
            if (attrMapping.isPresent()) {
                Optional<AttrNameMapping.Attr> attr = Optional.ofNullable(attrMapping.get().attr(attrName));
                if (!attr.isPresent()) {
                    throw new NotFound();
                }
                dtoAttrName = attr.get().dtoName();
            } else {
                dtoAttrName = attrName;
            }

            ConvertVariable.setDeref(deref);
            Object dtoValue = dto.getValue(dtoAttrName);
            Object returnValue = Values.toValue(dtoValue);
            ConvertVariable.init();

            log.debug("target: " + dto.getOid() + " - " + dtoAttrName);
            return Response.ok(Responses.json.format(returnValue), MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    /**
     * 指定した type の MVO を新規作成する
     *
     * @param uri
     * @param request
     * @param type    MVOの型
     * @param time    トランザクションに指定する時間(Epoch time millis)
     * @return 新規作成したMVOのuri
     * @throws ApiException
     */
    @POST
    @Path("/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static Response create(
            @Context UriInfo uri,
//            @Context HttpHeaders headers,
            @Context HttpServletRequest request,
            @PathParam("type") String type,
            @QueryParam("time") Date time
    ) throws ApiException {
        log.debug("[handle] POST --- " + uri.getPath());

        final Map<String, Object> reqArgs = Requests.toReqAttrs(request, type, null);
        try {
            Long timeMillis = time != null ? time.getTime() : System.currentTimeMillis();
            List<CommandBuilder> builder;
            try (AutoCloseableTx tx = AutoCloseableTx.beginTx(time)) {
                builder = CmdBuilderProcessor.build(CRUD.CREATE, type, reqArgs);
            }
            DtoChanges dtoChanges = SchedulableShellConnector.getInstance().executes(new DateTime(timeMillis), builder);

            // 新規作成されたdtoのURIを返す
            ApiConfig content = CmdBuilderMapping.instance().content(type);
            Optional<EntityDto> created = dtoChanges.getNewObjects().stream()
                    .filter(item -> content.dtoClasses.contains(item.getClass()))
                    .findFirst();
            if (created.isPresent()) {
                String location = Responses.getAbsoluteLocation(request, created.get(), null, null);
                return Response.created(new URI(location)).build();
            } else {
                return Response.ok().build();
            }
        } catch (InventoryException e) {
            // コマンドの実行に失敗 Bad Request
            log.info("builderConfig execute", e);
            throw new BadRequest("NAEF-00400", "更新失敗. " + e.getCause().getMessage(), e);
        } catch (ConfigurationException e) {
            log.error("builderConfig execute", e);
            throw new InternalServerError("API-01500", "設定ファイルエラー.", e);
        } catch (URISyntaxException e) {
            throw new InternalServerError("API-00500", "レスポンス送信失敗. URI 生成に失敗.", e);
        }
    }

    /**
     * 指定した type の MVO を更新する
     *
     * @param uri
     * @param request
     * @param type    MVOの型
     * @param id      MVO ID
     * @param time    トランザクションに指定する時間(Epoch time millis)
     * @return
     * @throws ApiException
     */
    @PUT
    @Path("/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static Response update(
            @Context UriInfo uri,
//            @Context HttpHeaders headers,
            @Context HttpServletRequest request,
            @PathParam("type") String type,
            @PathParam("id") String id,
            @QueryParam("time") Date time
    ) throws ApiException {
        log.debug("[handle] PUT --- " + uri.getPath());

        final Map<String, Object> reqArgs = Requests.toReqAttrs(request, type, id);

        try {
            Long timeMillis = time != null ? time.getTime() : System.currentTimeMillis();
            List<CommandBuilder> builder;
            try (AutoCloseableTx tx = AutoCloseableTx.beginTx(time)) {
                builder = CmdBuilderProcessor.build(CRUD.UPDATE, type, reqArgs);
            }
            SchedulableShellConnector.getInstance().executes(new DateTime(timeMillis), builder);
            return Response.ok().build();
        } catch (InventoryException e) {
            // コマンドの実行に失敗 Bad Request
            log.info("builderConfig execute", e);
            throw new BadRequest("NAEF-00400", "更新失敗. " + e.getCause().getMessage(), e);
        } catch (ConfigurationException e) {
            log.error("builderConfig execute", e);
            throw new InternalServerError("API-01500", "設定ファイルエラー.", e);
        }
    }

    /**
     * 指定した type の MVO を論理削除する
     * <p>
     * naefに物理削除は存在しない
     *
     * @param uri
     * @param request
     * @param type    MVOの型
     * @param id      MVO ID
     * @param time    トランザクションに指定する時間(Epoch time millis)
     * @return
     * @throws ApiException
     */
    @DELETE
    @Path("/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response delete(
            @Context UriInfo uri,
//            @Context HttpHeaders headers,
            @Context HttpServletRequest request,
            @PathParam("type") String type,
            @PathParam("id") String id,
            @QueryParam("time") Date time
    ) throws ApiException {
        log.debug("[handle] DELETE --- " + uri.getPath());

        final Map<String, Object> reqArgs = Requests.toReqAttrs(request, type, id);
        try {
            Long timeMillis = time != null ? time.getTime() : System.currentTimeMillis();
            List<CommandBuilder> builder;
            try (AutoCloseableTx tx = AutoCloseableTx.beginTx(time)) {
                builder = CmdBuilderProcessor.build(CRUD.DELETE, type, reqArgs);
            }
            SchedulableShellConnector.getInstance().executes(new DateTime(timeMillis), builder);
            return Response.ok().build();
        } catch (InventoryException e) {
            // コマンドの実行に失敗 Bad Request
            log.info("builderConfig execute", e);
            throw new BadRequest("NAEF-00400", "更新失敗. " + e.getCause().getMessage(), e);
        } catch (ConfigurationException e) {
            log.error("builderConfig execute", e);
            throw new InternalServerError("API-00500", "設定ファイルエラー.", e);
        }
    }
}
