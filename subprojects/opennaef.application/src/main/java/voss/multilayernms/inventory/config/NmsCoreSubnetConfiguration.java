package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreSubnetConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreSubnetConfiguration";
    public static final String FILE_NAME = "NmsCoreSubnetConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Subnet configuration.";

    private static NmsCoreSubnetConfiguration instance = null;

    public static NmsCoreSubnetConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreSubnetConfiguration();
        }
        return instance;
    }

    public NmsCoreSubnetConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}