package voss.core.server.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.NaefBridge;
import voss.core.server.util.ExceptionUtils;

import java.io.File;
import java.io.IOException;

public class CoreConfiguration extends ServiceConfiguration {
    public static final String NAME = "CoreConfig";
    public static final String FILE_NAME = "CoreConfiguration.xml";
    public static final String DESCRIPTION = "naef-core configuration.";

    public static final String KEY_NAEF_BRIDGE = "naef-bridge";
    public static final String KEY_ATTRIBUTE_POLICY = "attribute-policy";

    public static final String KEY_DEBUG_MODE = "debug-mode";
    public static final String KEY_DEBUG_DIR = "debug-dir";

    public static final String KEY_NAEF_SERVER_URL = "db-server-address";
    public static final String KEY_NAEF_SERVER_PORT = "db-server-port";
    public static final String KEY_NAEF_SERVICE_NAME = "db-service-name";
    public static final String KEY_NAEF_SERVICE_MODE = "db-service-mode";
    public static final String KEY_NAEF_HISTORY_URL = "db-history-url";

    public static final String KEY_MODE_SEPARATED = "separated";
    public static final String KEY_MODE_INTEGRATED = "integrated";

    public static final String KEY_UPDATE_SERVER_URL = "update-server-address";
    public static final String KEY_UPDATE_SERVER_PORT = "update-server-port";

    private static CoreConfiguration instance = null;

    public synchronized static CoreConfiguration getInstance() {
        if (instance == null) {
            try {
                instance = new CoreConfiguration();
                instance.reloadConfiguration();
            } catch (IOException e) {
                throw new IllegalStateException("failed to load config.", e);
            }
        }
        return instance;
    }

    private Class<? extends NaefBridge> naefBridgeClass = null;
    private AttributePolicy attributePolicy = null;
    private Class<? extends AttributePolicy> attributePolicyClass = null;
    private NaefBridge naefBridge = null;
    @SuppressWarnings("unused")
    private NaefBridge updateNeafBridge = null;
    private boolean isDebug = false;
    private String debugDirName = "./debug";
    private String dbServerAddress = "127.0.0.1";
    private int dbServerPort = 38000;
    private String dbServiceName = "inventory";
    private String dbServiceMode = KEY_MODE_INTEGRATED;
    private String updateServerAddress = "127.0.0.1";
    private int updateServerPort = 38000;
    private String dbHistoryUrl = "http://127.0.0.1:38004/mvo.Dump?id=";

    private CoreConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected synchronized void reloadConfigurationInner() throws IOException {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());

            String _naefBridgeClassName = config.getString(KEY_NAEF_BRIDGE);
            Class<? extends NaefBridge> _bridgeClass = loadClass(_naefBridgeClassName, NaefBridge.class);
            if (!isBridgeSettingValid(_bridgeClass)) {
                throw new IllegalStateException("naef-brdige cannot change.");
            }

            String _attributePolicyClassName = config.getString(KEY_ATTRIBUTE_POLICY);
            Class<? extends AttributePolicy> _policyClass = loadClass(_attributePolicyClassName, AttributePolicy.class);

            String __debugMode = config.getString(KEY_DEBUG_MODE);
            boolean _debugMode = (__debugMode == null ? false : __debugMode.toLowerCase().equals("debug"));
            String _debugDirName = config.getString(KEY_DEBUG_DIR);

            String _inventoryServerAddress = config.getString(KEY_NAEF_SERVER_URL);
            String _inventoryServerPort = config.getString(KEY_NAEF_SERVER_PORT);
            int inventoryPort = Integer.parseInt(_inventoryServerPort);
            String _inventoryServiceName = config.getString(KEY_NAEF_SERVICE_NAME);
            String _inventoryHistoryUrl = config.getString(KEY_NAEF_HISTORY_URL);

            String _inventoryServerMode = null;
            try {
                _inventoryServerMode = config.getString(KEY_NAEF_SERVICE_MODE);
            } catch (Exception e) {
                log().warn("failed to get mode", e);
            }

            String _updateServerAddress = config.getString(KEY_NAEF_SERVER_URL);
            String _updateServerPort = config.getString(KEY_NAEF_SERVER_PORT);
            int updateServerPort = Integer.parseInt(_updateServerPort);

