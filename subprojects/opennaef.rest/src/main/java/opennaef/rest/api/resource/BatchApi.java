package opennaef.rest.api.resource;

import opennaef.rest.SchedulableShellConnector;
import opennaef.rest.api.NaefRestApi;
import opennaef.rest.api.Requests;
import opennaef.rest.api.config.ConfigurationException;
import opennaef.rest.api.helper.CmdBuilderProcessor;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.BadRequest;
import opennaef.rest.api.response.InternalServerError;
import opennaef.rest.api.response.Responses;
import net.arnx.jsonic.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.DateTime;
import tef.skelton.dto.DtoChanges;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.exception.InventoryException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 複数のnaef apiを1txで実行する
 */
@Path("batch")
public class BatchApi {
    private static final Logger log = LoggerFactory.getLogger(BatchApi.class);

    @POST
    public static Response batch(
            @Context UriInfo uri,
            @Context HttpServletRequest request,
            @QueryParam("time") Date time,
            InputStream reqBody
    ) throws ApiException {
        log.debug("[batch] POST --- " + uri.getPath());

        final List<Query> queries;
        try {
            queries = Responses.json.parse(reqBody, new TypeReference<List<Query>>() {});
        } catch (IOException e) {
            throw new BadRequest("API-02402", "不正なフォーマットです.");
        }

        try {
            List<CommandBuilder> builders = new ArrayList<>();
            for (Query item : queries) {
                String[] targetType = Requests.toPathArgs(item.url, NaefRestApi.APPLICATION_PATH);
                List<CommandBuilder> builder = CmdBuilderProcessor.build(Requests.toCRUD(item.httpMethod), targetType[0], item.payload);
                builders.addAll(builder);
            }

            final Long timeMillis = time != null ? time.getTime() : System.currentTimeMillis();
            DtoChanges dtoChanges = SchedulableShellConnector.getInstance().executes(new DateTime(timeMillis), builders);

            Map<String, Object> res = new HashMap<>();
            Map<String, List<String>> locations = new HashMap<>();
            List<String> created = new ArrayList<>();
            List<String> updated = new ArrayList<>();
            res.put("locations", locations);
            locations.put("created", created);
            locations.put("updated", updated);

            dtoChanges.getNewObjects().forEach(item -> created.add(Responses.getAbsoluteLocation(request, item, timeMillis, null)));
            dtoChanges.getChangedObjects().forEach(item -> updated.add(Responses.getAbsoluteLocation(request, item, timeMillis, null)));

            return Response.ok(Responses.json.format(res), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (InventoryException e) {
            // コマンドの実行に失敗 Bad Request
            log.info("builderConfig execute", e);
            throw new BadRequest("NAEF-00400", "更新失敗. " + e.getCause().getMessage(), e);
        } catch (ConfigurationException e) {
            log.error("builderConfig execute", e);
            throw new InternalServerError("API-01500", "設定ファイルエラー.", e);
        }
    }

    private static class Query {
        public String url;
        public String httpMethod;
        public Map<String, Object> payload;
    }
}
