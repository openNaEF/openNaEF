package tef.skelton;

public class ValueException extends KnownRuntimeException {

    public ValueException(String message) {
        super(message);
    }

    public ValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