            if (!_bridgeClass.equals(this.naefBridgeClass)) {
                this.naefBridge = null;
                this.updateNeafBridge = null;
                log().info("reset naef bridge.");
            }
            if (!_policyClass.equals(this.attributePolicyClass)) {
                this.attributePolicy = null;
                log().info("reset attribute policy.");
            }
            this.naefBridgeClass = _bridgeClass;
            this.attributePolicyClass = _policyClass;
            this.isDebug = _debugMode;
            this.debugDirName = _debugDirName;
            this.dbServerAddress = _inventoryServerAddress;
            this.dbServerPort = inventoryPort;
            this.dbServiceName = _inventoryServiceName;
            if (_inventoryServerMode != null) {
                this.dbServiceMode = _inventoryServerMode;
            }
            this.dbHistoryUrl = _inventoryHistoryUrl;
            this.updateServerAddress = _updateServerAddress;
            this.updateServerPort = updateServerPort;

            log().info("naef bridge=" + this.naefBridgeClass.getName());
            log().info("attribute policy=" + this.attributePolicyClass.getName());
            log().info("debug-dir=" + this.debugDirName);
            log().info("inventory service mode=" + this.dbServiceMode);
            log().info("inventory url=" + getDbServerAddress()
                    + ":" + getDbServerPort()
                    + "/" + getDbServiceName());
            log().info("update url=" + getUpdateServerAddress()
                    + ":" + getUpdateServerPort()
                    + "/" + getDbServiceName());
            log().info("inventory history URL=" + getInventoryHistoryUrl());
            log().info("reload finished.");
        } catch (ConfigurationException e) {
            log().error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + FILE_NAME);
        }
    }

    private boolean isBridgeSettingValid(Class<?> newClass) {
        if (newClass == null) {
            throw new IllegalStateException("no naef-bridge class.");
        }
        if (!this.dbServiceMode.equals(KEY_MODE_INTEGRATED)) {
            return true;
        }
        if (this.naefBridge == null) {
            return true;
        } else if (this.naefBridgeClass.equals(newClass)) {
            return true;
        }
        return false;
    }

    protected void publishServices() {
    }

    public synchronized NaefBridge getBridge() {
        if (this.naefBridge == null) {
            try {
                this.naefBridge = this.naefBridgeClass.newInstance();
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
        return this.naefBridge;
    }

    public synchronized NaefBridge getUpdateServerBridge() {
        return getBridge();
    }

    public synchronized boolean isBridgeInstanciated() {
        return this.naefBridge != null;
    }

    public synchronized AttributePolicy getAttributePolicy() {
        if (this.attributePolicy == null) {
            try {
                this.attributePolicy = this.attributePolicyClass.newInstance();
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
        return this.attributePolicy;
    }

    public synchronized boolean isDebug() {
        return this.isDebug;
    }

    public synchronized String getDebugDirName() {
        return this.debugDirName;
    }

    public synchronized File getDebugDir() {
        if (this.debugDirName == null) {
            throw new IllegalArgumentException();
        }
        File dir = new File(this.debugDirName);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                throw new IllegalStateException("failed to create debug-dir:" + this.debugDirName);
            }
        }
        return dir;
    }

    public synchronized String getDbServerUrl() {
        return "rmi://" + this.dbServerAddress + ":" + this.dbServerPort + "/" + this.dbServiceName;
    }

    public synchronized String getDbServerAddress() {
        return this.dbServerAddress;
    }

    public synchronized int getDbServerPort() {
        return this.dbServerPort;
    }

    public synchronized String getDbServiceName() {
        return this.dbServiceName;
    }

    public synchronized String getDbServiceMode() {
        return this.dbServiceMode;
    }

    public synchronized boolean isSeparatedMode() {
        return this.dbServiceMode.equals(KEY_MODE_SEPARATED);
    }

    public synchronized boolean isIntegratedMode() {
        return this.dbServiceMode.equals(KEY_MODE_INTEGRATED);
    }

    public synchronized String getUpdateServerAddress() {
        return this.updateServerAddress;
    }

    public synchronized int getUpdateServerPort() {
        return this.updateServerPort;
    }

    public synchronized String getInventoryHistoryUrl() {
        return this.dbHistoryUrl;
    }

    private synchronized static Logger log() {
        return LoggerFactory.getLogger(CoreConfiguration.class);
    }
}