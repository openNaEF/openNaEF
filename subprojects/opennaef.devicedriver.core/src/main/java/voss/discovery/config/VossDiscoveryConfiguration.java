package voss.discovery.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import static voss.discovery.config.VossConfigurationParams.*;

public class VossDiscoveryConfiguration {
    private static final Logger log = LoggerFactory.getLogger(VossDiscoveryConfiguration.class);
    public static final String VOSS_DIR_KEY = "vossnms.root.dir";
    private static final String CONFIG_FILE_TXT = "voss.config.txt";
    private static final String CONFIG_FILE_XML = "voss.config.xml";
    private static VossDiscoveryConfiguration instance = null;
    protected String configurationDirectory;
    protected String vossRootDirectory;
    private ResourceBundle resource = null;
    private String configFileName = null;
    private long timestamp = 0L;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    protected Properties vossConfigProperties = null;
    protected Properties extraProperties = new Properties();

    public static VossDiscoveryConfiguration getInstance() {
        if (instance == null) {
            instance = new VossDiscoveryConfiguration();
        }
        return instance;
    }

    protected VossDiscoveryConfiguration() {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public void refresh() throws IOException {
        init();
    }

    private synchronized void init() throws IOException {
        String dir = System.getProperty(VOSS_DIR_KEY);
        log.info("init():" + VOSS_DIR_KEY + "=" + dir);
        if (dir == null) {
            dir = "./";
        } else {
            if (dir.charAt(dir.length() - 1) != '/') {
                dir = dir + "/";
            }
        }
        File root = new File(dir);
        if (!root.exists() || !root.isDirectory()) {
            throw new IOException("illegal root directory: " + root.getAbsolutePath());
        }
        this.vossRootDirectory = dir;

        String confdir = this.vossRootDirectory + "config/";
        File f = new File(confdir);
        if (!f.exists()) {
            throw new IOException("config directory not found: " + confdir);
        }
        this.configurationDirectory = confdir;

        try {
            Properties properties = getProperties();
            Set<String> readKeys = new HashSet<String>();

            Properties newProperties = new Properties();
            for (VossConfigurationParams p : VossConfigurationParams.values()) {
                String value = properties.getProperty(p.toString());
                if (value == null) {
                    value = p.getDefaultValue();
                    log.debug("using default: " + value);
                }
                newProperties.put(p, value);
                readKeys.add(p.toString());
            }

            this.vossConfigProperties = newProperties;

            Properties newProperties2 = new Properties();
            for (Entry<Object, Object> entry : properties.entrySet()) {
                String key = entry.getKey().toString();
                if (readKeys.contains(key)) {
                    continue;
                }
                if (key.startsWith("-D")) {
                    key = key.substring(2);
                    String value = entry.getValue().toString();
                    System.setProperty(key, value);
                    newProperties2.put(key, value);
                    readKeys.add(key);
                } else {
                    log.warn("unknown key in " + this.configFileName + ": " + key);
                }
            }
            this.extraProperties = newProperties2;
        } catch (InvalidPropertiesFormatException e) {
            throw new IOException("config file format error.", e);
        }


        File resourceFile = new File(getResourceFileName());
        if (resourceFile.exists()) {
            ResourceBundle res = null;
            FileReader reader = new FileReader(resourceFile);
            res = new PropertyResourceBundle(reader);
            if (res != null) {
                this.resource = res;
            }
        } else {
            log.warn("resource file not found: " + getResourceFileName());
        }
    }

    public void dump() throws IOException {
        Properties props = new Properties();
        for (Entry<Object, Object> entry : this.vossConfigProperties.entrySet()) {
            String key = entry.getKey().toString();
            props.put(key, entry.getValue());
        }
        props.putAll(extraProperties);
        FileOutputStream fos = null;
        try {
            File file = new File(this.configFileName + ".dumped");
            fos = new FileOutputStream(file);
            String comment = "dumped at " + df.format(new Date());
            if (this.configFileName.endsWith(CONFIG_FILE_TXT)) {
                props.store(fos, comment);
            } else if (this.configFileName.endsWith(CONFIG_FILE_XML)) {
                props.storeToXML(fos, comment);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public final String getRootDirectory() {
        return this.vossRootDirectory;
    }

    public final String getConfigDirectory() {
        return this.configurationDirectory;
    }

    public final String getConfigFileName() {
        return this.configFileName;
    }

    public final long getTimestamp() {
        return this.timestamp;
    }

    private final Properties getProperties() throws InvalidPropertiesFormatException, IOException {
        FileInputStream fis = null;
        Properties prop = new Properties();
        try {
            File xml = new File(this.configurationDirectory, CONFIG_FILE_XML);
            if (xml.exists()) {
                log.info("using config: " + xml.getAbsolutePath());
                fis = new FileInputStream(xml);
                prop.loadFromXML(fis);
                this.configFileName = xml.getAbsolutePath();
                this.timestamp = System.currentTimeMillis();
                return prop;
            }
            File txt = new File(this.configurationDirectory, CONFIG_FILE_TXT);
            if (txt.exists()) {
                log.info("using config: " + txt.getAbsolutePath());
                fis = new FileInputStream(txt);
                prop.load(fis);
                this.configFileName = txt.getAbsolutePath();
                this.timestamp = System.currentTimeMillis();
                return prop;
            }
        } finally {
            fis.close();
        }
        throw new IllegalStateException("no config file found on ["
                + this.configurationDirectory + "].");
    }

    public final ResourceBundle getResource() {
        return resource;
    }

    public final String getResourceFileName() {
        return this.vossRootDirectory + this.vossConfigProperties.get(HTTP_SERVER_RESOURCE_FILENAME);
    }

    public final String getRmiRegistryHost() {
        return (String) this.vossConfigProperties.get(RMI_REGISTRY_SERVER_HOST);
    }

    public final int getRmiRegistryPort() {
        String value = (String) this.vossConfigProperties.get(RMI_REGISTRY_SERVER_PORT);
        return Integer.parseInt(value);
    }

    public final int getExceptionServicePort() {
        String value = (String) this.vossConfigProperties.get(EXCEPTION_SERVER_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getSessionServicePort() {
        String value = (String) this.vossConfigProperties.get(SESSION_SERVICE_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getNodeInfoServicePort() {
        String value = (String) this.vossConfigProperties.get(NODEINFO_SERVICE_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getNodeInfoSessionPort() {
        String value = (String) this.vossConfigProperties.get(NODEINFO_SESSION_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getAgentManagerServicePort() {
        String value = (String) this.vossConfigProperties.get(AGENTMANAGER_SERVICE_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getAgentManagerSessionPort() {
        String value = (String) this.vossConfigProperties.get(AGENTMANAGER_SESSION_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getSchedulerServicePort() {
        String value = (String) this.vossConfigProperties.get(SCHEDULER_SERVICE_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getSchedulerSessionPort() {
        String value = (String) this.vossConfigProperties.get(SCHEDULER_SESSION_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getAgentServicePort() {
        String value = (String) this.vossConfigProperties.get(AGENT_SERVICE_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getAgentSessionPort() {
        String value = (String) this.vossConfigProperties.get(AGENT_SESSION_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final List<String> getSupportedSites() {
        List<String> sites = new ArrayList<String>();
        String value = (String) this.vossConfigProperties.get(SUPPORTED_SITES);
        if (value.indexOf(',') == -1) {
            sites.add(value);
        } else {
            String[] names = value.split(",");
            for (String name : names) {
                if (name.trim().length() == 0) {
                    continue;
                }
                sites.add(name.trim());
            }
        }
        return sites;
    }

    public final int getInventoryServicePort() {
        String value = (String) this.vossConfigProperties.get(INVENTORY_SERVICE_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getInventorySessionPort() {
        String value = (String) this.vossConfigProperties.get(INVENTORY_SERVICE_RMI_PORT);
        return Integer.parseInt(value);
    }

    public final int getHttpPort() {
        String value = (String) this.vossConfigProperties.get(HTTP_SERVER_PORT);
        return Integer.parseInt(value);
    }

    public final int getAgentThreads() {
        String value = (String) this.vossConfigProperties.get(AGENT_THREADS);
        return Integer.parseInt(value);
    }

    public final String getAgentSiteName() {
        String value = (String) this.vossConfigProperties.get(AGENT_SITE_NAME);
        return value;
    }

    public final String getServerMode() {
        String value = (String) this.vossConfigProperties.get(SERVER_MODE);
        return value;
    }

    public final boolean isPrivateMode() {
        return getServerMode().toLowerCase().equals("private");
    }

    public final boolean isPublicMode() {
        return !getServerMode().toLowerCase().equals("private");
    }

    public final String getHttpLayoutFileName() {
        String fileName = (String) this.vossConfigProperties.get(HTTP_SERVER_LAYOUT);
        if (fileName == null) {
            return null;
        }
        return this.vossRootDirectory + fileName;
    }

    public final String getWebRootDirectory() {
        return this.vossRootDirectory + (String) this.vossConfigProperties.get(HTTP_SERVER_LOCAL_WEB_ROOT);
    }

    public final String getWarRootDirectory() {
        return this.vossRootDirectory + (String) this.vossConfigProperties.get(HTTP_SERVER_LOCAL_WAR_ROOT);
    }

    public final String getLibRootDirectory() {
        return this.vossRootDirectory + (String) this.vossConfigProperties.get(HTTP_SERVER_LOCAL_LIB_ROOT);
    }

    public final String getHttpServerDefaultHeaderInclude() {
        return getWebRootDirectory() + (String) this.vossConfigProperties.get(HTTP_SERVER_DEFAULT_HEADER_INCLUDE);
    }

    public final String getHttpServerDefaultFooterInclude() {
        return getWebRootDirectory() + (String) this.vossConfigProperties.get(HTTP_SERVER_DEFAULT_FOOTER_INCLUDE);
    }

    public final String getHttpServerDefaultCss() {
        return (String) this.vossConfigProperties.get(HTTP_SERVER_DEFAULT_CSS);
    }

    public String getDbBackupDirectory() {
        String dir = this.vossRootDirectory + (String) this.vossConfigProperties.get(DB_BACKUP_DIR);
        if (!dir.endsWith("/")) {
            dir = dir + "/";
        }
        return dir;
    }

    public boolean useAAA() {
        return Boolean.parseBoolean((String) this.vossConfigProperties.get(AAA_SERVICE_USE));
    }

    public String getAAAUrl() {
        return (String) this.vossConfigProperties.get(AAA_SERVICE_URL);
    }

    public int getAAALogoutPort() {
        String s = (String) this.vossConfigProperties.get(AAA_SERVICE_LOGOUT_NOTIFICATION_PORT);
        return Integer.parseInt(s);
    }

    public int getAAARequestTimeout() {
        String s = (String) this.vossConfigProperties.get(AAA_REQUEST_TIMEOUT_SEC);
        return Integer.parseInt(s) * 1000;
    }


    public String getInventoryBaseDirectory() {
        return this.vossRootDirectory + (String) this.vossConfigProperties.get(INVENTORY_BASE_DIR);
    }

    public String getInventoryFilesDirectory() {
        String dir = this.getInventoryBaseDirectory() + (String) this.vossConfigProperties.get(FILE_STORE_DIR);
        if (dir.endsWith("/")) {
            return dir;
        }
        return dir + "/";
    }

    public String getSnapshotsDirectory() {
        String dir = this.getInventoryBaseDirectory() + (String) this.vossConfigProperties.get(SNAPSHOT_STORE_DIR);
        if (dir.endsWith("/")) {
            return dir;
        }
        return dir + "/";
    }

    public String getLinkConfigurationFilePath() {
        return this.getInventoryBaseDirectory() + (String) this.vossConfigProperties.get(LINKS_CONFIG);
    }

    public int getFileGenerations() {
        return Integer.parseInt((String) this.vossConfigProperties.get(FILE_GENERATION));
    }

}