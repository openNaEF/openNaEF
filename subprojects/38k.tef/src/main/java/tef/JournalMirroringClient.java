package tef;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JournalMirroringClient extends Remote {

    public void hello() throws RemoteException;

    public void negotiationFailed
            (JournalMirroringServer.NegotiationException error)
            throws RemoteException;

    public void transferJournal
            (JournalMirroringServer.TransferableJournalEntry container)
            throws RemoteException;
}
