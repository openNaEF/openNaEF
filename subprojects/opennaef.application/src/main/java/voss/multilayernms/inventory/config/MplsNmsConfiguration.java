package voss.multilayernms.inventory.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfiguration;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.nmscore.rmi.NmsCoreService;
import voss.multilayernms.inventory.scheduler.SchedulerService;
import voss.nms.inventory.config.InventoryConfiguration;

import java.io.FileWriter;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.Registry;

public class MplsNmsConfiguration extends ServiceConfiguration {
    public static final String NAME = "MplsNmsConfiguration";
    public static final String FILE_NAME = "MplsNmsConfiguration.xml";
    public static final String DESCRIPTION = "MPLS-NMS specific configuration.";

    public static final String INVENTORY_RMI_SERVICE_PORT = "inventory-rmi-service-port";
    private static MplsNmsConfiguration instance = null;

    public synchronized static MplsNmsConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new MplsNmsConfiguration();
            instance.reloadConfiguration();
        }
        return instance;
    }

    private int inventoryRmiServicePort = 4649;
    private XMLConfiguration config = new XMLConfiguration();

    private MplsNmsConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    public XMLConfiguration getXMLConfiguration() {
        return config;
    }

    @Override
    protected synchronized void reloadConfigurationInner() throws IOException {
        try {
            config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());
            this.inventoryRmiServicePort = config.getInt(INVENTORY_RMI_SERVICE_PORT);
            log().info("reload finished.");
        } catch (ConfigurationException e) {
            log().error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + FILE_NAME);
        }
    }

    public synchronized void saveConfiguration() {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.addProperty(INVENTORY_RMI_SERVICE_PORT, this.inventoryRmiServicePort);
            FileWriter writer = new FileWriter(getConfigFile());
            config.save(writer);
        } catch (ConfigurationException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        } catch (IOException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    protected synchronized void publishServices() throws IOException, InventoryException, ExternalServiceException {
        log().info("called publishServices");

        InventoryConfiguration config = InventoryConfiguration.getInstance();
        int servicePort = config.getInventoryServicePort();
        Registry registry = config.getInventoryServiceRegistry();
        NmsCoreService nmsCoreService = new NmsCoreService(servicePort);
        Remote remote = null;
        try {
            remote = registry.lookup(NmsCoreService.SERVICE_NAME);
        } catch (NotBoundException e) {
        }
        if (remote == null) {
            try {
                registry.bind(NmsCoreService.SERVICE_NAME, nmsCoreService);
            } catch (AlreadyBoundException e) {
                throw new IllegalStateException("already bound: ", e);
            }
        } else {
            registry.rebind(NmsCoreService.SERVICE_NAME, nmsCoreService);
        }
        log().info("starting NmsCoreService.");

        SchedulerService scheduler = SchedulerService.getInstance();
        scheduler.stopService();
        scheduler.startService();

        log().info("completed publishServices");
    }

    private static Logger log() {
        return LoggerFactory.getLogger(MplsNmsConfiguration.class);
    }
}