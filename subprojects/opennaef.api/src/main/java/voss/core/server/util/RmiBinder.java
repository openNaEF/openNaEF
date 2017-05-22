package voss.core.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.CountDownLatch;

public class RmiBinder extends Thread {
    private static final long INTERVAL = 10 * 1000;
    private final Logger log = LoggerFactory.getLogger(RmiBinder.class);
    private final String name;
    private final int registryPort;
    private final Remote remote;
    private CountDownLatch readyToGo = new CountDownLatch(1);

    public RmiBinder(final int port, final String serviceName, final Remote remoteObj) throws RemoteException {
        super.setDaemon(true);
        this.registryPort = port;
        this.name = serviceName;
        this.remote = remoteObj;
        try {
            log.debug("bind target=127.0.0.1:" + port + ", name=" + name);
            rebind();
            log.info("Bound: " + this.name);
            readyToGo.countDown();
        } catch (RemoteException ex) {
            log.error("Failed to Registry.rebind()", ex);
            System.exit(-1);
        }
    }

    private void rebind() throws RemoteException {
        Registry registry = getRegistry();
        registry.rebind(this.name, this.remote);
    }

    private boolean find() throws RemoteException {
        String[] names = getRegistry().list();
        for (String str : names) {
            if (str.equals(this.name)) {
                return true;
            }
        }
        return false;
    }

    private Registry getRegistry() throws RemoteException {
        Registry registry = LocateRegistry.getRegistry(this.registryPort);
        try {
            registry.list();
        } catch (RemoteException e) {
            registry = null;
        }
        if (registry == null) {
            registry = LocateRegistry.createRegistry(this.registryPort);
        }
        return registry;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException ex) {
                log.error("Interrupted", ex);
            }
            try {
                if (!find()) {
                    try {
                        rebind();
                        log.info("Rebind success: " + this.name);
                        readyToGo.countDown();
                    } catch (RemoteException ex) {
                        log.error("Failed to Registry.revind()");
                    }
                }
            } catch (RemoteException ex) {
                log.error("Failed to Registry.list()");
            }
        }
    }

    public void startAndWait() throws InterruptedException {
        this.start();
        readyToGo.await();
    }
}