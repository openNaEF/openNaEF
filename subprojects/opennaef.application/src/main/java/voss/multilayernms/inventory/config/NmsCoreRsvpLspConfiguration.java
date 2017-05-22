package voss.multilayernms.inventory.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration.Node;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class NmsCoreRsvpLspConfiguration extends NmsCoreInventoryObjectConfiguration {
    private static final Logger log = LoggerFactory.getLogger(NmsCoreRsvpLspConfiguration.class);

    public static final String NAME = "NmsCoreRsvpLspConfiguration";
    public static final String FILE_NAME = "NmsCoreRsvpLspConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore RsvpLsp configuration.";

    public static final String KEY_HOP_IP_PATH_FIELD_NAME = "hop-and-ip-path-fields-name";

    private static NmsCoreRsvpLspConfiguration instance = null;

    private static List<String> hopIpPathFieldsName = null;

    public static NmsCoreRsvpLspConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCoreRsvpLspConfiguration();
        }
        return instance;
    }

    public NmsCoreRsvpLspConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected boolean loadExtraConfigration() throws IOException {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());

            for (Object obj : config.getRootNode().getChildren()) {
                Node node = (Node) obj;
                if (node.getName().equals(KEY_HOP_IP_PATH_FIELD_NAME)) {
                    List<String> values = new ArrayList<String>();

                    for (Object value : node.getChildren()) {
                        values.add((String) ((Node) value).getValue());
                    }
                    setHopIpPathFieldsName(values);
                    return true;
                }
            }
        } catch (ConfigurationException e) {
            log.error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + getConfigFile());
        }
        return false;
    }

    public List<String> getHopIpPathFieldsName() {
        return hopIpPathFieldsName;
    }

    public void setHopIpPathFieldsName(List<String> hopIpPathFieldsName) {
        NmsCoreRsvpLspConfiguration.hopIpPathFieldsName = hopIpPathFieldsName;
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}