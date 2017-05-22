package voss.discovery.iolib.remote;

import voss.discovery.iolib.ProgressMonitor;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteMonitorImpl extends UnicastRemoteObject implements
        RemoteMonitor {
    private static final long serialVersionUID = 1L;
    private final ProgressMonitor localMonitor = new ProgressMonitor();

    protected RemoteMonitorImpl(int port) throws RemoteException {
        super(port);
    }

    @Override
    public void abort() throws RemoteException {
        this.localMonitor.abort();
    }

    @Override
    public boolean isAborted() throws RemoteException {
        return !this.localMonitor.isRunning();
    }

    @Override
    public boolean isRunning() throws RemoteException {
        return this.localMonitor.isRunning();
    }

    @Override
    public void start() throws RemoteException {
        this.localMonitor.start();
    }

}