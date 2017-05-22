package voss.core.server.exception;

@SuppressWarnings("serial")
public class ExternalServiceException extends Exception {

    public ExternalServiceException() {
        super();
    }

    public ExternalServiceException(String msg) {
        super(msg);
    }

    public ExternalServiceException(Throwable cause) {
        super(cause);
    }

    public ExternalServiceException(String msg, Throwable cause) {
        super(msg, cause);
    }

}