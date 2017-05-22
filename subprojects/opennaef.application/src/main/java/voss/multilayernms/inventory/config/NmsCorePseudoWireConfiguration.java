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

public class NmsCorePseudoWireConfiguration extends NmsCoreInventoryObjectConfiguration {
    private static final Logger log = LoggerFactory.getLogger(NmsCorePseudoWireConfiguration.class);

    public static final String NAME = "NmsCorePseudoWireConfiguration";
    public static final String FILE_NAME = "NmsCorePseudoWireConfiguration.xml";
    public static final String DESCRIPTION = "NmsCore PseudoWire configuration.";

    public static final String KEY_RELATED_LSP_FIELD_NAME = "related-lsp-field-name";

    private static String relatedRsvpLspFieldName = null;

    private static NmsCorePseudoWireConfiguration instance = null;

    public static NmsCorePseudoWireConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new NmsCorePseudoWireConfiguration();
        }
        return instance;
    }

    public NmsCorePseudoWireConfiguration() throws IOException {
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
                if (node.getName().equals(KEY_RELATED_LSP_FIELD_NAME)) {
                    setRelatedRsvplspFieldName((String) node.getValue());
                    return true;
                }
            }
        } catch (ConfigurationException e) {
            log.error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + getConfigFile());
        }
        return false;
    }

    public String getRelatedRsvplspFieldName() {
        return relatedRsvpLspFieldName;
    }

    public void setRelatedRsvplspFieldName(String relatedRsvpLspFieldName) {
        NmsCorePseudoWireConfiguration.relatedRsvpLspFieldName = relatedRsvpLspFieldName;
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }
}