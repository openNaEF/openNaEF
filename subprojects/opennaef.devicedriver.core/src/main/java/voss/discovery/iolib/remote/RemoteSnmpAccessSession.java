package voss.discovery.iolib.remote;

import voss.discovery.iolib.snmp.SerializableVarBind;
import voss.discovery.iolib.snmp.SerializableWalkProcessor;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;


public interface RemoteSnmpAccessSession extends Remote {

    SerializableVarBind get(String oid) throws RemoteException;

    String getCommunityString() throws RemoteException;

    RemoteMonitor getMonitor() throws RemoteException;

    SerializableVarBind getNextChild(String oid) throws RemoteException;

    InetSocketAddress getSnmpAgentAddress() throws RemoteException;

    Map<String, SerializableVarBind> multiGet(String[] oids) throws RemoteException;

    void setCommunityString(String community) throws RemoteException;

    int getSnmpRetry() throws RemoteException;

    int getSnmpTimeoutSeconds() throws RemoteException;

    void setSnmpRetry(int retry) throws RemoteException;

    void setSnmpTimeoutSeconds(int sec) throws RemoteException;

    void walk(String oid, SerializableWalkProcessor walkProcessor) throws RemoteException;

    List<String> getCachedVarbinds() throws RemoteException;

    void close() throws RemoteException;
}