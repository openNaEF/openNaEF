package voss.nms.inventory.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfiguration;
import voss.core.server.exception.InventoryException;
import voss.model.Protocol;
import voss.nms.inventory.diff.network.DefaultNodeInfoFactory;
import voss.nms.inventory.diff.network.NodeInfoFactory;
import voss.nms.inventory.trigger.TriggerManager;
import voss.nms.inventory.trigger.TriggerService;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InventoryConfiguration extends ServiceConfiguration {
    public static final String NAME = "InventoryConfig";
    public static final String FILE_NAME = "InventoryConfiguration.xml";
    public static final String DESCRIPTION = "Inventory base configuration.";

    public static final String KEY_URL_BASE = "url-base";
    public static final String KEY_METADATA_DIR = "metadata-dir";
    public static final String KEY_PORTS_PANEL_LINES = "ports-panel-lines";
    public static final String KEY_SYNC_FEATURE = "sync-feature";

    public static final String KEY_DISCOVERY_MAX_TASK = "discovery-service-max-thread";

    public static final String KEY_INVENTORY_SERVICE_PORT = "inventory-service-port";

    public static final String KEY_TRIGGER_MANAGER_ID = "trigger-manager-id";

    public static final String KEY_CONFIGURATOR_SERVER_URL = "configurator-server-address";
    public static final String KEY_CONFIGURATOR_SERVER_PORT = "configurator-server-port";
    public static final String KEY_CONFIGURATOR_SERVICE_NAME = "configurator-service-name";

    public static final String KEY_DEFAULT_SNMP_COMMUNITY = "default-snmp-community-string";
    public static final String KEY_DEFAULT_SNMP_METHOD = "default-snmp-method";
    public static final String KEY_NODEINFO_FACTORY = "nodeinfo-factory";

    public static final String KEY_TRIGGER_SERVICE_SECTION = "trigger-services";
    public static final String KEY_TRIGGER_SERVICE = "trigger";

    private static InventoryConfiguration instance = null;

    public synchronized static InventoryConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new InventoryConfiguration();
            instance.reloadConfiguration();
        }
        return instance;
    }

    private String urlBase = "/opennaef.application";
    private String metadataDir = "c:/metadata";
    private int portsPanelLines = 0;
    private boolean enableSyncFeature = true;
    private int discoveryMaxTask = 20;

    private int inventoryServicePort = 4649;
    private String triggerManagerID = TriggerManager.LISTENER_NAME;

    private String configuratorServerAddress = "127.0.0.1";
    private int configuratorServerPort = 1099;
    private String configuratorServiceName = "Configurator";

    private String defaultSnmpCommunityString = "public";
    private String defaultSnmpMethod = Protocol.SNMP_V2C_GETNEXT.caption;
    private String nodeInfoFactoryClassName = DefaultNodeInfoFactory.class.getName();

    private List<Class<? extends TriggerService>> triggerServiceClasses = new CopyOnWriteArrayList<>();

    private InventoryConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected synchronized void reloadConfigurationInner() throws IOException {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());

            String _urlBase = config.getString(KEY_URL_BASE);
            String _metadataDir = config.getString(KEY_METADATA_DIR);
            String _portsPanelLines = config.getString(KEY_PORTS_PANEL_LINES);
            int portsPanelLines = Integer.parseInt(_portsPanelLines);
            String _syncFeature = config.getString(KEY_SYNC_FEATURE);

            String __inventoryServicePort = config.getString(KEY_INVENTORY_SERVICE_PORT);
            int _inventoryServicePort = Integer.parseInt(__inventoryServicePort);
            String _triggerManagerID = config.getString(KEY_TRIGGER_MANAGER_ID, TriggerManager.LISTENER_NAME);
            String __discoveryMaxTask = config.getString(KEY_DISCOVERY_MAX_TASK);
            int _discoveryMaxTask = 0;
            try {
                _discoveryMaxTask = Integer.parseInt(__discoveryMaxTask);
            } catch (NumberFormatException e) {
                log().warn("failed to get [" + KEY_DISCOVERY_MAX_TASK + "]", e);
            }

            String _configuratorServerAddress = config.getString(KEY_CONFIGURATOR_SERVER_URL);
            String _configuratorServerPort = config.getString(KEY_CONFIGURATOR_SERVER_PORT);
            int configuratorServerPort = Integer.parseInt(_configuratorServerPort);
            String _configuratorServiceName = config.getString(KEY_CONFIGURATOR_SERVICE_NAME);

            String _defaultSnmpCommunityString = config.getString(KEY_DEFAULT_SNMP_COMMUNITY);
            String _defaultSnmpMethod = config.getString(KEY_DEFAULT_SNMP_METHOD);

            String _nodeInfoFactory = config.getString(KEY_NODEINFO_FACTORY);
            loadClass(_nodeInfoFactory, NodeInfoFactory.class);
            this.urlBase = _urlBase;
            this.metadataDir = _metadataDir;
            this.portsPanelLines = portsPanelLines;
            this.enableSyncFeature = (_syncFeature != null && "enabled".equals(_syncFeature.toLowerCase()));
            this.discoveryMaxTask = _discoveryMaxTask;
            this.inventoryServicePort = _inventoryServicePort;
            this.triggerManagerID = _triggerManagerID;
            this.configuratorServerAddress = _configuratorServerAddress;
            this.configuratorServerPort = configuratorServerPort;
            this.configuratorServiceName = _configuratorServiceName;
            this.defaultSnmpCommunityString = _defaultSnmpCommunityString;
            this.defaultSnmpMethod = _defaultSnmpMethod;
            this.nodeInfoFactoryClassName = _nodeInfoFactory;

            this.triggerServiceClasses.clear();
            @SuppressWarnings("unchecked")
            List<String> triggerClasses = (List<String>) config.getList(KEY_TRIGGER_SERVICE_SECTION + "." + KEY_TRIGGER_SERVICE);
            for (String className : triggerClasses) {
                try {
                    Class<?> clazz = Class.forName(className);
                    if (TriggerService.class.isAssignableFrom(clazz)) {
                        this.triggerServiceClasses.add((Class<? extends TriggerService>) clazz);
                    } else {
                        log().warn("not trigger-service-class. " + className);
                    }
                } catch (ClassNotFoundException e) {
                    log().warn("trigger-service not found. " + className);
                }
            }

            log().info("Configurator URL=" + getConfiguratorServerAddress());
            log().info("defaultSnmpCommunityString=" + getDefaultSnmpCommunityString());
            log().info("defaultSnmpMethod=" + getDefaultSnmpMethod());
            log().info("reload finished.");
        } catch (ConfigurationException e) {
            log().error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + FILE_NAME);
        }
    }

    @Override
    protected void publishServices() {
    }

    public synchronized int getInventoryServicePort() {
        return this.inventoryServicePort;
    }

    public synchronized Registry getInventoryServiceRegistry() throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(this.inventoryServicePort);
            registry.list();
            return registry;
        } catch (RemoteException e) {
            log().warn("(ignorable) failed to get registry.");
            log().trace("exception detail:", e);
            Registry registry = LocateRegistry.createRegistry(this.inventoryServicePort);
            return registry;
        }
    }

    public synchronized String getTriggerManagerID() {
        return this.triggerManagerID;
    }

    public synchronized String getConfiguratorServerAddress() {
        return this.configuratorServerAddress;
    }

    public synchronized int getConfiguratorServerPort() {
        return this.configuratorServerPort;
    }

    public synchronized String getConfiguratorServiceName() {
        return this.configuratorServiceName;
    }

    public synchronized String getUrlBase() {
        return this.urlBase;
    }

    public synchronized String getMetadataDir() {
        return this.metadataDir;
    }

    public synchronized int getPortsPanelLines() {
        return this.portsPanelLines;
    }

    public synchronized boolean isEnableSyncFeature() {
        return this.enableSyncFeature;
    }

    private synchronized static Logger log() {
        return LoggerFactory.getLogger(InventoryConfiguration.class);
    }

    public int getDiscoveryMaxTask() {
        return discoveryMaxTask;
    }

    public void setDiscoveryMaxTask(int discoveryMaxTask) {
        this.discoveryMaxTask = discoveryMaxTask;
    }

    public synchronized String getDefaultSnmpCommunityString() {
        return this.defaultSnmpCommunityString;
    }

    public synchronized void setDefaultSnmpCommunityString(String community) {
        this.defaultSnmpCommunityString = community;
    }

    public synchronized String getDefaultSnmpMethod() {
        return this.defaultSnmpMethod;
    }

    public synchronized void setDefaultSnmpMethod(String s) {
        this.defaultSnmpMethod = s;
    }

    public synchronized String getNodeInfoFactoryClassName() {
        return this.nodeInfoFactoryClassName;
    }

    public synchronized NodeInfoFactory getNodeInfoFactory() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> cls = Class.forName(this.nodeInfoFactoryClassName);
        Class<? extends NodeInfoFactory> factoryClass = cls.asSubclass(NodeInfoFactory.class);
        return factoryClass.newInstance();
    }

    public List<Class<? extends TriggerService>> getTriggerServiceClasses() {
        return Collections.unmodifiableList(this.triggerServiceClasses);
    }
}