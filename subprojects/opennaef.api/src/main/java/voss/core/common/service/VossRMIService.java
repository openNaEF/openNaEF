package voss.core.common.service;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface VossRMIService extends Remote {

    String getServiceName() throws RemoteException;

    InetAddress getServiceAddress() throws RemoteException;

    int getServicePort() throws RemoteException;
}