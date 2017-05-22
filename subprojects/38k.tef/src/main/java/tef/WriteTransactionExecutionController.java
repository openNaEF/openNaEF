package tef;

import java.util.*;
import java.util.List;

final class WriteTransactionExecutionController {

    private static final class ExclusiveWriteTransactionLock {

        private Transaction executingExclusiveWriteTransaction_;

        synchronized void executionFinished(Transaction finishedTransaction) {
            if (!finishedTransaction.isExclusiveWrite()) {
                throw new IllegalArgumentException();
            }
            if (executingExclusiveWriteTransaction_ != finishedTransaction) {
                throw new IllegalStateException();
            }

            executingExclusiveWriteTransaction_ = null;

            notify();
        }

        synchronized void waitfor() {
            if (executingExclusiveWriteTransaction_ != null) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
        }

        synchronized void executionBegun(Transaction transaction) {
            if (!transaction.isExclusiveWrite()) {
                throw new IllegalArgumentException();
            }
            if (executingExclusiveWriteTransaction_ != null) {
                throw new IllegalStateException();
            }

            executingExclusiveWriteTransaction_ = transaction;
        }
    }

    private static final class WaitingQueue {

        private final List<Transaction> queue_ = new ArrayList<Transaction>();

        void enqueue(Transaction writeTransaction) {
            synchronized (queue_) {
                queue_.add(writeTransaction);
                queue_.notify();
            }
        }

        Transaction dequeue() {
            synchronized (queue_) {
                if (queue_.size() == 0) {
                    try {
                        queue_.wait();
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }

                Transaction nextWriteTransaction = queue_.remove(0);
                return nextWriteTransaction;
            }
        }
    }

    private static WriteTransactionExecutionController instance__
            = new WriteTransactionExecutionController();

    private final Set<Transaction> executingTransactions_ = new HashSet<Transaction>();

    private final WaitingQueue waitingQueue_ = new WaitingQueue();
    private final ExclusiveWriteTransactionLock exclusiveWriteTransactionLock_
            = new ExclusiveWriteTransactionLock();

    private final Map<Transaction, Mutex> transactionExecutionMutexes_
            = new HashMap<Transaction, Mutex>();

    private Transaction lastTransaction_;

    private WriteTransactionExecutionController() {
        synchronized (WriteTransactionExecutionController.class) {
            if (instance__ != null) {
                throw new IllegalStateException();
            } else {
                instance__ = this;
            }
        }

        new Thread() {
            public void run() {
                try {
                    while (true) {
                        exclusiveWriteTransactionLock_.waitfor();

                        Transaction nextWriteTransaction = waitingQueue_.dequeue();

                        if (lastTransaction_ != null
                                && (lastTransaction_.getId().serial >=
                                nextWriteTransaction.getId().serial)) {
                            throw new IllegalStateException();
                        }
                        lastTransaction_ = nextWriteTransaction;

                        if (nextWriteTransaction.isExclusiveWrite()) {
                            synchronized (executingTransactions_) {
                                while (executingTransactions_.size() > 0) {
                                    try {
                                        executingTransactions_.wait();
                                    } catch (InterruptedException ie) {
                                        throw new RuntimeException(ie);
                                    }
                                }
                            }

                            exclusiveWriteTransactionLock_
                                    .executionBegun(nextWriteTransaction);
                        }

                        synchronized (executingTransactions_) {
                            executingTransactions_.add(nextWriteTransaction);
                        }

                        getExecutionMutex(nextWriteTransaction).unlock();
                    }
                } catch (RuntimeException re) {
                    TefService.instance().logError("", re);
                    throw re;
                } catch (Error e) {
                    TefService.instance().logError("", e);
                    throw e;
                }
            }
        }.start();
    }

    final void enqueue(Transaction writeTransaction) {
        synchronized (transactionExecutionMutexes_) {
            transactionExecutionMutexes_.put(writeTransaction, new Mutex());
        }

        waitingQueue_.enqueue(writeTransaction);
    }

    final void transactionFinished(Transaction writeTransaction) {
        synchronized (transactionExecutionMutexes_) {
            transactionExecutionMutexes_.remove(writeTransaction);
        }

        synchronized (executingTransactions_) {
            executingTransactions_.remove(writeTransaction);
            executingTransactions_.notify();
        }

        if (writeTransaction.isExclusiveWrite()) {
            exclusiveWriteTransactionLock_.executionFinished(writeTransaction);
        }
    }

    static WriteTransactionExecutionController getInstance() {
        return instance__;
    }

    final Mutex getExecutionMutex(Transaction transaction) {
        synchronized (transactionExecutionMutexes_) {
            return transactionExecutionMutexes_.get(transaction);
        }
    }
}
