package tef.skelton;

public class AuthenticationException extends KnownRuntimeException {

    public AuthenticationException() {
    }

    public AuthenticationException(Throwable t) {
        super(t);
    }

    public AuthenticationException(String message) {
        super(message);
    }
}
