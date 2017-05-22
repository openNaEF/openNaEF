package voss.nms.inventory.diff;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DiffSetManager extends Remote {
    List<String> list() throws RemoteException;

    void createNewDiff(DiffCategory category) throws RemoteException;

    void abort(DiffCategory category) throws RemoteException;

    DiffSet getDiffSet(DiffCategory category) throws RemoteException;

    void apply(DiffUnit unit) throws RemoteException;

    void discard(DiffUnit unit) throws RemoteException;

    boolean isRunning(DiffCategory category) throws RemoteException;
}