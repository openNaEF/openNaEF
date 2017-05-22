package opennaef.rest.api.resource;

import opennaef.rest.api.response.Responses;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * opennaef.rest api から発生する予期しない Exception からレスポンスを生成するマッパー
 */
@Provider
public class UncheckedExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable t) {
        return Responses.toResponse(t);
    }
}
