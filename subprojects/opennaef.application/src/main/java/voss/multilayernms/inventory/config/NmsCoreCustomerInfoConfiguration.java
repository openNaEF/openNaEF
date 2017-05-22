package voss.multilayernms.inventory.config;

import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;

public class NmsCoreCustomerInfoConfiguration extends NmsCoreInventoryObjectConfiguration {
    public static final String NAME = "NmsCoreCustomerInfoConfiguration";
    public static final String FILE_NAME = "NmsCoreCustomerInfoConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore CustomerInfo configuration.";

    private static NmsCoreCustomerInfoConfiguration instance = null;

    public static NmsCoreCustomerInfoConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreCustomerInfoConfiguration();
        }
        return instance;
    }

    public NmsCoreCustomerInfoConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}