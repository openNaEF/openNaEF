package tef.skelton.dto;

import tef.TransactionId;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface DtoChangeListener extends Remote {

    public void transactionCommitted(
        TransactionId transactionId, Set<EntityDto> newObjects, Set<EntityDto> changedObjects, DtoChanges changes)
        throws RemoteException;
}
