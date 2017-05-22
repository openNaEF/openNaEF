package opennaef.rest.api;

import opennaef.rest.api.config.CmdBuilderMapping;
import opennaef.rest.api.config.ConfigurationException;
import opennaef.rest.api.config.api.ApiConfig;
import opennaef.rest.api.helper.CmdBuilderProcessor;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.BadRequest;
import opennaef.rest.api.response.InternalServerError;
import opennaef.rest.api.response.NotFound;
import net.arnx.jsonic.JSON;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.*;

/**
 * opennaef.rest api の http request を処理する static な関数群
 */
public class Requests {
    /**
     * ApiContexts から API の設定を取り出す
     *
     * @param type uri path
     * @return API 設定
     * @throws NotFound            ApiContextsに設定が存在しなかった
     * @throws InternalServerError 設定ファイルの読み込みに失敗した
     */
    public static ApiConfig getContextConf(String type) throws NotFound, InternalServerError {
        ApiConfig content;
        try {
            content = CmdBuilderMapping.instance().content(type);
        } catch (ConfigurationException e) {
            throw new InternalServerError("API-01500", "設定ファイルエラー.", e);
        }
        if (content == null) {
            // targetType が設定にない -> 404
            throw new NotFound();
        }
        return content;
    }

    public static Map<String, Object> toReqAttrs(HttpServletRequest req, String type, String id) throws ApiException {
        final Map<String, Object> reqAttrs;
        if (req.getMethod().equals(HttpMethod.POST) || req.getMethod().equals(HttpMethod.PUT)) {
            try {
                reqAttrs = JSON.decode(req.getInputStream());
            } catch (IOException e) {
                throw new BadRequest("API-02402", "不正なフォーマットです.");
            }
        } else {
            reqAttrs = new LinkedHashMap<>();
        }

        // HEADER
        Collections.list(req.getHeaderNames())
                .forEach(name -> reqAttrs.put(CmdBuilderProcessor.PREFIX_HEADER + name, req.getHeader(name)));

        // QUERY
        Optional.ofNullable(req.getQueryString()).ifPresent(queries -> {
            Arrays.stream(queries.split("&")).forEach(q -> {
                String[] splited = q.split("=");
                if (splited.length >= 2) {
                    reqAttrs.put(CmdBuilderProcessor.PREFIX_URI_QUERY + splited[0], splited[1]);
                }
            });
        });

        // PATH
        if (type != null) reqAttrs.put(CmdBuilderProcessor.PREFIX_URI_PATH + "type", type);
        if (id != null) reqAttrs.put(CmdBuilderProcessor.PREFIX_URI_PATH + "id", id);

        return reqAttrs;
    }

    public static String[] toPathArgs(String uri, String contextPath) {
        String path = uri;
        path = path.replace(contextPath, "");
        path = path.startsWith("/") ? path.substring(1) : path;
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return path.split("/");
    }

    public static CRUD toCRUD(String method) {
        if ("GET".equalsIgnoreCase(method))  return CRUD.READ;
        if ("POST".equalsIgnoreCase(method))  return CRUD.CREATE;
        if ("PUT".equalsIgnoreCase(method))  return CRUD.UPDATE;
        if ("DELETE".equalsIgnoreCase(method))  return CRUD.DELETE;
        return null;
    }
}
