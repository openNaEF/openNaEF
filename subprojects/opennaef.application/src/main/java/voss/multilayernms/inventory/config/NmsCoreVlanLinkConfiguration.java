package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreVlanLinkConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreVlanLinkConfiguration";
    public static final String FILE_NAME = "NmsCoreVlanLinkConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore VlanLink configuration.";

    private static NmsCoreVlanLinkConfiguration instance = null;

    public static NmsCoreVlanLinkConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreVlanLinkConfiguration();
        }
        return instance;
    }

    public NmsCoreVlanLinkConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}