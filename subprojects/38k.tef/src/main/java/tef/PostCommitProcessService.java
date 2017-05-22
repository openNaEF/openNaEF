package tef;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

class PostCommitProcessService extends Thread {

    private static final String LOGGER_ID = "[pcps]";

    static class CommittedTransactionContainer {

        private final CountDownLatch transactionClosedLatch_ = new CountDownLatch(1);

        final JournalMirroringServer.TransferableJournalEntry journal;
        final CommittedTransaction committedTransaction;
        final List<PostTransactionProcessor> postProcessors;

        CommittedTransactionContainer
                (JournalMirroringServer.TransferableJournalEntry journal,
                 CommittedTransaction committedTransaction,
                 List<PostTransactionProcessor> postProcessors) {
            this.journal = journal;
            this.committedTransaction = committedTransaction;
            this.postProcessors = postProcessors;
        }

        void transactionClosed() {
            transactionClosedLatch_.countDown();
        }

        void awaitForTransactionClosed() {
            try {
                transactionClosedLatch_.await();
            } catch (InterruptedException ie) {
            }
        }
    }

    private static class TransactionCommitNotifier {

        private final Thread notifierThread_;
        private final String name_;
        private final TransactionCommitListener listener_;
        private volatile boolean isToTerminate_ = false;
        private final BlockingQueue<CommittedTransaction> committedTransactions_
                = new LinkedBlockingQueue<CommittedTransaction>();

        TransactionCommitNotifier(String name, TransactionCommitListener listener) {
            if (name == null) {
                throw new IllegalArgumentException();
            }

            name_ = name;
            listener_ = listener;
            notifierThread_ = new Thread() {

                @Override
                public void run() {
                    while (!isToTerminate_ || committedTransactions_.size() > 0) {
                        final CommittedTransaction committedTransaction;
                        try {
                            committedTransaction = committedTransactions_.take();
                        } catch (InterruptedException ie) {
                            continue;
                        }

                        listener_.notifyTransactionCommitted(committedTransaction);
                    }
                }
            };
            notifierThread_.start();
        }

        String getName() {
            return name_;
        }

        TransactionCommitListener getListener() {
            return listener_;
        }

        void addCommittedTransaction(CommittedTransaction committedTransaction) {
            try {
                committedTransactions_.put(committedTransaction);
            } catch (InterruptedException ie) {
                TefService.instance().logError("", ie);
                throw new RuntimeException(ie);
            }
        }

        void requestTerminate() {
            isToTerminate_ = true;
            notifierThread_.interrupt();
        }
    }

    private final TefService tefService_;
    private final List<TransactionCommitNotifier> commitNotifiers_
            = new ArrayList<TransactionCommitNotifier>();
    private final BlockingQueue<CommittedTransactionContainer> committedTransactions_
            = new LinkedBlockingQueue<CommittedTransactionContainer>();

    PostCommitProcessService(TefService tefService) {
        tefService_ = tefService;

        start();
    }

    @Override
    public void run() {
        while (true) {
            final CommittedTransactionContainer container;
            try {
                container = committedTransactions_.take();
            } catch (InterruptedException ie) {
                continue;
            }

            container.awaitForTransactionClosed();

            {
                if (tefService_.getJournalDistributor() != null) {
                    tefService_.getJournalDistributor().distributeJournal(container.journal);
                }
            }

            synchronized (commitNotifiers_) {
                for (TransactionCommitNotifier notifier : commitNotifiers_) {
                    notifier.addCommittedTransaction(container.committedTransaction);
                }
            }

            new Thread() {

                @Override
                public void run() {
                    final String transactionIdStr
                            = container.committedTransaction.getTransactionId().getIdString();
                    for (PostTransactionProcessor postProcessor : container.postProcessors) {
                        String idStr = transactionIdStr + ":" + postProcessor.getName();
                        try {
                            tefService_.logMessage(">" + idStr);
                            postProcessor.process();
                            tefService_.logMessage("<" + idStr);
                        } catch (Throwable t) {
                            tefService_.logError(idStr, t);
                        }
                    }
                }
            }.start();
        }
    }

    TransactionCommitNotifier getTransactionCommitNotifierByName(String name) {
        synchronized (commitNotifiers_) {
            for (TransactionCommitNotifier notifier : commitNotifiers_) {
                if (notifier.getName().equals(name)) {
                    return notifier;
                }
            }
            return null;
        }
    }

    void addTransactionCommitListener(String name, TransactionCommitListener listener) {
        if (name == null) {
            throw new IllegalArgumentException
                    ("name is null, transaction commit listener name is mandatory.");
        }

        synchronized (commitNotifiers_) {
            if (getTransactionCommitNotifierByName(name) != null) {
                throw new IllegalArgumentException
                        ("duplicated transaction commit listener name: " + name);
            }
            commitNotifiers_.add(new TransactionCommitNotifier(name, listener));

            TefService.instance().logMessage(LOGGER_ID + "add commit listener, name:" + name);
        }
    }

    void removeTransactionCommitListener(TransactionCommitListener listener) {
        synchronized (commitNotifiers_) {
            for (TransactionCommitNotifier notifier
                    : new ArrayList<TransactionCommitNotifier>(commitNotifiers_)) {
                if (notifier.getListener() == listener) {
                    notifier.requestTerminate();
                    commitNotifiers_.remove(notifier);
                    TefService.instance().logMessage
                            (LOGGER_ID + "remove commit listener, name:" + notifier.getName());
                }
            }
        }
    }

    void removeTransactionCommitListener(String name) {
        synchronized (commitNotifiers_) {
            TransactionCommitNotifier notifier = getTransactionCommitNotifierByName(name);
            if (notifier != null) {
                removeTransactionCommitListener(notifier.getListener());
            }
        }
    }

    CommittedTransactionContainer registCommittedTransaction
            (JournalMirroringServer.TransferableJournalEntry journal,
             CommittedTransaction committedTransaction,
             List<PostTransactionProcessor> postProcessors) {
        CommittedTransactionContainer container;
        try {
            container = new CommittedTransactionContainer
                    (journal, committedTransaction, postProcessors);
            committedTransactions_.put(container);
        } catch (InterruptedException ie) {
            tefService_.logError("", ie);
            throw new RuntimeException(ie);
        }

        return container;
    }
}
