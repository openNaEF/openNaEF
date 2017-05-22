package voss.discovery.iolib.remote;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpClient;
import net.snmp.SnmpResponseException;
import net.snmp.VarBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SerializableVarBind;
import voss.discovery.iolib.snmp.SerializableWalkProcessor;
import voss.discovery.iolib.snmp.SnmpAccessImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteSnmpAccessSessionImpl extends UnicastRemoteObject implements
        RemoteSnmpAccessSession {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(RemoteSnmpAccessSessionImpl.class);
    @SuppressWarnings("unused")
    private final RemoteSnmpAccessServer server;
    private final SnmpAccessImpl localAccess;
    private final RemoteMonitor monitor;

    private boolean running = true;

    protected RemoteSnmpAccessSessionImpl(int port, RemoteSnmpAccessServer server,
                                          SnmpAccessImpl access) throws RemoteException {
        super(port);
        this.server = server;
        this.localAccess = access;
        this.monitor = new RemoteMonitorImpl(port);
    }

    @Override
    public void close() {
        this.running = false;
    }

    private void check() throws RemoteException {
        if (!this.running) {
            throw new RemoteException("session closed.");
        }
    }

    @Override
    public SerializableVarBind get(String oid) throws RemoteException {
        check();
        try {
            VarBind varbind = localAccess.get(oid);
            SerializableVarBind result = new SerializableVarBind(varbind);
            return result;
        } catch (AbortedException e) {
            throw new RemoteException("aborted.", e);
        } catch (SocketTimeoutException e) {
            throw new RemoteException("timeout.", e);
        } catch (SocketException e) {
            throw new RemoteException("socket problem.", e);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage(), e);
        } catch (SnmpResponseException e) {
            throw new RemoteException("illegal snmp response.", e);
        }
    }

    @Override
    public Map<String, SerializableVarBind> multiGet(String[] oids) throws RemoteException {
        try {
            Map<String, VarBind> response = this.localAccess.multiGet(oids);
            Map<String, SerializableVarBind> result = new HashMap<String, SerializableVarBind>();
            for (Map.Entry<String, VarBind> entry : response.entrySet()) {
                SerializableVarBind vb = new SerializableVarBind(entry.getValue());
                result.put(entry.getKey(), vb);
            }
            return result;
        } catch (AbortedException e) {
            throw new RemoteException("aborted.", e);
        } catch (SocketTimeoutException e) {
            throw new RemoteException("timeout.", e);
        } catch (SocketException e) {
            throw new RemoteException("socket problem.", e);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage(), e);
        } catch (SnmpResponseException e) {
            throw new RemoteException("illegal snmp response.", e);
        }
    }

    @Override
    public SerializableVarBind getNextChild(String oid) throws RemoteException {
        try {
            VarBind varbind = this.localAccess.getNextChild(oid);
            SerializableVarBind result = new SerializableVarBind(varbind);
            return result;
        } catch (AbortedException e) {
            throw new RemoteException("aborted.", e);
        } catch (SocketTimeoutException e) {
            throw new RemoteException("timeout.", e);
        } catch (SocketException e) {
            throw new RemoteException("socket problem.", e);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage(), e);
        } catch (SnmpResponseException e) {
            throw new RemoteException("illegal snmp response.", e);
        }
    }

    @Override
    public void walk(String oid, SerializableWalkProcessor walkProcessor)
            throws RemoteException {
        try {
            this.localAccess.walk(oid, walkProcessor);
        } catch (AbortedException e) {
            throw new RemoteException("aborted.", e);
        } catch (SocketTimeoutException e) {
            throw new RemoteException("timeout.", e);
        } catch (SocketException e) {
            throw new RemoteException("socket problem.", e);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage(), e);
        } catch (SnmpResponseException e) {
            throw new RemoteException("illegal snmp response.", e);
        } catch (RepeatedOidException e) {
            throw new RemoteException("repeated oid.", e);
        }
    }

    @Override
    public String getCommunityString() throws RemoteException {
        return this.localAccess.getCommunityString();
    }

    @Override
    public RemoteMonitor getMonitor() throws RemoteException {
        check();
        return this.monitor;
    }

    @Override
    public InetSocketAddress getSnmpAgentAddress() throws RemoteException {
        return this.localAccess.getSnmpAgentAddress();
    }

    public SnmpClient getSnmpClient() throws RemoteException {
        return this.localAccess.getSnmpClient();
    }

    @Override
    public int getSnmpRetry() throws RemoteException {
        return this.localAccess.getSnmpRetry();
    }

    @Override
    public int getSnmpTimeoutSeconds() throws RemoteException {
        return this.localAccess.getSnmpTimeoutSeconds();
    }

    @Override
    public void setCommunityString(String community) throws RemoteException {
        this.localAccess.setCommunityString(community);
    }

    @Override
    public void setSnmpRetry(int retry) throws RemoteException {
        this.localAccess.setSnmpRetry(retry);
    }

    @Override
    public void setSnmpTimeoutSeconds(int sec) throws RemoteException {
        this.localAccess.setSnmpTimeoutSeconds(sec);
    }

    @Override
    public List<String> getCachedVarbinds() {
        return this.localAccess.getCachedVarbinds();
    }
}