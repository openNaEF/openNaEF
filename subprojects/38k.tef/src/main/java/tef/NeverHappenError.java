package tef;

public class NeverHappenError extends Error {

    NeverHappenError() {
    }

    NeverHappenError(String message) {
        super(message);
    }

    NeverHappenError(Throwable cause) {
        super(cause);
    }

    NeverHappenError(String message, Throwable cause) {
        super(message, cause);
    }
}
