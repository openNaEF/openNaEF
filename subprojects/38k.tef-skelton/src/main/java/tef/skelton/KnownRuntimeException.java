package tef.skelton;

public class KnownRuntimeException extends RuntimeException {

    public KnownRuntimeException() {
    }

    public KnownRuntimeException(Throwable cause) {
        super(cause);
    }

    public KnownRuntimeException(String message) {
        super(message);
    }

    public KnownRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
