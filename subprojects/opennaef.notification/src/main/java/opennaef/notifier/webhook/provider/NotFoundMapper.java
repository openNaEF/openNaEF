package opennaef.notifier.webhook.provider;

import opennaef.notifier.util.NotFound;
import opennaef.notifier.util.Responses;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Not found exception mapper
 */

@Provider
public class NotFoundMapper implements ExceptionMapper<NotFound> {
    @Override
    public Response toResponse(NotFound exception) {
        return Responses.notFound();
    }
}