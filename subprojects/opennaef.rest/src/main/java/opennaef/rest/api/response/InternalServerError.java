package opennaef.rest.api.response;

public class InternalServerError extends ApiException {
    public static final String DEFAULT_ERROR_CODE = "HTTP-500";
    public static final String DEFAULT_MESSAGE = "Internal Server Error.";

    public InternalServerError(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public InternalServerError(String errorCode, String message, Throwable throwable) {
        super(500, errorCode, message, throwable);
    }
}
