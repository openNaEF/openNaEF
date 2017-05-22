package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreLinkConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreLinkConfiguration";
    public static final String FILE_NAME = "NmsCoreLinkConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Link configuration.";

    private static NmsCoreLinkConfiguration instance = null;

    public static NmsCoreLinkConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreLinkConfiguration();
        }
        return instance;
    }

    public NmsCoreLinkConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}