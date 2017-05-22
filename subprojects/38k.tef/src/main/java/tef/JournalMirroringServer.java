package tef;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JournalMirroringServer extends Remote {

    public static class NegotiationException extends Exception {

        public NegotiationException(String message) {
            super(message);
        }

        public NegotiationException(Throwable cause) {
            super(cause);
        }
    }

    public static class TransferableJournalEntry extends JournalEntry implements Serializable {

        public final TransactionDigest digest;
        public final byte[] journalContents;

        TransferableJournalEntry(TransactionDigest digest, byte[] journalContents) {
            this.digest = digest;
            this.journalContents = journalContents;
        }

        @Override
        protected String getJournalName() {
            return Integer.toString(digest.transactionId.serial, 16)
                    + COMMITTED_JOURNAL_FILE_NAME_SUFFIX;
        }

        @Override
        protected InputStream getInputStream() {
            return new ByteArrayInputStream(journalContents);
        }

        @Override
        protected long getSize() {
            return journalContents.length;
        }
    }

    public static final String RMI_SERVICE_NAME = "tef.journal-distribution-service";

    public void hello() throws RemoteException;

    public TransactionDigest getTransactionDigest() throws RemoteException;

    public void addDistributee
            (JournalMirroringClient distributee, TransactionDigest synchronizationBase)
            throws RemoteException, NegotiationException;
}
