package voss.nms.inventory.database;

import naef.ui.NaefDtoFacade;
import naef.ui.NaefShellFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.database.NaefBridge;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.mplsnms.MplsnmsNaefService;
import voss.mplsnms.MplsnmsRmiServiceAccessPoint;
import voss.nms.inventory.config.InventoryConfiguration;
import voss.nms.inventory.trigger.TriggerManager;
import voss.nms.inventory.trigger.TriggerService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class DefaultNaefBridge implements NaefBridge {
    private MplsnmsRmiServiceAccessPoint ap = null;
    private final Logger log;

    public DefaultNaefBridge() {
        this.log = LoggerFactory.getLogger(DefaultNaefBridge.class);
    }

    private MplsnmsRmiServiceAccessPoint connect() throws ExternalServiceException {
        try {
            CoreConfiguration config = CoreConfiguration.getInstance();
            if (config.isIntegratedMode()) {
                try {
                    voss.mplsnms.MplsnmsNaefService.start();
                } catch (Exception e) {
                    log.warn("got exception from server (already started?).", e);
                }
                return connectInternal();
            } else {
                return connectRemote(config);
            }
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    private MplsnmsRmiServiceAccessPoint connectRemote(CoreConfiguration config) throws NotBoundException,
            MalformedURLException, RemoteException {
        if (this.ap != null) {
            try {
                this.ap.getServiceFacade();
                return this.ap;
            } catch (RemoteException e) {
                log.warn("got error from existing remote [MplsnmsRmiServiceAccessPoint].", e);
                this.ap = null;
            }
        }
        MplsnmsRmiServiceAccessPoint newAccessPoint = (MplsnmsRmiServiceAccessPoint) Naming.lookup(config.getDbServerUrl());
        if (newAccessPoint == null) {
            throw new IllegalStateException("inventory server not found on " + config.getDbServerUrl());
        }
        this.ap = newAccessPoint;
        registTriggers();
        return ap;
    }

    private MplsnmsRmiServiceAccessPoint connectInternal() {
        if (this.ap != null) {
            return ap;
        }
        this.ap = MplsnmsNaefService.getRmiServiceAccessPoint();
        registTriggers();
        return ap;
    }

    private void registTriggers() {
        try {
            log.info("starting: ScriptTrigger.");
            TriggerManager manager = TriggerManager.getInstance();
            InventoryConfiguration conf = InventoryConfiguration.getInstance();
            conf.getTriggerServiceClasses().forEach(clazz -> {
                try {
                    TriggerService service = clazz.newInstance();
                    manager.register(service);
                    service.start();
                    log.info("registered: " + clazz.getName());
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("trigger-service instantiate error.", e);
                }
            });
            manager.start();
            log.info("registered: ScriptTrigger.");
        } catch (Exception e) {
            log.warn("failed to start trigger.", e);
        }
    }

    private MplsnmsRmiServiceAccessPoint getRmiServiceAccessPoint() throws InventoryException, ExternalServiceException {
        if (this.ap == null) {
            return connect();
        }
        try {
            this.ap.getServiceFacade();
            return this.ap;
        } catch (RemoteException e) {
            log.error("cannot connect access-point.", e);
            return connect();
        } catch (Exception e) {
            log.error("cannot connect access-point.", e);
            return connect();
        }
    }

    @Override
    public NaefDtoFacade getDtoFacade() throws IOException, RemoteException, ExternalServiceException {
        try {
            MplsnmsRmiServiceAccessPoint ap = getRmiServiceAccessPoint();
            NaefDtoFacade facade = ap.getServiceFacade().getDtoFacade();
            return facade;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    @Override
    public NaefShellFacade getShellFacade() throws IOException, RemoteException, ExternalServiceException {
        try {
            MplsnmsRmiServiceAccessPoint ap = getRmiServiceAccessPoint();
            return ap.getServiceFacade().getShellFacade();
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    @Override
    public void close() throws IOException, RemoteException, ExternalServiceException {
        System.err.println("close() : not supported.");
    }
}
