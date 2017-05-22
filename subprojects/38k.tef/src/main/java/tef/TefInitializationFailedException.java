package tef;

public class TefInitializationFailedException extends RuntimeException {

    public TefInitializationFailedException(String message) {
        super(message);
    }

    public TefInitializationFailedException(Throwable t) {
        super(t);
    }

    public TefInitializationFailedException() {
    }
}
