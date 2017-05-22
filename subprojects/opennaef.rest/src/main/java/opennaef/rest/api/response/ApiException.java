package opennaef.rest.api.response;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * opennaef.rest api が発生させる Exception
 */
public class ApiException extends Exception {
    private final int _httpResponseCode;
    private final String _errorCode;
    private final String _message;

    public ApiException(int httpResponseCode, String errorCode, String message) {
        this(httpResponseCode, errorCode, message, null);
    }

    public ApiException(int httpResponseCode, String errorCode, String message, Throwable e) {
        super(e);
        _httpResponseCode = httpResponseCode;
        _errorCode = errorCode;
        _message = message;
    }

    public int httpResponseCode() {
        return _httpResponseCode;
    }

    public String errorCode() {
        return _errorCode;
    }

    public String message() {
        return _message;
    }

    public String stackTraceString() {
        Throwable cause = getCause();
        if (cause == null) return null;

        StringWriter errors = new StringWriter();
        cause.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }
}
