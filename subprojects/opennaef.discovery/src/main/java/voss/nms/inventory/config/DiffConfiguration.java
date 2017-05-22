package voss.nms.inventory.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfiguration;
import voss.core.server.config.VossConfigException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.diff.network.*;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;

public class DiffConfiguration extends ServiceConfiguration {
    public static final String NAME = "DiffConfiguration";
    public static final String FILE_NAME = "DiffConfiguration.xml";
    public static final String DESCRIPTION = "Diff service configuration.";

    public static final String KEY_DEBUG_STORE_DIR = "debug-store-directory";
    public static final String KEY_DIFFSET_STORE_DIR = "diffset-store-directory";
    public static final String KEY_DIFF_POLICY = "diff-policy";
    public static final String KEY_VPN_DRIVER = "vpn-driver";
    public static final String KEY_NODEINFO_FACTORY = "nodeinfo-factory";
    public static final String ATTR_DISPLAY_NAME = ".display-name";
    public static final String ATTR_AUTO_APPLY = ".auto-apply";

    public static final String KEY_DISCOVERY_BASE = "discovery";
    public static final String KEY_DISCOVERY_DISPLAY_NAME = KEY_DISCOVERY_BASE + ATTR_DISPLAY_NAME;
    public static final String KEY_DISCOVERY_AUTO_APPLY = KEY_DISCOVERY_BASE + ATTR_AUTO_APPLY;
    public static final String KEY_DISCOVERY_SERVER_ADDRESS = KEY_DISCOVERY_BASE + ".server-address";
    public static final String KEY_DISCOVERY_SERVER_PORT = KEY_DISCOVERY_BASE + ".server-port";
    public static final String KEY_DISCOVERY_NODE_URL = KEY_DISCOVERY_BASE + ".node-url";
    public static final String KEY_DISCOVERY_RUN_MODE = KEY_DISCOVERY_BASE + ".run-mode";
    public static final String MODE_REMOTE = "remote";
    public static final String MODE_IN_PROCESS = "in-process";

    private static DiffConfiguration instance = null;

