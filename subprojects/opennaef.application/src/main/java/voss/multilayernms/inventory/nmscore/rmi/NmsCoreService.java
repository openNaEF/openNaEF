package voss.multilayernms.inventory.nmscore.rmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class NmsCoreService extends UnicastRemoteObject {
    private static final Logger log = LoggerFactory.getLogger(NmsCoreService.class);

    public static final String SERVICE_NAME = "NmsCoreService";
    private static final long serialVersionUID = 1L;

    public NmsCoreService(int port) throws RemoteException {
        super(port);
    }
}