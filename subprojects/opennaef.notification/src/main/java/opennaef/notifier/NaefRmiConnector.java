package opennaef.notifier;

import naef.ui.NaefDtoFacade;
import naef.ui.NaefShellFacade;
import opennaef.notifier.config.NotifierConfig;
import opennaef.notifier.util.Logs;
import pasaran.naef.PasaranNaefService;
import voss.mplsnms.MplsnmsRmiServiceAccessPoint;

import java.rmi.Naming;
import java.rmi.RemoteException;

/**
 * NaEF との RMI 接続を管理する
 */
public final class NaefRmiConnector {
    private static final NaefRmiConnector _instance = new NaefRmiConnector();
    private MplsnmsRmiServiceAccessPoint _ap;

    private NaefRmiConnector() {
    }

    public static NaefRmiConnector instance() {
        return NaefRmiConnector._instance;
    }

    /**
     * NaEF へ RMI 接続を行う
     *
     * @return RMI AP
     */
    public MplsnmsRmiServiceAccessPoint connect() {
        if (_ap != null) {
            // TODO naefへの再接続/DtoChangeListenerの再登録
            return _ap;
        }

        try {
            Logs.naef.info("connecting...");
            MplsnmsRmiServiceAccessPoint integratedAp = PasaranNaefService.getRmiServiceAccessPoint();
            if (integratedAp != null) {
                // naef integrated mode
                Logs.naef.info("connection successful. [INTEGRATED] mode");
                _ap = integratedAp;
            } else {
                // naef separate mode
                NotifierConfig conf = NotifierConfig.instance();
                String rmi = "rmi://" + conf.naefAddr() + ":" + conf.naefPort() + "/" + conf.naefServiceName();
                MplsnmsRmiServiceAccessPoint separateAp = (MplsnmsRmiServiceAccessPoint) Naming.lookup(rmi);
                _ap = separateAp;
                Logs.naef.info("connection successful. [SEPARATE] mode");
            }
        } catch (Exception e) {
            Logs.naef.error("connection failed", e);
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
