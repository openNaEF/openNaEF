package pasaran;

import naef.ui.NaefRmiServiceAccessPoint;
import naef.ui.NaefRmiServiceFacade;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


public final class NaefConnector {
    private static final NaefConnector instance_ = new NaefConnector();
    private NaefRmiServiceAccessPoint _ap;

    private NaefConnector() {
    }

    public static NaefConnector getInstance() {
        return NaefConnector.instance_;
    }

    public NaefRmiServiceFacade getConnection() throws RemoteException {
        if(_ap != null) {
            return _ap.getServiceFacade();
        }
        try {
            String naefRmiPort = System.getProperty("naef-rmi-port", "38100");
            String naefServerAddr = "rmi://localhost:" + naefRmiPort + "/mplsnms";

            _ap = (NaefRmiServiceAccessPoint) Naming.lookup(naefServerAddr);
        } catch (NotBoundException | MalformedURLException e) {
            e.printStackTrace();
        }
        return _ap.getServiceFacade();
    }
}
