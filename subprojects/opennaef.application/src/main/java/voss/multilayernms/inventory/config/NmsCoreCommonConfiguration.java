package voss.multilayernms.inventory.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfiguration;
import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class NmsCoreCommonConfiguration extends ServiceConfiguration {
    private static final Logger log = LoggerFactory.getLogger(NmsCoreCommonConfiguration.class);

    public static final String NAME = "NmsCoreCommonConfiguration";
    public static final String FILE_NAME = "NmsCoreCommonConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore Common configuration.";

    public static final String KEY_DISPATCHER_IP_ADDRESS = "dispatcher-ip-address";

    private List<String> dispatcherIpAddress = null;

    private String diffsetStoreDirectoryName = "./diffset";

    private static NmsCoreCommonConfiguration instance = null;

    public static NmsCoreCommonConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreCommonConfiguration(FILE_NAME, NAME, DESCRIPTION);
        }
        return instance;
    }

    public NmsCoreCommonConfiguration(String fileName, String name,
                                      String description) throws IOException {
        super(fileName, name, description);
        reloadConfiguration();
        File dir = new File(this.diffsetStoreDirectoryName);
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            if (!result) {
                throw new IOException("failed to create diffset directory:" + this.diffsetStoreDirectoryName);
            }
        }
    }

    @Override
    protected void reloadConfigurationInner() throws IOException {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());

            log.debug("loading config:" + getConfigName());

            for (Object obj : config.getRootNode().getChildren()) {
                Node node = (Node) obj;

                if (node.getName().equals(KEY_DISPATCHER_IP_ADDRESS)) {
                    List<String> values = new ArrayList<String>();

                    for (Object value : node.getChildren()) {
                        values.add((String) ((Node) value).getValue());
                    }
                    setDispatcherIpAddress(values);
                } else {
                    log.info("unknown node:" + node.getName());
                }

            }
        } catch (ConfigurationException e) {
            log.error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + getConfigFile());
        }

    }


    public List<String> getDispatcherIpAddress() {
        return dispatcherIpAddress;
    }

    public void setDispatcherIpAddress(List<String> dispatcherIpAddress) {
        this.dispatcherIpAddress = dispatcherIpAddress;
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}