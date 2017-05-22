package opennaef.rest.api.helper;

import naef.ui.NaefDtoFacade;
import opennaef.rest.Classes;
import opennaef.rest.NaefRmiConnector;
import opennaef.rest.api.CRUD;
import opennaef.rest.api.config.CmdBuilderMapping;
import opennaef.rest.api.config.ConfigurationException;
import opennaef.rest.api.config.api.ApiConfig;
import opennaef.rest.api.config.api.ArgConfig;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.BadRequest;
import opennaef.rest.api.response.InternalServerError;
import opennaef.rest.api.response.MethodNotAllowed;
import opennaef.rest.api.spawner.DtoSpawner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO;
import tef.skelton.ResolveException;
import tef.skelton.dto.EntityDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.builder.NodeCommandBuilder;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CmdBuilderMappingConfig を元にCommandBuilderを生成する
 * <p>
 * 可能な限りErrorResponseExceptionを投げる
 */
public class CmdBuilderProcessor {
    private static final Logger log = LoggerFactory.getLogger(CmdBuilderProcessor.class);

    public static final String PREFIX_URI_PATH = "$";
    public static final String PREFIX_URI_QUERY = "&";
    public static final String PREFIX_HEADER = "%";

    public static final Pattern listAttr = Pattern.compile(".+\\[(\\d+)\\]$");

    public static List<CommandBuilder> build(CRUD crud, String contextName, Map<String, Object> rawArgs) throws ApiException, ConfigurationException {
        if (crud == null || crud == CRUD.READ) throw new MethodNotAllowed();

        final ApiConfig conf = CmdBuilderMapping.instance().content(contextName);
        if (conf == null) {
            // 設定が存在しない == サポートしていないHttpMethod
            throw new MethodNotAllowed();
        }
        List<CommandBuilder> builders;
        try {
            builders = conf.helper.help(crud, conf, rawArgs);
            switch (crud) {
                case CREATE:
                case UPDATE:
                    for (CommandBuilder builder : builders) {
                        builder.buildCommand();
                    }
                    break;
                case DELETE:
                    // FIXME ワークアラウンド
                    if (builders instanceof NodeCommandBuilder) {
                        ((NodeCommandBuilder) builders).setCascadeDelete(true);
                    }
                    for (CommandBuilder builder : builders) {
                        builder.buildDeleteCommand();
                    }
            }
            // TODO BuildResult.Fail の場合にはExceptionを投げる


        } catch (IOException | ExternalServiceException e) {
            // IOException: naef接続の際のコンフィグを読み込みで発生する Internal Server Error
            // ExternalServiceException: naef接続に失敗した場合に発生する Internal Server Error
            log.error("build", e);
            throw new InternalServerError("NAEF-00500", "NAEF通信エラー.", e);
        } catch (InventoryException e) {
            // コマンドの実行に失敗 Bad Request
            log.info("build", e);
            throw new BadRequest("API-02400", e.getMessage(), e);
        }
        return builders;
    }

    static Object getArg(Map<String, Object> attrs, ArgConfig conf) throws ClassNotFoundException, ApiException {
        final String attrName = conf.attrName;
        final Object raw;

        // list attr
        Matcher listAttrMatcher = listAttr.matcher(attrName);
        if (listAttrMatcher.find()) {
            String _attrName = attrName.substring(0, attrName.lastIndexOf('['));
            @SuppressWarnings("unchecked")
            Collection<String> rawValue = (Collection<String>) CmdBuilderProcessor.toValue(
                    attrs.get(_attrName),
                    "java.util.Collection<java.lang.String>");
            if (!(rawValue instanceof Collection)) {
                throw new IllegalStateException("class type error.");
            }
            raw = new ArrayList<>(rawValue).get(Integer.parseInt(listAttrMatcher.group(1)));
        } else {
            raw = attrs.get(attrName);
        }

        return toValue(raw, conf.parameterizedType);
    }

    static <R> R toValue(Object raw, Class<R> clazz) throws ClassNotFoundException, ApiException {
        if (clazz == null) return null;
        return (R) toValue(raw, clazz.getName());
    }

    // TODO Values で処理できるようにする必要がある
    static Object toValue(Object raw, String parameterizedType) throws ClassNotFoundException, ApiException {
        if (raw == null) return null;

        String paramClassType = parameterizedType;
        if (paramClassType.contains("<")) {
            paramClassType = paramClassType.substring(0, paramClassType.indexOf("<"));
        }
        Class<?> type = Classes.forName(paramClassType);

        if (type.isInstance(raw)) {
            return raw;
        }

        if (EntityDto.class.isAssignableFrom(type)) {
            try {
                NaefDtoFacade dtoFacade = NaefRmiConnector.instance().dtoFacade();
                MVO.MvoId mvoId = dtoFacade.resolveMvoId((String) raw);
                EntityDto dto = DtoSpawner.spawn(mvoId);
                return dto;
            } catch (RemoteException e) {
                throw new InternalServerError("NAEF-00500", "NAEF通信エラー.", e);
            } catch (ResolveException e) {
                // mvo id 解決に失敗. 存在しないオブジェクトが指定された
                throw new BadRequest(e);
            }
        }

        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(Objects.toString(raw));
        }

        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(Objects.toString(raw));
        }

        if (type == long.class || type == Long.class) {
            return Long.parseLong(Objects.toString(raw));
        }

        if (type == double.class || type == Double.class) {
            return Double.parseDouble(Objects.toString(raw));
        }

        if (Collection.class.isAssignableFrom(type)) {
            String genericsType = parameterizedType.substring(paramClassType.length());
            Collection<?> values = (Collection<?>) raw;

            Collection<Object> result;
            if (List.class.isAssignableFrom(type)) {
                result = new ArrayList<>();
            } else if (Set.class.isAssignableFrom(type)) {
                result = new HashSet<>();
            } else {
                throw new IllegalStateException("unsupported type: " + type.getName());
            }

            for (Object value : values) {
                result.add(toValue(value, genericsType));
            }
            return result;
        }
        return raw;
    }

    /**
     * value が null でなければ consumer が実行される
     *
     * @param value
     * @param consumer
     */
    public static <T> void ifPresent(T value, Consumer<T> consumer) {
        Optional<T> opt = Optional.ofNullable(value);
        opt.ifPresent(consumer);
    }
}
