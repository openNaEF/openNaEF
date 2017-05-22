package voss.discovery.iolib.remote;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpClient;
import net.snmp.SnmpResponseException;
import net.snmp.VarBind;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.ProgressMonitor;
import voss.discovery.iolib.snmp.SerializableVarBind;
import voss.discovery.iolib.snmp.SerializableWalkProcessor;
import voss.discovery.iolib.snmp.SnmpAccess;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteSnmpAccessClient implements SnmpAccess {
    private final RemoteSnmpAccessSession session;

    public RemoteSnmpAccessClient(RemoteSnmpAccessSession session) {
        this.session = session;
    }

    @Override
    public void clearCache() {
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public VarBind get(String oid) throws SocketTimeoutException,
            SocketException, IOException, SnmpResponseException,
            AbortedException {
        try {
            SerializableVarBind vb = session.get(oid);
            if (vb != null) {
                return vb.getVarBind();
            }
            return null;
        } catch (RemoteException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public String getCommunityString() {
        try {
            return session.getCommunityString();
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public ProgressMonitor getMonitor() {
        return new ProgressMonitor();
    }

    @Override
    public VarBind getNextChild(String oid) throws AbortedException,
            SocketTimeoutException, SocketException, IOException,
            SnmpResponseException {
        try {
            SerializableVarBind vb = session.getNextChild(oid);
            if (vb != null) {
                return vb.getVarBind();
            }
            return null;
        } catch (RemoteException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public InetSocketAddress getSnmpAgentAddress() {
        try {
            return session.getSnmpAgentAddress();
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public SnmpClient getSnmpClient() {
        return null;
    }

    @Override
    public Map<String, VarBind> multiGet(String[] oids)
            throws SocketTimeoutException, SocketException, IOException,
            SnmpResponseException, AbortedException {
        try {
            Map<String, SerializableVarBind> res = session.multiGet(oids);
            Map<String, VarBind> result = new HashMap<String, VarBind>();
            for (Map.Entry<String, SerializableVarBind> entry : res.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getVarBind());
            }
            return result;
        } catch (RemoteException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void setCommunityString(String community) {
        try {
            session.setCommunityString(community);
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void setMonitor(ProgressMonitor monitor) {
    }

    @Override
    public int getSnmpRetry() {
        try {
            return session.getSnmpRetry();
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public int getSnmpTimeoutSeconds() {
        try {
            return session.getSnmpTimeoutSeconds();
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void setSnmpRetry(int retry) {
        try {
            session.setSnmpRetry(retry);
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void setSnmpTimeoutSeconds(int sec) {
        try {
            session.setSnmpTimeoutSeconds(sec);
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void walk(String oid, SerializableWalkProcessor walkProcessor)
            throws AbortedException, SocketTimeoutException, SocketException,
            IOException, RepeatedOidException, SnmpResponseException {
        try {
            session.walk(oid, walkProcessor);
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public List<String> getCachedVarbinds() {
        try {
            return session.getCachedVarbinds();
        } catch (RemoteException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}