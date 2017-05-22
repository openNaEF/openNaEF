package opennaef.rest.api.response;

public class BadRequest extends ApiException {
    public static final String DEFAULT_ERROR_CODE = "HTTP-400";
    public static final String DEFAULT_MESSAGE = "Bad Request.";

    public BadRequest() {
        this(null);
    }

    public BadRequest(Throwable t) {
        this(DEFAULT_ERROR_CODE, DEFAULT_MESSAGE, t);
    }

    public BadRequest(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public BadRequest(String errorCode, String message, Throwable throwable) {
        super(400, errorCode, message, throwable);
    }
}
