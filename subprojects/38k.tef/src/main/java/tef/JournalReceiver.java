package tef;

import java.io.*;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class JournalReceiver extends Thread implements JournalMirroringClient {

    enum Status {

        LINK_DOWN, SYNCHRONIZING, MIRRORING, WAITING_TO_RECONNECT, ERROR
    }

    static final int DEFAULT_RECONNECTINTERVAL = 180000;

    private final TefService tefService_;
    private final CountDownLatch masterSynchronizationLatch_ = new CountDownLatch(1);
    private volatile boolean isMirroringEnabled_ = false;
    private volatile int reconnectInterval_ = DEFAULT_RECONNECTINTERVAL;
    private volatile Status status_ = Status.LINK_DOWN;
    private final BlockingQueue<JournalMirroringServer.TransferableJournalEntry> journals_
            = new LinkedBlockingQueue<JournalMirroringServer.TransferableJournalEntry>();
    private volatile JournalMirroringServer.NegotiationException negotiationError_;

    JournalReceiver(final TefService tefService) throws IOException {
        if (tefService.getRunningMode() != TefService.RunningMode.MIRROR) {
            throw new IllegalStateException
                    ("failed to initialize journal receiver: service mode is master.");
        }

        tefService_ = tefService;

        UnicastRemoteObject.exportObject(this);

        start();
    }

    public void hello() {
    }

    public void negotiationFailed(JournalMirroringServer.NegotiationException error) {
        negotiationError_ = error;
    }

    public void transferJournal(JournalMirroringServer.TransferableJournalEntry journal) {
        journals_.add(journal);
    }

    @Override
    public void run() {
        tefService_.setTransactionRestoringThread();

        try {
            while (true) {
                status_ = Status.LINK_DOWN;
                if (isMirroringEnabled_) {
                    JournalMirroringServer server = null;
                    try {
                        server = connectMasterServer();
                    } catch (java.rmi.ConnectException ce) {
                        logMessage("failed to connect: " + ce.getMessage());
                    } catch (RemoteException re) {
                        logMessage("failed to connect: " + re.getMessage());
                    } catch (NotBoundException nbe) {
                        logMessage("failed to connect: " + nbe.getMessage());
                    }

                    if (server != null) {
                        try {
                            processTransferedJournals(server);
                        } catch (RemoteException re) {
                            logMessage(re.getMessage());
                        } finally {
                            logMessage("connection closed.");
                        }
                    }
                }

                try {
                    status_ = Status.WAITING_TO_RECONNECT;
                    Thread.sleep(reconnectInterval_);
                } catch (InterruptedException ie) {
                }
            }
        } catch (JournalMirroringServer.NegotiationException ne) {
            status_ = Status.LINK_DOWN;
            logError("negotiation failed.", ne);
            tefService_.disableTefService();
        } catch (JournalRestorationException jre) {
            status_ = Status.ERROR;
            logError("journal restoration error.", jre);
            tefService_.disableTefService();
        } catch (Throwable t) {
            status_ = Status.LINK_DOWN;
            logError("mirroring aborted.", t);
        }
    }

    private JournalMirroringServer connectMasterServer()
            throws RemoteException, NotBoundException, MalformedURLException {
        String distributorUrl
                = tefService_.getTefServiceConfig().getMasterServerConfig().distributorUrl;
        logMessage("connecting master, " + distributorUrl);
        return (JournalMirroringServer) Naming.lookup(distributorUrl);
    }

    private void processTransferedJournals(JournalMirroringServer server)
            throws RemoteException,
            JournalMirroringServer.NegotiationException,
            JournalRestorationException {
        status_ = Status.SYNCHRONIZING;
        journals_.clear();

        TransactionDigest masterBase = server.getTransactionDigest();
        TransactionDigest localDigest = getLocalTransactionDigest();
        logMessage
                ("master base: " + TransactionId.getIdString(masterBase.transactionId)
                        + ", local base: " + TransactionId.getIdString(localDigest.transactionId));

        server.addDistributee(this, localDigest);

        JournalEntryRestorer restorer = new JournalEntryRestorer(tefService_, false);
        while (true) {
            if (negotiationError_ != null) {
                throw negotiationError_;
            }

            if (!isMirroringEnabled_) {
                return;
            }

            if (status_ != Status.MIRRORING
                    && TransactionId.equals(masterBase.transactionId, localDigest.transactionId)) {
                if (!Arrays.equals(masterBase.digest, localDigest.digest)) {
                    throw new JournalMirroringServer.NegotiationException
                            ("base transaction digest mismatch.");
                }

                masterSynchronizationLatch_.countDown();
                status_ = Status.MIRRORING;
                logMessage("synchronization completed.");
            }

            JournalMirroringServer.TransferableJournalEntry journal = null;
            try {
                journal = journals_.poll(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
            }
            if (journal == null) {
                server.hello();
                continue;
            }

            Transaction transaction = restorer.restore(journal);
            File receivedJournalFile = writeJournalFile(journal, transaction.getCommittedTime());
            Transaction.adjustTime(transaction.getCommittedTime(), "journal receiver");
            TransactionContext.commit();

            localDigest = getLocalTransactionDigest();
            if (!journal.digest.equals(localDigest)) {
                throw new JournalRestorationException
                        ("digest error: " + localDigest.transactionId.getIdString());
            }
        }
    }

    private File writeJournalFile
            (JournalMirroringServer.TransferableJournalEntry journal, long committedTime) {
        int transactionId = journal.getTransactionId();

        File journalFile
                = JournalFileUtils
                .getJournalFilePath
                        (transactionId, committedTime, JournalEntry.COMMITTED_JOURNAL_FILE_NAME_SUFFIX);
        if (journalFile.exists()) {
            throw new IllegalStateException
                    ("journal file already exists: " + journalFile.getAbsolutePath());
        }

        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(journalFile));
            out.write(journal.journalContents);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }

        return journalFile;
    }

    private TransactionDigest getLocalTransactionDigest() {
        return tefService_.getTransactionDigestComputer().getDigest();
    }

    synchronized boolean isMirroringEnabled() {
        return isMirroringEnabled_;
    }

    synchronized void setMirroringEnabled(boolean value) {
        isMirroringEnabled_ = value;
        interrupt();
        logMessage("mirroring " + (value ? "enabled" : "disabled") + ".");
    }

    synchronized void setReconnectInterval(int interval) {
        reconnectInterval_ = interval;
        interrupt();
        logMessage("reconnect interval: " + interval);
    }

    Status getStatus() {
        return status_;
    }

    void awaitUntilSynchronizeWithMaster() {
        try {
            masterSynchronizationLatch_.await();
        } catch (InterruptedException ie) {
        }
    }

    private void logMessage(String message) {
        tefService_.logMessage("[journal receiver] " + message);
    }

    private void logError(String message, Throwable t) {
        tefService_.logError("[journal receiver] " + message, t);
    }
}