    public synchronized static DiffConfiguration getInstance() {
        if (instance == null) {
            try {
                instance = new DiffConfiguration();
                instance.reloadConfiguration();
            } catch (IOException e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
        return instance;
    }

    private String discoveryDisplayName = "NETWORK";
    private boolean discoveryAutoApply = false;
    private String discoveryRunMode = MODE_IN_PROCESS;
    private String discoveryServerAddress = "127.0.0.1";
    private int discoveryServerPort = 4444;
    private String discoveryNodeUrl = "http://127.0.0.1:1226/show-device/?site=DEFAULT&device-id=";
    private String diffsetStoreDirectoryName = "./diff";
    private String debugStoreDirectoryName = "./debug";
    private String diffPolicyClassName = DefaultDiffPolicy.class.getName();
    private String vpnDriverClassName = NullVpnDriver.class.getName();
    private String nodeInfoFactoryClassName = DefaultNodeInfoFactory.class.getName();

    private DiffConfiguration() throws IOException {
        super(FILE_NAME, NAME, DESCRIPTION);
    }

    @Override
    protected synchronized void reloadConfigurationInner() throws IOException {
        try {
            XMLConfiguration config = new XMLConfiguration();
            config.setDelimiterParsingDisabled(true);
            config.load(getConfigFile());

            String _runMode = MODE_IN_PROCESS;
            try {
                _runMode = config.getString(KEY_DISCOVERY_RUN_MODE);
            } catch (Exception e) {
                log().warn("failed to get mode", e);
            }

            String _discoveryDisplayName = config.getString(KEY_DISCOVERY_DISPLAY_NAME);
            String __discoveryAutoApply = config.getString(KEY_DISCOVERY_AUTO_APPLY);
            boolean _discoveryAutoApply = __discoveryAutoApply == null ? false : Boolean.parseBoolean(__discoveryAutoApply);
            String _discoveryServerAddress = config.getString(KEY_DISCOVERY_SERVER_ADDRESS);
            String __discoveryServerPort = config.getString(KEY_DISCOVERY_SERVER_PORT);
            int _discoveryServerPort = Integer.parseInt(__discoveryServerPort);
            String _discoveryNodeUrl = config.getString(KEY_DISCOVERY_NODE_URL);

            String _debugStoreDirName = config.getString(KEY_DEBUG_STORE_DIR);
            String _diffsetStoreDirName = config.getString(KEY_DIFFSET_STORE_DIR);
            String _diffPolicy = config.getString(KEY_DIFF_POLICY);
            loadClass(_diffPolicy, DiffPolicy.class);
            String _vpnDriver = config.getString(KEY_VPN_DRIVER);
            loadClass(_vpnDriver, VpnDriver.class);
            String _nodeInfoFactory = config.getString(KEY_NODEINFO_FACTORY);
            loadClass(_nodeInfoFactory, NodeInfoFactory.class);
            this.debugStoreDirectoryName = _debugStoreDirName;
            this.diffsetStoreDirectoryName = _diffsetStoreDirName;
            this.diffPolicyClassName = _diffPolicy;
            this.vpnDriverClassName = _vpnDriver;
            this.nodeInfoFactoryClassName = _nodeInfoFactory;
            if (_runMode != null) {
                this.discoveryRunMode = _runMode;
            }
            this.discoveryDisplayName = _discoveryDisplayName;
            this.discoveryAutoApply = _discoveryAutoApply;
            this.discoveryServerAddress = _discoveryServerAddress;
            this.discoveryServerPort = _discoveryServerPort;
            this.discoveryNodeUrl = null;
            this.discoveryNodeUrl = _discoveryNodeUrl;
            this.discoveryDisplayName = _discoveryDisplayName;
            this.discoveryAutoApply = _discoveryAutoApply;

            log().info("diffPolicyClassName=" + getDiffPolicyClassName());
            log().info("vpnDriverClassName=" + getVpnDriverClassName());
            log().info("nodeInfoFactoryClassName=" + getNodeInfoFactoryClassName());

            log().info("discoveryDisplayName=" + getDiscoveryDisplayName());
            log().info("discoveryAutoApply=" + isDiscoveryAutoApply());
            log().info("discoveryServerAddress=" + getDiscoveryServerAddress());
            log().info("discoveryServerPort=" + getDiscoveryServerPort());
            log().info("discoveryRunMode=" + getDiscoveryRunMode());
            log().info("discoveryNodeUrl=" + getDiscoveryNodeUrl());
            log().info("debugStoreDirectoryName=" + getDebugStoreDirectoryName());
            log().info("reload finished.");
        } catch (ConfigurationException e) {
            log().error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + FILE_NAME);
        }
    }

    public synchronized boolean isRemoteMode() {
        return this.discoveryRunMode.equals(MODE_REMOTE);
    }

    public synchronized boolean isInProcessMode() {
        return this.discoveryRunMode.equals(MODE_IN_PROCESS);
    }

    public synchronized String getDiscoveryDisplayName() {
        return discoveryDisplayName;
    }

    public synchronized void setDiscoveryDisplayName(String discoveryDisplayName) {
        this.discoveryDisplayName = discoveryDisplayName;
    }

    public synchronized boolean isDiscoveryAutoApply() {
        return discoveryAutoApply;
    }

    public synchronized void setDiscoveryAutoApply(boolean discoveryAutoApply) {
        this.discoveryAutoApply = discoveryAutoApply;
    }

    public synchronized String getDiscoveryRunMode() {
        return discoveryRunMode;
    }

    public synchronized void setDiscoveryRunMode(String discoveryRunMode) {
        this.discoveryRunMode = discoveryRunMode;
    }

    public synchronized String getDiscoveryServerAddress() {
        return discoveryServerAddress;
    }

    public synchronized void setDiscoveryServerAddress(String discoveryServerAddress) {
        this.discoveryServerAddress = discoveryServerAddress;
    }

    public synchronized int getDiscoveryServerPort() {
        return discoveryServerPort;
    }

    public synchronized void setDiscoveryServerPort(int discoveryServerPort) {
        this.discoveryServerPort = discoveryServerPort;
    }

    public synchronized String getDiscoveryNodeUrl() {
        return discoveryNodeUrl;
    }

    public synchronized void setDiscoveryNodeUrl(String discoveryNodeUrl) {
        this.discoveryNodeUrl = discoveryNodeUrl;
    }

    public synchronized String getDiscoveryInventoryUrl() {
        String result = null;
        if (this.discoveryServerAddress == null) {

        }
        return "rmi://" + result + "";
    }

    public synchronized String getDebugStoreDirectoryName() {
        return debugStoreDirectoryName;
    }

    public synchronized void setDebugStoreDirectoryName(String debugStoreDirName) {
        this.debugStoreDirectoryName = debugStoreDirName;
    }

    public synchronized File getDiffsetStoreDirectory() throws IOException {
        return getDirectory(KEY_DIFFSET_STORE_DIR, this.diffsetStoreDirectoryName);
    }

    public synchronized File getDebugStoreDirectory() throws IOException {
        return getDirectory(KEY_DEBUG_STORE_DIR, this.debugStoreDirectoryName);
    }

    public synchronized String getDiffPolicyClassName() {
        return this.diffPolicyClassName;
    }

    public synchronized Class<? extends DiffPolicy> getDiffPolicyClass() throws ClassNotFoundException {
        Class<?> cls = Class.forName(this.diffPolicyClassName);
        return cls.asSubclass(DiffPolicy.class);
    }

    public synchronized String getVpnDriverClassName() {
        return this.vpnDriverClassName;
    }

    public synchronized Class<? extends VpnDriver> getVpnDriverClass() throws ClassNotFoundException {
        Class<?> cls = Class.forName(this.vpnDriverClassName);
        return cls.asSubclass(VpnDriver.class);
    }

    public synchronized String getNodeInfoFactoryClassName() {
        return this.nodeInfoFactoryClassName;
    }

    public synchronized NodeInfoFactory getNodeInfoFactory() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class<?> cls = Class.forName(this.nodeInfoFactoryClassName);
        Class<? extends NodeInfoFactory> factoryClass = cls.asSubclass(NodeInfoFactory.class);
        return factoryClass.newInstance();
    }

    public synchronized VpnDriver getVpnDriver() {
        try {
            Class<? extends VpnDriver> driverClass = getVpnDriverClass();
            return driverClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        } catch (InstantiationException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public synchronized DiffPolicy getDiffPolicy() throws ExternalServiceException {
        try {
            Class<? extends DiffPolicy> policyClass = getDiffPolicyClass();
            return policyClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        } catch (InstantiationException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    private File getDirectory(String keyName, String dirName) throws IOException {
        if (dirName == null) {
            throw new IOException("config#" + keyName + " is null.");
        }
        File dir = new File(dirName);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                throw new IOException(keyName + "["
                        + dirName + "] creation failed.");
            }
        } else if (!dir.isDirectory()) {
            throw new IOException(keyName + "["
                    + dirName + "] is not directory.");
        }
        return dir;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(DiffConfiguration.class);
    }

    @Override
    protected void publishServices() throws RemoteException, IOException, VossConfigException, InventoryException,
            ExternalServiceException {
    }

}