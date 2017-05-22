package opennaef.rest.api.resource;

import opennaef.rest.api.config.CmdBuilderMapping;
import opennaef.rest.api.config.AttrNameMapping;
import opennaef.rest.api.config.ConfigurationException;
import opennaef.rest.api.config.NaefApiConfig;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.InternalServerError;
import opennaef.rest.api.response.Responses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * naef api config api
 */
@Path("/configs")
public class ConfigsApi {
    private static final Logger log = LoggerFactory.getLogger(ConfigsApi.class);

    @GET
    @Path("/reload")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response reload() throws ApiException {
        log.info("config reload");
        // TODO 管理者権限でしか実行できてはいけない

        try {
            // naef config
            NaefApiConfig.instance().reload();

            // configurable-mvo-api config
            CmdBuilderMapping.instance().reload();

            // attribute-name-mapping config
            AttrNameMapping.instance().reload();
        } catch (ConfigurationException e) {
            throw new InternalServerError("API-01500", "設定ファイルエラー. " + e.getMessage(), e);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("state", "success");
        result.put("configs", new String[] {NaefApiConfig.CONFIG_FILE, CmdBuilderMapping.CONFIG_FILE, AttrNameMapping.CONFIG_FILE});
        return Response.ok(Responses.json.format(result), MediaType.APPLICATION_JSON_TYPE).build();
    }
}
