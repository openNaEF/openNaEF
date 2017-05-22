package opennaef.rest.api.response;

public class NotFound extends ApiException {
    public static final String DEFAULT_ERROR_CODE = "HTTP-404";
    public static final String DEFAULT_MESSAGE = "Not Found.";

    public NotFound() {
        this(null);
    }

    public NotFound(Throwable e) {
        super(404, DEFAULT_ERROR_CODE, DEFAULT_MESSAGE, e);
    }
}
