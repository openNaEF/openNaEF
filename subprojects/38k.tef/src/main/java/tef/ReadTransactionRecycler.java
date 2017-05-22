package tef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

class ReadTransactionRecycler {

    private static final long EXPIRE = 5 * 60 * 60 * 1000l;
    private static final long CHECK_INTERVAL = 3 * 60 * 1000l;

    private final TefService tefService_;
    private final Map<Thread, Transaction> transactions_ = new HashMap<Thread, Transaction>();
    private final Map<Thread, Long> lastAccess_ = new HashMap<Thread, Long>();

    ReadTransactionRecycler(TefService tefService) {
        tefService_ = tefService;
        new Thread() {

            @Override
            public void run() {
                while (true) {
                    synchronized (ReadTransactionRecycler.this) {
                        for (Thread thread : new HashSet<Thread>(transactions_.keySet())) {
                            if (thread.getState() == Thread.State.TERMINATED) {
                                disposeTransaction(thread, "t");
                            }

                            Long lastAccess = lastAccess_.get(thread);
                            if (lastAccess != null
                                    && System.currentTimeMillis() - lastAccess > EXPIRE) {
                                disposeTransaction(thread, "e");
                            }
                        }
                    }

                    try {
                        Thread.sleep(CHECK_INTERVAL);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }.start();
    }

    private synchronized void disposeTransaction(Thread thread, String cause) {
        Transaction tx = transactions_.get(thread);
        if (tx.isActive()) {
            TransactionContext.close(tx);
        }
        transactions_.remove(thread);
        lastAccess_.remove(thread);
        log("-" + cause, tx, thread);
    }

    synchronized TransactionId.R setupTransaction() {
        Thread thread = Thread.currentThread();

        Transaction tx = transactions_.get(thread);
        if (tx != null) {
            if (!tx.isActive()) {
                disposeTransaction(thread, "c");
            } else if (isOutOfDate(tx)) {
                disposeTransaction(thread, "o");
            }
        }

        long time = System.currentTimeMillis();
        tx = transactions_.get(thread);
        if (tx == null) {
            TransactionContext.beginReadTransaction();
            tx = Transaction.getContextTransaction();
            transactions_.put(thread, tx);
            log("+", tx, thread);
        } else {
            if (!tx.isReadOnly()) {
                throw new IllegalStateException(tx.getId().toString());
            }

            Transaction lasttx = Transaction.getLastCommittedTransaction();
            tx.setTargetVersion
                    (lasttx == null
                            ? TransactionId.PREHISTORIC_TRANSACTION_ID
                            : (TransactionId.W) lasttx.getId());
            tx.setTargetTime(time);
        }

        lastAccess_.put(thread, new Long(time));

        return (TransactionId.R) tx.getId();
    }

    private boolean isOutOfDate(Transaction tx) {
        return tx.getBaseTransaction() != Transaction.getLastCommittedTransaction();
    }

    private void log(String message, Transaction tx, Thread thread) {
        log("[" + tx.getId() + "," + thread.getId() + "," + thread.getName() + "]" + message);
    }

    private void log(String message) {
        tefService_.logMessage("[rtr]" + message);
    }

    synchronized boolean isRecycleTransaction(Transaction tx) {
        return transactions_.values().contains(tx);
    }

    void terminateRecycleReadTransaction() {
        Transaction contextTransaction = Transaction.getContextTransaction();
        if (contextTransaction != null && isRecycleTransaction(contextTransaction)) {
            TransactionContext.close();
        }
    }
}
