package tef;

public interface TransactionCommitListener {

    public void notifyTransactionCommitted(CommittedTransaction committedTransaction);
}
