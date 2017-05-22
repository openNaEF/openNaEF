package tef;

abstract class TransactionCanNotProgressException extends RuntimeException {

    TransactionCanNotProgressException() {
        this("");
    }

    TransactionCanNotProgressException(String message) {
        super(message);

        TransactionContext.rollback();
    }
}
