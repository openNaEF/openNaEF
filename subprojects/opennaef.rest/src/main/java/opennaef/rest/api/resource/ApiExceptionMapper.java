package opennaef.rest.api.resource;

import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.Responses;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * opennaef.rest api から発生する ApiException からレスポンスを生成するマッパー
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {
    @Override
    public Response toResponse(ApiException exception) {
        exception.printStackTrace();
        return Responses.toResponse(exception);
    }
}
