package tef;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class JournalDistributor implements JournalMirroringServer {

    static class ClientAgent extends Thread {

        private static int maxSequence__ = 0;

        private final int sequence_;
        private final JournalDistributor distributor_;
        private final JournalMirroringClient distributee_;
        private final TransactionDigest synchronizationBase_;
        private final BlockingQueue<TransferableJournalEntry> queue_
                = new LinkedBlockingQueue<TransferableJournalEntry>();

        ClientAgent
                (final JournalDistributor distributor,
                 final JournalMirroringClient distributee,
                 final TransactionDigest synchronizationBase) {
            synchronized (ClientAgent.class) {
                sequence_ = maxSequence__++;
            }

            distributor_ = distributor;
            distributee_ = distributee;
            synchronizationBase_ = synchronizationBase;

            distributor_.addClientAgent(this);

            start();
        }

        @Override
        public void run() {
            NegotiationException initializationError;
            try {
                sendCommittedTransactions();
                initializationError = null;
            } catch (NegotiationException ne) {
                initializationError = ne;
            } catch (Throwable t) {
                initializationError = new NegotiationException(t);
            }
            if (initializationError != null) {
                negotiationFailed(initializationError);
                return;
            }

            logMessage("mirroring start.");
            try {
                while (true) {
                    TransferableJournalEntry journal = null;
                    try {
                        journal = queue_.poll(3000, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ie) {
                    }

                    if (journal == null) {
                        distributee_.hello();
                    } else {
                        distributee_.transferJournal(journal);
                    }
                }
            } catch (RemoteException re) {
                logMessage("connection error.");
            } catch (Throwable t) {
                logError("", t);
            } finally {
                distributor_.removeClientAgent(this);
            }
        }

        private void sendCommittedTransactions() throws IOException, NegotiationException {
            TransactionId.W clientBaseTransactionId = synchronizationBase_.transactionId;

            if (clientBaseTransactionId != null
                    && Transaction.getTransaction(clientBaseTransactionId) == null) {
                throw new NegotiationException
                        ("no base transaction: " + clientBaseTransactionId.getIdString());
            }

            logMessage
                    ("mirroring base: "
                            + (clientBaseTransactionId == null
                            ? "null"
                            : clientBaseTransactionId.getIdString()));

            TransactionDigestComputer transactionDigestComputer = new TransactionDigestComputer();

            TransactionId.W lastCommittedTransactionId
                    = TransactionContext.getLastCommittedTransactionId();
            if (lastCommittedTransactionId == null) {
                return;
            }

            int maxTransactionId = lastCommittedTransactionId.serial;

            TransactionId.W lastTransferedTransaction = null;
            FullJournalEntries entries = new FullJournalEntries(distributor_.getTefService());
            for (JournalEntry journal : entries.getJournalEntryIterable()) {
                if (journal.getTransactionId() > maxTransactionId) {
                    break;
                }

                byte[] journalContents = journal.getContents();
                TransactionId.W transactionId = new TransactionId.W(journal.getTransactionId());
                transactionDigestComputer.update(transactionId, journalContents);

                if (clientBaseTransactionId != null
                        && journal.getTransactionId() <= clientBaseTransactionId.serial) {
                    if (journal.getTransactionId() == clientBaseTransactionId.serial) {
                        if (!Arrays.equals
                                (transactionDigestComputer.getDigest().digest,
                                        synchronizationBase_.digest)) {
                            throw new NegotiationException("base transaction digest mismatch.");
                        }
                    }

                    continue;
                }

                distributee_.transferJournal
                        (new TransferableJournalEntry
                                (transactionDigestComputer.getDigest(), journalContents));
                lastTransferedTransaction = transactionId;
            }
            entries.close();

            logMessage
                    ("last transaction: "
                            + (lastTransferedTransaction == null
                            ? "-"
                            : lastTransferedTransaction.getIdString()));
        }

        private void negotiationFailed(NegotiationException initializationError) {
            logError("distributor initialization failed.", initializationError);
            distributor_.removeClientAgent(this);
            try {
                distributee_.negotiationFailed(initializationError);
            } catch (RemoteException re) {
                logError("", re);
            }
        }

        void addJournal(TransferableJournalEntry journal) {
            queue_.add(journal);
        }

        String getIdentifierString() {
            return Integer.toString(sequence_, 16);
        }

        private void logMessage(String message) {
            distributor_.logMessage("[" + getIdentifierString() + "]" + message);
        }

        private void logError(String message, Throwable t) {
            distributor_.logError("[" + getIdentifierString() + "]" + message, t);
        }
    }

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final byte[] LINE_SEPARATOR_BYTES = LINE_SEPARATOR.getBytes();

    private final TefService tefService_;
    private final Collection<InetAddress> authorizedClientAddresses_;
    private final List<ClientAgent> clientAgents_ = new ArrayList<ClientAgent>();

    JournalDistributor
            (final TefService tefService,
             final Collection<InetAddress> authorizedClientAddresses)
            throws IOException, java.rmi.AlreadyBoundException {
        tefService_ = tefService;
        authorizedClientAddresses_ = authorizedClientAddresses;

        UnicastRemoteObject.exportObject(this);
        tefService_.getRmiRegistry().bind(JournalMirroringServer.RMI_SERVICE_NAME, this);
    }

    TefService getTefService() {
        return tefService_;
    }

    public void hello() {
    }

    public TransactionDigest getTransactionDigest() {
        return tefService_.getTransactionDigestComputer().getDigest();
    }

    public void addDistributee
            (JournalMirroringClient distributee, TransactionDigest synchronizationBase)
            throws NegotiationException {
        try {
            String distributeeHost = RemoteServer.getClientHost();
            if (!authenticate(InetAddress.getByName(distributeeHost))) {
                logMessage("unauthorized access, " + distributeeHost);
                throw new NegotiationException("authentication failed.");
            }

            ClientAgent agent
                    = new ClientAgent(JournalDistributor.this, distributee, synchronizationBase);
            agent.logMessage(distributeeHost);
        } catch (NegotiationException ne) {
            throw ne;
        } catch (Exception e) {
            tefService_.logError("", e);
            throw new NegotiationException(e);
        }
    }

    private boolean authenticate(InetAddress address) {
        return authorizedClientAddresses_.contains(address);
    }

    private void addClientAgent(ClientAgent agent) {
        synchronized (clientAgents_) {
            clientAgents_.add(agent);
        }

        logMessage("new receiver: " + agent.getIdentifierString());
    }

    private void removeClientAgent(ClientAgent agent) {
        synchronized (clientAgents_) {
            clientAgents_.remove(agent);
        }

        logMessage("receiver removed: " + agent.getIdentifierString());
    }

    void distributeJournal(TransferableJournalEntry journal) {
        synchronized (clientAgents_) {
            for (ClientAgent client : clientAgents_) {
                client.addJournal(journal);
            }
        }
    }

    private void logMessage(String message) {
        tefService_.logMessage("[journal distributor]" + message);
    }

    private void logError(String message, Throwable t) {
        tefService_.logError("[journal distributor]" + message, t);
    }
}
