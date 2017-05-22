package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCorePortConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCorePortConfiguration";
    public static final String FILE_NAME = "NmsCorePortConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Port configuration.";

    private static NmsCorePortConfiguration instance = null;

    public static NmsCorePortConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCorePortConfiguration();
        }
        return instance;
    }

    public NmsCorePortConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}