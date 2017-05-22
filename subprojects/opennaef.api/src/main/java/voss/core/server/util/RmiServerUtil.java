package voss.core.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.common.service.VossRMIService;

import javax.swing.*;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public final class RmiServerUtil {
    private static final Logger log = LoggerFactory.getLogger(RmiServerUtil.class);
    private static final int RMI_CONNECT_INTERVAL = 5000;

    private RmiServerUtil() {

    }

    public static Remote connectService(final InetAddress addr, final int port,
                                        final String serviceName) throws AccessException {
        Remote remote = null;
        while (true) {
            try {
                log.debug("attempt to connect registry at " + addr.getHostAddress() + ":" + port);
                log.debug("serviceName=" + serviceName);
                remote = LocateRegistry.getRegistry(addr.getHostAddress(), port).lookup(serviceName);
                VossRMIService srv = (VossRMIService) remote;
                Proxy proxy = (Proxy) remote;
                log.debug("Connection Info: " + Proxy.getInvocationHandler(proxy));
                log.info("Connected to \"" + buildStartupMessage(srv));
                return remote;
            } catch (AccessException e) {
                throw e;
            } catch (NotBoundException ex) {
                log.info(serviceName + " not found in " + addr);
                log.debug(ex.getMessage(), ex);
            } catch (RemoteException e) {
                log.info(serviceName + " has exception in " + addr);
                log.debug(e.getMessage(), e);
                remote = null;
            }
            if (null != remote) {
                break;
            } else {
                try {
                    Thread.sleep(RMI_CONNECT_INTERVAL);
                } catch (InterruptedException ex) {
                    log.warn("interrupted.", ex);
                }
                log.info("retry connecting...");
            }
        }
        return remote;
    }

    public static InetAddress getLocalAddress() {
        String localIP = System.getProperty("java.rmi.server.hostname");
        InetAddress localAddress = null;
        try {
            if (localIP != null) {
                localAddress = InetAddress.getByName(localIP);
            } else {
                localAddress = InetAddress.getLocalHost();
            }
            return localAddress;
        } catch (UnknownHostException e) {
            log.error("illegal localhost: " + localIP, e);
            return null;
        }
    }

    public static String buildStartupMessage(final VossRMIService service)
            throws RemoteException {
        return service.getServiceName();

    }

    public static boolean isServiceBound(final InetAddress addr, final int port,
                                         final String serviceName) throws AccessException {
        try {
            VossRMIService intf = (VossRMIService) LocateRegistry
                    .getRegistry(addr.getHostAddress(), port).lookup(serviceName);
            intf.getServiceName();
            return true;
        } catch (NotBoundException e) {
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    public static void watchConnectService(final VossRMIService service) {
        watchConnectService(service, new Thread() {
            public void run() {
                System.err.println("Connection to the server has lost");
            }
        });
    }

    public static void watchConnectService(final VossRMIService service,
                                           final JFrame frame) {
        watchConnectService(service, new Thread() {
            public void run() {
                JOptionPane.showMessageDialog(frame,
                        "Connection to the server has lost");
                frame.setTitle(frame.getTitle() + " : not connected");
            }
        });
    }

    public static void watchConnectService(final VossRMIService service,
                                           final Thread thread) {
        (new Thread() {
            public void run() {
                while (true) {
                    try {
                        service.getServiceName();
                        Thread.sleep(RMI_CONNECT_INTERVAL);
                    } catch (RemoteException ex) {
                        log.error("Connection lost.");
                        thread.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        }).start();
    }
}