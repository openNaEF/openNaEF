package tef;

import lib38k.logger.Logger;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public interface TransactionManager extends Remote, DistributedTransactionComponent {

    public void begin() throws RemoteException;

    public void commit(StackTraceCatalog.CatalogId stacktraceCatalogId)
            throws CommitFailedException, RemoteException;

    public void rollback(StackTraceCatalog.CatalogId stacktraceCatalogId)
            throws RemoteException;

    static interface Standalone extends TransactionManager {

        static final class Impl implements Standalone, RunsAtLocalSide {

            private Transaction transaction_;

            Impl(Transaction transaction) {
                transaction_ = transaction;
            }

            @Override
            public void begin() {
                Facade.bind(transaction_, this);
                transaction_.begin();
            }

            @Override
            public void commit(StackTraceCatalog.CatalogId stacktraceCatalogId)
                    throws CommitFailedException {
                transaction_.resume();

                try {
                    transaction_.phase1Commit();
                } catch (CommitFailedException te) {
                    rollback(stacktraceCatalogId);
                    throw te;
                } catch (RuntimeException re) {
                    TefService.instance().logError("", re);
                    rollback(stacktraceCatalogId);
                    throw re;
                } catch (Error e) {
                    TefService.instance().logError("", e);
                    rollback(stacktraceCatalogId);
                    throw e;
                }

                transaction_.phase2Commit(stacktraceCatalogId);
            }

            @Override
            public void rollback(StackTraceCatalog.CatalogId stacktraceCatalogId) {
                transaction_.resume();

                transaction_.rollback(stacktraceCatalogId);
            }
        }
    }

    static interface Distributed extends TransactionManager {

        public GlobalTransactionId getGlobalTransactionId() throws RemoteException;

        static final class Impl
                extends UnicastRemoteObject
                implements Distributed, RunsAtCoordinatorSide {
            private static final class ExecutionMutex {

                private TransactionManager.Distributed currentLock_ = null;

                synchronized final void lock(final TransactionManager.Distributed lock) {
                    while (currentLock_ != null) {
                        try {
                            wait();
                        } catch (InterruptedException ie) {
                            throw new RuntimeException(ie);
                        }
                    }

                    currentLock_ = lock;
                }

                synchronized final void unlock(final TransactionManager.Distributed lock) {
                    if (currentLock_ != lock) {
                        throw new IllegalArgumentException();
                    }

                    currentLock_ = null;
                    notifyAll();
                }
            }

            private static final String LOG_MESSAGE_TYPE_REGULAR = "+";
            private static final String LOG_MESSAGE_TYPE_ERROR = "-";

            private static ExecutionMutex executionMutex__ = new ExecutionMutex();

            private DistributedTransactionService.Impl service_;
            private Logger logger_;
            private GlobalTransactionId globalTransactionId_;
            private StackTraceCatalog.CatalogId stacktraceCatalogId_;
            private LocalTransactionProxy[] participants_;
            private Map<LocalTransactionProxy, String> participantsNames_
                    = new HashMap<LocalTransactionProxy, String>();
            private Map<LocalTransactionProxy, TransactionId.W> participantsTxIds_
                    = new HashMap<LocalTransactionProxy, TransactionId.W>();

            Impl(DistributedTransactionService.Impl service,
                 Logger logger,
                 LocalTransactionProxy[] participants,
                 StackTraceCatalog.CatalogId stacktraceCatalogId)
                    throws RemoteException {
                executionMutex__.lock(this);

                service_ = service;
                logger_ = logger;
                participants_ = participants;
                stacktraceCatalogId_ = stacktraceCatalogId;
                globalTransactionId_ = new GlobalTransactionId();

                log(new String[]{
                        "*",
                        stacktraceCatalogId_.getGlobalId(),
                        Integer.toString(participants_.length)
                });
            }

            @Override
            public GlobalTransactionId getGlobalTransactionId() {
                return globalTransactionId_;
            }

            private String getTefServiceId(LocalTransactionProxy tefService) {
                return participantsNames_.get(tefService);
            }

            private TransactionId.W getLocalTxId(LocalTransactionProxy tefService) {
                return participantsTxIds_.get(tefService);
            }

            @Override
            public void begin() throws RemoteException {
                for (LocalTransactionProxy participant : participants_) {
                    participant.setDistributedTransaction(this);
                }

                for (LocalTransactionProxy participant : participants_) {
                    try {
                        participant.beginTransaction(stacktraceCatalogId_);
                        participantsNames_.put(participant, participant.getTefServiceId());
                        participantsTxIds_.put(participant, participant.getLocalTransactionId());

                        log(new String[]{
                                getTefServiceId(participant),
                                getLocalTxId(participant).getIdString()
                        });
                    } catch (RemoteException re) {
                        err(
                                new String[]{
                                        "local transaction starting failed.",
                                        getTefServiceId(participant)},
                                re);
                    }
                }
            }

            @Override
            public void commit(StackTraceCatalog.CatalogId stacktraceCatalogId) {
                try {
                    log(new String[]{"c", stacktraceCatalogId.getGlobalId()});
                    for (int i = 0; i < participants_.length; i++) {
                        LocalTransactionProxy participant = participants_[i];
                        try {
                            participant.phase1Commit();
                            log(new String[]{"c1:" + i, getTefServiceId(participant)});
                        } catch (Throwable t) {
                            err(new String[]{"c1:" + i, getTefServiceId(participant)}, t);
                            rollbackImpl(stacktraceCatalogId);
                            throw new CommitFailedException(getTefServiceId(participant), t);
                        }
                    }

                    for (int i = 0; i < participants_.length; i++) {
                        LocalTransactionProxy participant = participants_[i];
                        try {
                            participant.phase2Commit();
                            log(new String[]{"c2:" + i, getTefServiceId(participant)});
                        } catch (Throwable t) {
                            err(new String[]{"c2:" + i, getTefServiceId(participant)}, t);
                            continue;
                        }
                    }
                    log(new String[]{"."});

                    Map<String, TransactionId.W> txIds = new HashMap<String, TransactionId.W>();
                    for (LocalTransactionProxy proxy : participantsTxIds_.keySet()) {
                        String serviceId = participantsNames_.get(proxy);
                        txIds.put(serviceId, participantsTxIds_.get(proxy));
                    }
                    service_.addHistoryEntry
                            (service_.newHistoryEntry(getGlobalTransactionId(), txIds));
                } finally {
                    cleanup();
                }
            }

            @Override
            public void rollback(StackTraceCatalog.CatalogId stacktraceCatalogId) {
                try {
                    rollbackImpl(stacktraceCatalogId);
                } finally {
                    cleanup();
                }
            }

            private void rollbackImpl(StackTraceCatalog.CatalogId stacktraceCatalogId) {
                log(new String[]{"r", stacktraceCatalogId.getGlobalId()});
                for (int i = 0; i < participants_.length; i++) {
                    LocalTransactionProxy participant = participants_[i];
                    try {
                        participant.rollback();
                        log(new String[]{"r:" + i, getTefServiceId(participant)});
                    } catch (Throwable t) {
                        err(new String[]{"r:" + i, getTefServiceId(participant)}, t);
                        continue;
                    }
                }
                log(new String[]{"."});
            }

            private void cleanup() {
                executionMutex__.unlock(this);

                logger_ = null;
                participants_ = null;
                participantsNames_ = null;
            }

            void log(String[] message) {
                log(LOG_MESSAGE_TYPE_REGULAR, message);
            }

            void err(String[] message, Throwable t) {
                log(LOG_MESSAGE_TYPE_ERROR, message);
                logger_.printStackTrace(t);
            }

            private void log(String messageType, String[] message) {
                String timeStr = Long.toString(System.currentTimeMillis());

                logger_.print
                        (getGlobalTransactionId().getIdString()
                                + "\t" + timeStr
                                + "\t" + messageType);
                for (int i = 0; i < message.length; i++) {
                    logger_.print("\t" + message[i]);
                }
                logger_.println();
            }
        }
    }

    static final class Facade {

        private static final Map<Transaction, TransactionManager> txs__
                = new HashMap<Transaction, TransactionManager>();

        static {
            Transaction.addTransactionCloseListener
                    (new Transaction.TransactionCloseListener() {

                        @Override
                        public void notifyClosed(Transaction transaction) {
                            synchronized (txs__) {
                                txs__.remove(transaction);
                            }
                        }
                    });
        }

        private Facade() {
        }

        static final void bind(Transaction transaction, TransactionManager tx) {
            synchronized (txs__) {
                txs__.put(transaction, tx);
            }
        }

        static final Transaction getContextTransaction()
                throws NoTransactionContextFoundException {
            Transaction result = Transaction.getContextTransaction();
            if (result == null) {
                throw new NoTransactionContextFoundException();
            }
            return result;
        }

        private static TransactionManager getTransactionManager(Transaction transaction) {
            synchronized (txs__) {
                return txs__.get(transaction);
            }
        }

        static final Transaction beginStandaloneTransaction
                (boolean isReadOnly,
                 boolean isConcurrentWrite,
                 StackTraceCatalog.CatalogId stacktraceCatalogId) {
            try {
                Transaction transaction
                        = new Transaction
                        (Transaction.TRANSACTION_TYPE_STANDALONE,
                                isReadOnly,
                                isConcurrentWrite,
                                stacktraceCatalogId);
                TransactionManager.Standalone standaloneTx
                        = new TransactionManager.Standalone.Impl(transaction);
                standaloneTx.begin();

                return transaction;
            } catch (RemoteException re) {
                NeverHappenError neverHappen = new NeverHappenError(re);
                TefService.instance().logError("", neverHappen);
                throw neverHappen;
            } catch (RuntimeException re) {
                TefService.instance().logError("", re);
                throw re;
            } catch (Error e) {
                TefService.instance().logError("", e);
                throw e;
            }
        }

        static final Transaction restoreTransaction
                (int transactionId, long beginTime, long committedTime) {
            try {
                Transaction transaction
                        = new Transaction(transactionId, beginTime, committedTime);
                TransactionManager.Standalone standaloneTx
                        = new TransactionManager.Standalone.Impl(transaction);
                standaloneTx.begin();
                return transaction;
            } catch (RemoteException re) {
                NeverHappenError neverHappen = new NeverHappenError(re);
                TefService.instance().logError("", neverHappen);
                throw neverHappen;
            }
        }

        static final GlobalTransactionId beginDistributedTransaction
                (StackTraceCatalog.CatalogId stacktraceCatalogId) {
            TransactionManager.Distributed distributedTx = null;
            try {
                distributedTx
                        = distributedTransactionService__
                        .createDistributedTransaction(stacktraceCatalogId);
                distributedTx.begin();
                return distributedTx.getGlobalTransactionId();
            } catch (RemoteException re) {
                throw new RuntimeException
                        ("connecting to distributed transaction service failed.", re);
            } catch (RuntimeException re) {
                TefService.instance().logError("", re);
                if (distributedTx != null) {
                    try {
                        distributedTx.rollback(stacktraceCatalogId);
                    } catch (RemoteException remoteExp) {
                        TefService.instance().logError("", remoteExp);
                    }
                }
                throw re;
            } catch (Error e) {
                TefService.instance().logError("", e);
                if (distributedTx != null) {
                    try {
                        distributedTx.rollback(stacktraceCatalogId);
                    } catch (RemoteException re) {
                        TefService.instance().logError("", re);
                    }
                }
                throw e;
            }
        }

        private static Transaction getLocalTransaction(TransactionManager tx) {
            synchronized (txs__) {
                for (Transaction transaction : txs__.keySet()) {
                    TransactionManager mappedTx = txs__.get(transaction);
                    if (tx.equals(mappedTx)) {
                        return transaction;
                    }
                }
                return null;
            }
        }

        static Transaction getLocalTransaction(GlobalTransactionId globalTransactionId) {
            synchronized (txs__) {
                for (Transaction transaction : txs__.keySet()) {
                    TransactionManager mappedTx = txs__.get(transaction);
                    if (!(mappedTx instanceof TransactionManager.Distributed)) {
                        continue;
                    }
                    GlobalTransactionId mappedTxId;
                    try {
                        mappedTxId = ((TransactionManager.Distributed) mappedTx)
                                .getGlobalTransactionId();
                    } catch (RemoteException re) {
                        throw new RuntimeException(re);
                    }
                    if (globalTransactionId.equals(mappedTxId)) {
                        return transaction;
                    }
                }
                return null;
            }
        }

        static GlobalTransactionId getGlobalTransactionId(Transaction transaction) {
            TransactionManager tx = txs__.get(transaction);
            if (!(tx instanceof TransactionManager.Distributed)) {
                return null;
            } else {
                try {
                    return ((TransactionManager.Distributed) tx).getGlobalTransactionId();
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
        }

        static void commit(StackTraceCatalog.CatalogId stacktraceCatalogId)
                throws CommitFailedException {
            TransactionManager tx = getTransactionManager(getContextTransaction());
            synchronized (tx) {
                getLocalTransaction(tx).suspend();
                try {
                    tx.commit(stacktraceCatalogId);
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
        }

        static void rollback(StackTraceCatalog.CatalogId stacktraceCatalogId) {
            rollback
                    (getTransactionManager(getContextTransaction()),
                            stacktraceCatalogId);
        }

        private static void rollback
                (TransactionManager tx, StackTraceCatalog.CatalogId stacktraceCatalogId) {
            synchronized (tx) {
                getLocalTransaction(tx).suspend();
                try {
                    tx.rollback(stacktraceCatalogId);
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
        }

        static void close
                (Transaction transaction, StackTraceCatalog.CatalogId stacktraceCatalogId) {
            TransactionManager tx = getTransactionManager(transaction);
            if (tx == null) {
                throw new IllegalStateException();
            }

            synchronized (tx) {
                if (!transaction.hasCommitted()) {
                    TransactionManager.Facade.rollback(tx, stacktraceCatalogId);
                }
            }
        }

        private static DistributedTransactionService distributedTransactionService__;

        static void initializeLocalSide(String transactionCoordinatorServiceUrl)
                throws RemoteException, NotBoundException, MalformedURLException {
            distributedTransactionService__
                    = (DistributedTransactionService) Naming
                    .lookup(transactionCoordinatorServiceUrl);

            TefServiceProxy tefServiceProxy = new TefServiceProxy.Impl();
            UnicastRemoteObject.exportObject(tefServiceProxy);
            distributedTransactionService__.enlist(tefServiceProxy);
        }

        static synchronized boolean isDistributedTransactionServiceAvailable() {
            return distributedTransactionService__ != null;
        }

        static GlobalTransactionId getGlobalTxId(TransactionId.W localTxId, boolean isStrict) {
            try {
                return distributedTransactionService__.getGlobalTxId
                        (TefService.instance().getServiceName(), localTxId, isStrict);
            } catch (RemoteException re) {
                TefService.instance().logError("", re);
                throw new RuntimeException(re);
            }
        }

        static TransactionId.W getLocalTxId(GlobalTransactionId globalTxId) {
            try {
                return distributedTransactionService__
                        .getLocalTxId(TefService.instance().getServiceName(), globalTxId);
            } catch (RemoteException re) {
                TefService.instance().logError("", re);
                throw new RuntimeException(re);
            }
        }
    }
}
