package opennaef.notifier.webhook.provider;

import net.arnx.jsonic.JSONException;
import opennaef.notifier.util.Responses;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * JSON exception mapper
 */
@Provider
public class JSONExceptionMapper implements ExceptionMapper<JSONException> {
    @Override
    public Response toResponse(JSONException exception) {
        return Responses.badRequest();
    }
}