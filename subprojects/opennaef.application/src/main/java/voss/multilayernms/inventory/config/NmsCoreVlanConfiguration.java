package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreVlanConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreVlanConfiguration";
    public static final String FILE_NAME = "NmsCoreVlanConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Vlan configuration.";

    private static NmsCoreVlanConfiguration instance = null;

    public static NmsCoreVlanConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreVlanConfiguration();
        }
        return instance;
    }

    public NmsCoreVlanConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}