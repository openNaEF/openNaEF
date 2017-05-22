package voss.core.server.exception;

@SuppressWarnings("serial")
public class AAAException extends Exception {

    public AAAException() {
        super();
    }

    public AAAException(String msg) {
        super(msg);
    }

    public AAAException(String msg, Throwable cause) {
        super(msg, cause);
    }
}