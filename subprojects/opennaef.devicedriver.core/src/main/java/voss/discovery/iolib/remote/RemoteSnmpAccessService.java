package voss.discovery.iolib.remote;

import voss.model.NodeInfo;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteSnmpAccessService extends Remote {
    public static final String SERVICE_NAME = "RemoteSnmpAccess";
    public static final int DEFAULT_PORT = 44444;

    RemoteSnmpAccessSession getSession(NodeInfo node) throws RemoteException;

    RemoteSnmpAccessSession getSession(NodeInfo node, List<InetAddress> specifiedAddress) throws RemoteException;

    RemoteSnmpAccessSession getSession(NodeInfo node, InetAddress specifiedAddress) throws RemoteException;

    void addListener(RemoteSnmpServiceListener listener) throws RemoteException;

    void removeListener(RemoteSnmpServiceListener listener) throws RemoteException;
}