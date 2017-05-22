package tef;

public class TransactionAbortedException extends RuntimeException {

    public TransactionAbortedException() {
    }

    public TransactionAbortedException(String message) {
        super(message);
    }

    public TransactionAbortedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionAbortedException(Throwable cause) {
        super(cause);
    }
}
