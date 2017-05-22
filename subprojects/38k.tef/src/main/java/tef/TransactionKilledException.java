package tef;

public class TransactionKilledException extends TransactionCanNotProgressException {

    TransactionKilledException() {
    }

    TransactionKilledException(String message) {
        super(message);
    }
}
