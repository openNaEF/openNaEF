package voss.discovery.iolib.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteMonitor extends Remote {
    void start() throws RemoteException;

    void abort() throws RemoteException;

    boolean isAborted() throws RemoteException;

    boolean isRunning() throws RemoteException;
}