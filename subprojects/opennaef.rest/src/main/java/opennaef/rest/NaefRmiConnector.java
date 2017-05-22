package opennaef.rest;

import naef.ui.NaefDtoFacade;
import naef.ui.NaefShellFacade;
import voss.mplsnms.MplsnmsNaefService;
import voss.mplsnms.MplsnmsRmiServiceAccessPoint;

import java.rmi.RemoteException;

public final class NaefRmiConnector {
    private static final NaefRmiConnector _instance = new NaefRmiConnector();
    private MplsnmsRmiServiceAccessPoint _ap;

    private NaefRmiConnector() {
    }

    public static NaefRmiConnector instance() {
        return NaefRmiConnector._instance;
    }

    public MplsnmsRmiServiceAccessPoint connect() {
        if (_ap != null) {
            // TODO naefへの再接続/DtoChangeListenerの再登録
            return _ap;
        }
        try {
            // naef sepalate mode
//            NaefApiConfig conf = NaefApiConfig.instance();
//            String rmi = "rmi://" + conf.naefAddr() + ":" + conf.naefPort() + "/" + conf.naefServiceName();
//            _ap = (MplsnmsRmiServiceAccessPoint) Naming.lookup(rmi);

            // naef integrated mode
            _ap = MplsnmsNaefService.getRmiServiceAccessPoint();

        } catch (Exception e) {
            throw new IllegalStateException("naef 接続に失敗.");
        }
        return _ap;
    }

    public NaefDtoFacade dtoFacade() throws RemoteException {
        connect();
        return _ap.getServiceFacade().getDtoFacade();
    }

    public NaefShellFacade shellFacade() throws RemoteException {
        connect();
        return _ap.getServiceFacade().getShellFacade();
    }
}
