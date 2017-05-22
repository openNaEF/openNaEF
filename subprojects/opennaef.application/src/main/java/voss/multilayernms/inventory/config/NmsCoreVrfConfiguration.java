package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreVrfConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreVrfConfiguration";
    public static final String FILE_NAME = "NmsCoreVrfConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Vrf configuration.";

    private static NmsCoreVrfConfiguration instance = null;

    public static NmsCoreVrfConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreVrfConfiguration();
        }
        return instance;
    }

    public NmsCoreVrfConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}