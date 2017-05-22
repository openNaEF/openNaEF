package tef;

public class ConcurrentTransactionException extends TransactionCanNotProgressException {

    ConcurrentTransactionException() {
    }

    ConcurrentTransactionException(String message) {
        super(message);
    }
}
