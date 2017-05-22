package voss.discovery.iolib.remote;

import voss.discovery.iolib.VossEvent;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteSnmpServiceListener extends Remote {
    String getName() throws RemoteException;

    void update(VossEvent event, Object updated) throws RemoteException;
}