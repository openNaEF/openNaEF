package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreSubnetIpConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreSubnetIpConfiguration";
    public static final String FILE_NAME = "NmsCoreSubnetIpConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Subnet Ip configuration.";

    private static NmsCoreSubnetIpConfiguration instance = null;

    public static NmsCoreSubnetIpConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreSubnetIpConfiguration();
        }
        return instance;
    }

    public NmsCoreSubnetIpConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}