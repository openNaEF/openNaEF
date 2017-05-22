package voss.discovery.iolib.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.RealDeviceAccessFactory;
import voss.discovery.iolib.snmp.SnmpAccessImpl;
import voss.model.NodeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

@SuppressWarnings("serial")
public class RemoteSnmpAccessServer extends UnicastRemoteObject implements RemoteSnmpAccessService {
    private static final Logger log = LoggerFactory.getLogger(RemoteSnmpAccessServer.class);
    private final int port;
    private final Set<RemoteSnmpServiceListener> listeners =
            Collections.synchronizedSet(new HashSet<RemoteSnmpServiceListener>());
    private final RealDeviceAccessFactory factory = new RealDeviceAccessFactory();

    public RemoteSnmpAccessServer(int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
        this.port = port;
    }

    public RemoteSnmpAccessServer(int port) throws RemoteException {
        this(port, null, null);
    }

    @Override
    public synchronized RemoteSnmpAccessSession getSession(NodeInfo node)
            throws RemoteException {
        return getSession(node, node.listIpAddress());
    }

    @Override
    public synchronized RemoteSnmpAccessSession getSession(NodeInfo node,
                                                           List<InetAddress> specifiedAddress) throws RemoteException {
        String client = getClient();
        try {
            DeviceAccess access = factory.getDeviceAccess(node, specifiedAddress);
            if (access == null) {
                throw new RemoteException("cannot get deviceAccess.");
            }
            if (!(access.getSnmpAccess() instanceof SnmpAccessImpl)) {
                throw new RemoteException("bug! " +
                        "RealDeviceAccessFactory.getDeviceAccess() " +
                        "not returned SnmpAccessImpl.");
            }
            RemoteSnmpAccessSessionImpl session =
                    new RemoteSnmpAccessSessionImpl(port, this, (SnmpAccessImpl) access.getSnmpAccess());
            log.info("session returned to " + client);
            return session;
        } catch (IOException e) {
            throw new RemoteException("faied to connect.", e);
        } catch (AbortedException e) {
            throw new RemoteException("aborted.", e);
        }
    }

    @Override
    public synchronized RemoteSnmpAccessSession getSession(NodeInfo node, InetAddress specifiedAddress)
            throws RemoteException {
        List<InetAddress> list = new ArrayList<InetAddress>();
        return getSession(node, list);
    }

    private String getClient() {
        try {
            return super.getClientHost();
        } catch (ServerNotActiveException e) {
            return "[" + e.getMessage() + "]";
        }
    }

    @Override
    public synchronized void addListener(RemoteSnmpServiceListener listener) throws RemoteException {
        this.listeners.add(listener);
    }

    @Override
    public synchronized void removeListener(RemoteSnmpServiceListener listener) throws RemoteException {
        this.listeners.remove(listener);
    }

    public static void main(String[] args) {
        System.setSecurityManager(new RMISecurityManager());
        try {
            if (args.length == 0) {
                System.err.println("Usage: " + RemoteSnmpAccessServer.class.getName()
                        + " <port> [default-timeout:30] [default-retry:3]");
                System.exit(1);
            }
            int port = Integer.parseInt(args[0]);
            LocateRegistry.createRegistry(port);
            Registry registry = null;
            try {
                registry = LocateRegistry.getRegistry(port);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (registry == null) {
            }
            RemoteSnmpAccessService server = new RemoteSnmpAccessServer(port);
            log.debug(registry.list().toString());
            registry.bind(SERVICE_NAME, server);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}