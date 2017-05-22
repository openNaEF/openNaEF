package tef;

public class NoTransactionContextFoundException extends IllegalStateException {

    public NoTransactionContextFoundException() {
        super("no transaction-context.");
    }
}
