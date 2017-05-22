package opennaef.rest.api.response;


/**
 * エラーレスポンスPOJO
 */
public class ErrorResponse {
    /**
     * HTTP Response Code
     */
    private int _http_response_code;

    /**
     * Error Code
     */
    private String _code;

    /**
     * Error Message
     */
    private String _message;

    /**
     * Debug
     */
    private String _debugMessage;

    public int getHttpResponseCode() {
        return _http_response_code;
    }

    public void setHttpResponseCode(int status) {
        this._http_response_code = status;
    }

    public String getCode() {
        return _code;
    }

    public void setCode(String code) {
        this._code = code;
    }

    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        this._message = message;
    }

    public String getDebugMessage() {
        return _debugMessage;
    }

    public void setDebugMessage(String debugMessage) {
        this._debugMessage = debugMessage;
    }
}
