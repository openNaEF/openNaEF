package opennaef.notifier.util;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Response Utility
 */
public class Responses {
    public static Response ok(Object entity) {
        String json;
        if (entity instanceof String) {
            json = (String) entity;
        } else {
            json = JsonUtils.toJson(entity);
        }
        return Response
                .ok(json)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    public static Response created(URI path, Object entity) {
        return Response
                .created(path)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(JsonUtils.toJson(entity))
                .build();
    }

    public static Response noContent() {
        return Response
                .noContent()
                .build();
    }

    public static Response notFound() {
        return Response
                .status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    public static Response badRequest() {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }

    public static Response badRequest(String message) {
        Map<String, String> res = new HashMap<>();
        res.put("message", message);
        return Response
                .status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(JsonUtils.toJson(res))
                .build();
    }

    public static Response internalServerError() {
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
