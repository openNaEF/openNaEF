package voss.discovery.agent.vmware.collector;

import com.vmware.vim25.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.vmware.collector.traverser.HostNetworkSystemTraverser;
import voss.discovery.agent.vmware.collector.traverser.Traverser;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

class Collector {
    private String serverName;
    private String userName;
    private String password;
    private Traverser traverser;

    private VimPortType service;
    private ServiceContent siContent;

    private final static Logger log = LoggerFactory.getLogger(Collector.class);

    public Collector(String serverName, String userName, String password, Traverser traverser) {
        this.serverName = serverName;
        this.userName = userName;
        this.password = password;
        this.traverser = traverser;
    }

    public ObjectContent[] getProperties() throws IOException {
        try {
            log.debug("VMWare getProperties1 =  " + serverName + " login start ");
            login(serverName, userName, password);
            refreshNetworkSystem();
            ObjectContent[] properties = traverser.collectProperty(service, siContent);
            log.debug("VMWare getProperties2 = " + properties + " on serverName = " + serverName);
            logout();
            log.debug("VMWare getProperties3 =  " + serverName + " logout finished ");
            return properties;
        } catch (RuntimeFault e) {
            throw new IOException("failed to connect to ESX Server (" + this.serverName + ")", e);
        } catch (MalformedURLException e) {
            throw new IOException("failed to connect to ESX Server (" + this.serverName + ")", e);
        } catch (RemoteException e) {
            throw new IOException("failed to connect to ESX Server (" + this.serverName + ")", e);
        } catch (ServiceException e) {
            throw new IOException("failed to connect to ESX Server (" + this.serverName + ")", e);
        } catch (RuntimeException e) {
            throw new IOException("failed to connect to ESX Server (" + this.serverName + ")", e);
        }
    }

    private void refreshNetworkSystem() {
        try {
            ObjectContent[] objectContents = new HostNetworkSystemTraverser().collectProperty(service, siContent);
            log.info("HostNetworkSystemTraverser is " + objectContents);
            if (objectContents != null) {
                for (ObjectContent oc : objectContents) {
                    for (DynamicProperty dp : oc.getPropSet()) {
                        Object val = dp.getVal();
                        if (val instanceof ManagedObjectReference) {
                            ManagedObjectReference ref = (ManagedObjectReference) val;
                            if (ref.getType().equals("HostNetworkSystem")) {
                                service.refreshNetworkSystem(ref);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            log.debug("failed to get HostNetworkSystem" + e);
        }
    }

    private void login(String serverName, String userName, String password) throws MalformedURLException, ServiceException, RuntimeFault, RemoteException {
        String url = "https://" + serverName + "/sdk";
        log.debug("VMWare login url = " + url);
        ManagedObjectReference siRef;
        VimServiceLocator locator;

        System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");

        siRef = new ManagedObjectReference();
        siRef.setType("ServiceInstance");
        siRef.set_value("ServiceInstance");

        locator = new VimServiceLocator();
        locator.setMaintainSession(true);
        service = locator.getVimPort(new URL(url));
        siContent = service.retrieveServiceContent(siRef);
        log.debug("siContent.getSessionManager() = " + siContent.getSessionManager());
        if (siContent.getSessionManager() != null) {
            try {
                service.login(siContent.getSessionManager(),
                        userName,
                        password,
                        null);
            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void logout() throws RuntimeFault, RemoteException {
        service.logout(siContent.getSessionManager());
        service = null;
        siContent = null;
    }
}