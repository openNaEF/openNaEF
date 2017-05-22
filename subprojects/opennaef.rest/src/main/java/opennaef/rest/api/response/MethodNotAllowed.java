package opennaef.rest.api.response;

public class MethodNotAllowed extends ApiException {
    public static final String DEFAULT_ERROR_CODE = "HTTP-405";
    public static final String DEFAULT_MESSAGE = "Method Not Allowed.";

    public MethodNotAllowed() {
        this(null);
    }

    public MethodNotAllowed(Throwable e) {
        super(405, DEFAULT_ERROR_CODE, DEFAULT_MESSAGE, e);
    }
}
