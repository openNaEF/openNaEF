package opennaef.notifier.webhook.provider;

import opennaef.notifier.util.Responses;
import opennaef.notifier.webhook.PingFailed;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Not found exception mapper
 */

@Provider
public class PingFailedMapper implements ExceptionMapper<PingFailed> {
    @Override
    public Response toResponse(PingFailed exception) {
        return Responses.badRequest("ping failed");
    }
}