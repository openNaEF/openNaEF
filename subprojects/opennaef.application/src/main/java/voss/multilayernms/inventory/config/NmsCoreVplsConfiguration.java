package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreVplsConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreVplsConfiguration";
    public static final String FILE_NAME = "NmsCoreVplsConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Vpls configuration.";

    private static NmsCoreVplsConfiguration instance = null;

    public static NmsCoreVplsConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreVplsConfiguration();
        }
        return instance;
    }

    public NmsCoreVplsConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}