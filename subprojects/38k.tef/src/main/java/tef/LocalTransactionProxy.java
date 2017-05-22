package tef;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

interface LocalTransactionProxy extends Remote, DistributedTransactionComponent {

    public void setDistributedTransaction
            (TransactionManager.Distributed distributedTransaction)
            throws RemoteException;

    public String getTefServiceId() throws RemoteException;

    public TransactionId.W getLocalTransactionId() throws RemoteException;

    public void beginTransaction(StackTraceCatalog.CatalogId stacktraceCatalogId)
            throws RemoteException;

    public void phase1Commit() throws RemoteException;

    public void phase2Commit() throws RemoteException;

    public void rollback() throws RemoteException;

    static class Impl
            extends UnicastRemoteObject
            implements LocalTransactionProxy, RunsAtLocalSide {
        private TransactionManager.Distributed distributedTransaction_;
        private Transaction transaction_;

        Impl() throws RemoteException {
        }

        public void setDistributedTransaction
                (TransactionManager.Distributed distributedTransaction) {
            distributedTransaction_ = distributedTransaction;
        }

        public String getTefServiceId() {
            return TefService.instance().getServiceName();
        }

        public TransactionId.W getLocalTransactionId() {
            return (TransactionId.W) transaction_.getId();
        }

        public void beginTransaction(StackTraceCatalog.CatalogId stacktraceCatalogId) {
            TefService.instance().getReadTransactionRecycler().terminateRecycleReadTransaction();

            transaction_
                    = new Transaction
                    (Transaction.TRANSACTION_TYPE_DISTRIBUTED, false, false, stacktraceCatalogId);
            TransactionManager.Facade.bind(transaction_, distributedTransaction_);
            transaction_.begin();
            transaction_.suspend();
        }

        public void phase1Commit() {
            transaction_.resume();

            try {
                transaction_.phase1Commit();
            } finally {
                transaction_.suspend();
            }
        }

        public void phase2Commit() {
            transaction_.resume();

            transaction_.phase2Commit(null);
        }

        public void rollback() {
            transaction_.resume();

            transaction_.rollback(null);
        }
    }
}
