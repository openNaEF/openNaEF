package voss.core.common.exception;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ExceptionServiceInterface extends Remote {
    void saveException(final ExceptionHolder ex) throws RemoteException;
}