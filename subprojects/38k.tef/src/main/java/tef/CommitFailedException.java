package tef;

public class CommitFailedException extends RuntimeException {

    CommitFailedException(String message) {
        super(message);
    }

    CommitFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
