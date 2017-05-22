package tef;

public interface WriteTransaction<T> {

    public interface Resultless {

        public void execute() throws TransactionAbortedException;
    }

    public T execute() throws TransactionAbortedException;
}
