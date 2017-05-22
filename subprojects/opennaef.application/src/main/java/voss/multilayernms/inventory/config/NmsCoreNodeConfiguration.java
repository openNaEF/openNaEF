package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreNodeConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreNodeConfiguration";
    public static final String FILE_NAME = "NmsCoreNodeConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Node configuration.";

    private static NmsCoreNodeConfiguration instance = null;

    public static NmsCoreNodeConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreNodeConfiguration();
        }
        return instance;
    }

    public NmsCoreNodeConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}