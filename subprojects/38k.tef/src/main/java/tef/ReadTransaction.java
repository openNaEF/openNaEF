package tef;

public interface ReadTransaction<T> {

    public interface Resultless {

        public void execute() throws TransactionAbortedException;
    }

    public T execute() throws TransactionAbortedException;
}
