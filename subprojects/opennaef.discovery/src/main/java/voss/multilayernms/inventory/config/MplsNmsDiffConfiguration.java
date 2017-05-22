package voss.multilayernms.inventory.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfiguration;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.model.Protocol;
import voss.multilayernms.inventory.diff.service.RegularExecution;
import voss.multilayernms.inventory.diff.util.ConfigUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MplsNmsDiffConfiguration extends ServiceConfiguration {
    public static final String NAME = "MplsNmsDiffConfiguration";
    public static final String FILE_NAME = "MplsNmsDiffConfiguration.xml";
    public static final String DESCRIPTION = "MPLS-NMS specific diff configuration.";

    public static final String INVENTORY_RMI_SERVICE_PORT = "inventory-rmi-service-port";

    public static final String KEY_DIFFSET_STORE_DIR = "diffset-store-directory";

    public static final String ATTR_URL = ".url";
    public static final String ATTR_DISPLAY_NAME = ".display-name";
    public static final String ATTR_AUTO_APPLY = ".auto-apply";
    public static final String ATTR_CONNECTION_STRING = ".connection-string";
    public static final String ATTR_REGULAR_EXECUTION = ".regular-execution";
    public static final String ATTR_START_TIME = ".start-time";
    public static final String ATTR_INTERVAL = ".interval";

    public static final String ATTR_SCHEDULER_BASE_TIME = ".scheduler-base-time";
    public static final String ATTR_SCHEDULER_INTERVAL_MINUTES = ".scheduler-interval-minutes";
    public static final String ATTR_PING_NUMBER = ".ping-number";
    public static final String ATTR_PING_SIZE = ".ping-size";
    public static final String ATTR_PING_TIMEOUT = ".ping-timeout";

    public static final String KEY_DISCOVERY_BASE = "discovery";
    public static final String KEY_DISCOVERY_DISPLAY_NAME = KEY_DISCOVERY_BASE + ATTR_DISPLAY_NAME;
    public static final String KEY_DISCOVERY_MAX_THREAD_SIZE = KEY_DISCOVERY_BASE + ".max-thread-size";
    public static final String KEY_DISCOVERY_SNMP_TIMEOUT_SEC = KEY_DISCOVERY_BASE + ".snmp-timeout-sec";
    public static final String KEY_DISCOVERY_SNMP_RETRY_TIMES = KEY_DISCOVERY_BASE + ".snmp-retry-times";
    public static final String KEY_DISCOVERY_TELNET_TIMEOUT_SEC = KEY_DISCOVERY_BASE + ".telnet-timeout-sec";

    public static final String KEY_DISCOVERY_SCHEDULER_BASE_TIME =
            KEY_DISCOVERY_BASE +
                    ATTR_REGULAR_EXECUTION +
                    ATTR_START_TIME;
    public static final String KEY_DISCOVERY_SCHEDULER_INTERVAL_MINUTES =
            KEY_DISCOVERY_BASE +
                    ATTR_REGULAR_EXECUTION +
                    ATTR_INTERVAL;


    public static final String KEY_TYPE_ATTR = "[@type]";
    public static final String KEY_VENDOR_ATTR = "[@vendor]";

    public static final String KEY_FROM_ATTR = "[@from]";
    public static final String KEY_TO_ATTR = "[@to]";

    public static final String KEY_DISCOVERY_TYPE_MAPPING_BASE = "discovery-type-mapping.mapping";
    public static final String KEY_DISCOVERY_TYPE_MAPPING_KEY = KEY_DISCOVERY_TYPE_MAPPING_BASE + KEY_FROM_ATTR;

    public static final String KEY_VALUE_MAPPING_BASE = "value-mapping.mapping";
    public static final String KEY_VALUE_MAPPING_KEY = KEY_VALUE_MAPPING_BASE + KEY_FROM_ATTR;

    public static final String KEY_DEFAULT_SNMP_COMMUNITY = "default-snmp-community-string";
    public static final String KEY_DEFAULT_SNMP_METHOD = "default-snmp-method";

    private static MplsNmsDiffConfiguration instance = null;

    public synchronized static MplsNmsDiffConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new MplsNmsDiffConfiguration();
            instance.reloadConfiguration();
        }
        return instance;
    }

    private int inventoryRmiServicePort = 4649;
    private String discoveryDisplayName = "NETWORK";
    private int discoveryMaxThreadSize = 5;
    private int discoverySnmpTimeoutSeconds = 30;
    private int discoverySnmpRetryTimes = 3;
    private int discoveryTelnetTimeoutSeconds = 30;
    private Date discoverySchedulerBaseTime = new Date();
    private int discoverySchedulerIntervalMinutes = 1440;

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private final SimpleDateFormat df2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private String diffsetStoreDirectoryName = "./diffset";

    private String eventManagerURI = null;
    private String httpAgentURI = null;

    private Date rttSchedulerBaseTime = new Date();
    private int rttSchedulerIntervalMin = 1440;
    private int rttPingNumber = 5;
    private int rttPingSize = 60;
    private int rttPingTimeout = 5;

    private Date operStatusSchedulerBaseTime = new Date();
    private int operStatusSchedulerIntervalMin = 1440;

    private String defaultSnmpCommunityString = "public";
    private String defaultSnmpMethod = Protocol.SNMP_V2C_GETNEXT.caption;

    private List<FromToValueHolder> discoveryTypeMappings = new ArrayList<FromToValueHolder>();
    private List<FromToValueHolder> valueMappings = new ArrayList<FromToValueHolder>();

    private XMLConfiguration config = new XMLConfiguration();

    private MplsNmsDiffConfiguration() throws IOException {
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

            String _discoveryDisplayName = config.getString(KEY_DISCOVERY_DISPLAY_NAME);
            String __discoveryMaxThreadSize = config.getString(KEY_DISCOVERY_MAX_THREAD_SIZE);
            int _discoveryMaxThreadSize = Integer.parseInt(__discoveryMaxThreadSize);
            String __discoverySnmpTimeoutSeconds = config.getString(KEY_DISCOVERY_SNMP_TIMEOUT_SEC);
            int _discoverySnmpTimeoutSeconds = Integer.parseInt(__discoverySnmpTimeoutSeconds);
            String __discoverySnmpRetryTimes = config.getString(KEY_DISCOVERY_SNMP_RETRY_TIMES);
            int _discoverySnmpRetryTimes = Integer.parseInt(__discoverySnmpRetryTimes);
            String __discoveryTelnetTimeoutSeconds = config.getString(KEY_DISCOVERY_TELNET_TIMEOUT_SEC);
            int _discoveryTelnetTimeoutSeconds = Integer.parseInt(__discoveryTelnetTimeoutSeconds);
            String __discoverySchedulerBaseTime = config.getString(KEY_DISCOVERY_SCHEDULER_BASE_TIME);
            Date _discoverySchedulerBaseTime = df.parse(__discoverySchedulerBaseTime);
            String __discoverySchedulerIntervalMinutes = config.getString(KEY_DISCOVERY_SCHEDULER_INTERVAL_MINUTES);
            int _discoverySchedulerIntervalMinutes = Integer.parseInt(__discoverySchedulerIntervalMinutes);

            String _diffsetStoreDirName = config.getString(KEY_DIFFSET_STORE_DIR);
            File dir = new File(_diffsetStoreDirName);
            if (!dir.exists()) {
                boolean result = dir.mkdirs();
                if (!result) {
                    throw new IOException("failed to create diffset directory:" + _diffsetStoreDirName);
                }
            }

            List<FromToValueHolder> _discoveryTypeMappings = new ArrayList<FromToValueHolder>();
            List<?> __discoveryTypeMappings = config.getList(KEY_DISCOVERY_TYPE_MAPPING_KEY);
            for (int i = 0; i < __discoveryTypeMappings.size(); i++) {
                String key = KEY_DISCOVERY_TYPE_MAPPING_BASE + "(" + i + ")";
                String _from = config.getString(key + KEY_FROM_ATTR);
                String _to = config.getString(key + KEY_TO_ATTR);
                FromToValueHolder holder = new FromToValueHolder();
                holder.from = _from;
                holder.to = _to;
                _discoveryTypeMappings.add(holder);
            }

            List<FromToValueHolder> _valueMappings = new ArrayList<FromToValueHolder>();
            List<?> __valueMappings = config.getList(KEY_VALUE_MAPPING_KEY);
            for (int i = 0; i < __valueMappings.size(); i++) {
                String key = KEY_VALUE_MAPPING_BASE + "(" + i + ")";
                String _from = config.getString(key + KEY_FROM_ATTR);
                String _to = config.getString(key + KEY_TO_ATTR);
                FromToValueHolder holder = new FromToValueHolder();
                holder.from = _from;
                holder.to = _to;
                _valueMappings.add(holder);
            }

            String _defaultSnmpCommunityString = config.getString(KEY_DEFAULT_SNMP_COMMUNITY);
            String _defaultSnmpMethod = config.getString(KEY_DEFAULT_SNMP_METHOD);

            this.diffsetStoreDirectoryName = _diffsetStoreDirName;
            this.discoveryDisplayName = _discoveryDisplayName;
            this.discoveryMaxThreadSize = _discoveryMaxThreadSize;
            this.discoverySnmpTimeoutSeconds = _discoverySnmpTimeoutSeconds;
            this.discoverySnmpRetryTimes = _discoverySnmpRetryTimes;
            this.discoveryTelnetTimeoutSeconds = _discoveryTelnetTimeoutSeconds;
            this.discoverySchedulerBaseTime = _discoverySchedulerBaseTime;
            this.discoverySchedulerIntervalMinutes = _discoverySchedulerIntervalMinutes;
            setDiscoveryTypeMappings(_discoveryTypeMappings);
            setValueMappings(_valueMappings);
            this.defaultSnmpCommunityString = _defaultSnmpCommunityString;
            this.defaultSnmpMethod = _defaultSnmpMethod;

            log().info("diffsetStoreDirectoryName" + getDiffsetStoreDirectoryName());
            log().info("discoveryDisplayName" + getDiscoveryDisplayName());
            log().info("discoverySchedulerIntervalMinutes" + getDiscoverySchedulerIntervalMinutes());
            log().info("discoverySchedulerBaseTime" + getDiscoverySchedulerBaseTime());
            log().info("discoveryMaxThreadSize=" + getDiscoveryMaxThreadSize());
            log().info("discoverySnmpTimeoutSeconds=" + getDiscoverySnmpTimeoutSeconds());
            log().info("discoverySnmpRetryTimes=" + getDiscoverySnmpRetryTimes());
            log().info("discoveryTelnetTimeoutSeconds=" + getDiscoveryTelnetTimeoutSeconds());
            log().info("diffsetStoreDirectoryName=" + getDiffsetStoreDirectoryName());
            log().info("defaultSnmpCommunityString=" + getDefaultSnmpCommunityString());
            log().info("defaultSnmpMethod=" + getDefaultSnmpMethod());
            log().info("reload finished.");
        } catch (ConfigurationException e) {
            log().error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + FILE_NAME);
        } catch (ParseException e) {
            log().error(e.getMessage(), e);
            throw new IOException("failed to reload config: " + FILE_NAME);
        }
    }

    public synchronized void saveConfiguration() {
        try {
            XMLConfiguration config = new XMLConfiguration();

            config.addProperty(INVENTORY_RMI_SERVICE_PORT, this.inventoryRmiServicePort);

            config.addProperty(KEY_DIFFSET_STORE_DIR, this.diffsetStoreDirectoryName);
            config.addProperty(ConfigUtil.KEY_DEBUG_STORE_DIR, ConfigUtil.getInstance().getDebugDumpDir());

            config.addProperty(KEY_DISCOVERY_DISPLAY_NAME, this.discoveryDisplayName);
            config.addProperty(KEY_DISCOVERY_MAX_THREAD_SIZE, this.discoveryMaxThreadSize);
            config.addProperty(KEY_DISCOVERY_SNMP_TIMEOUT_SEC, this.discoverySnmpTimeoutSeconds);
            config.addProperty(KEY_DISCOVERY_SNMP_RETRY_TIMES, this.discoverySnmpRetryTimes);
            config.addProperty(KEY_DISCOVERY_TELNET_TIMEOUT_SEC, this.discoveryTelnetTimeoutSeconds);
            config.addProperty(KEY_DISCOVERY_SCHEDULER_BASE_TIME, df2.format(this.discoverySchedulerBaseTime));
            config.addProperty(KEY_DISCOVERY_SCHEDULER_INTERVAL_MINUTES, this.discoverySchedulerIntervalMinutes);

            config.addProperty(ConfigUtil.KEY_DEBUG_FLAG, ConfigUtil.getInstance().isDebugFlagOn());

            int j = 0;
            for (FromToValueHolder fromToValueHolder : discoveryTypeMappings) {
                String key = KEY_DISCOVERY_TYPE_MAPPING_BASE + "(" + j + ")";
                config.setProperty(key + KEY_FROM_ATTR, fromToValueHolder.from);
                config.setProperty(key + KEY_TO_ATTR, fromToValueHolder.to);
                j++;
            }

            int k = 0;
            for (FromToValueHolder fromToValueHolder : valueMappings) {
                String key = KEY_VALUE_MAPPING_BASE + "(" + k + ")";
                config.setProperty(key + KEY_FROM_ATTR, fromToValueHolder.from);
                config.setProperty(key + KEY_TO_ATTR, fromToValueHolder.to);
                k++;
            }

            config.setProperty(KEY_DEFAULT_SNMP_COMMUNITY, this.defaultSnmpCommunityString);
            config.setProperty(KEY_DEFAULT_SNMP_METHOD, this.defaultSnmpMethod);

            FileWriter writer = new FileWriter(getConfigFile());
            config.save(writer);
        } catch (ConfigurationException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        } catch (IOException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @Override
    protected synchronized void publishServices() throws RemoteException, IOException, InventoryException, ExternalServiceException {
        log().info("called publishServices");
        File diffsetStoreDir = new File(this.diffsetStoreDirectoryName);
        if (!diffsetStoreDir.exists()) {
            boolean result = diffsetStoreDir.mkdirs();
            if (!result) {
                throw new IOException("failed to create diffset directory:" + this.diffsetStoreDirectoryName);
            } else {
                log().info("diffset directory created: " + this.diffsetStoreDirectoryName);
            }
        }
        try {
            ConfigUtil.getInstance().reload();
            RegularExecution.getInstance().reScheduleAll();
            log().info("rescheduling diff-service.");
        } catch (Exception e) {
            log().error("failed to (re)publish diff-service.", e);
        }
        log().info("completed publishServices");
    }

    public synchronized String getDiffsetStoreDirectoryName() {
        return diffsetStoreDirectoryName;
    }

    public synchronized void setDiffsetStoreDirectoryName(String diffsetStoreDirName) {
        this.diffsetStoreDirectoryName = diffsetStoreDirName;
    }

    public synchronized File getDiffsetStoreDirectory() throws IOException {
        if (this.diffsetStoreDirectoryName == null) {
            throw new IOException("config.diffsetStoreDirectoryName is null.");
        }
        File dir = new File(this.diffsetStoreDirectoryName);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                throw new IOException("config.config.diffsetStoreDirectoryName["
                        + this.diffsetStoreDirectoryName + "] creation failed.");
            }
        } else if (!dir.isDirectory()) {
            throw new IOException("config.config.diffsetStoreDirectoryName["
                    + this.diffsetStoreDirectoryName + "] is not directory.");
        }
        return dir;
    }

    public synchronized String getDiscoveryDisplayName() {
        return this.discoveryDisplayName;
    }

    public synchronized int getDiscoveryMaxThreadSize() {
        return discoveryMaxThreadSize;
    }

    public synchronized void setDiscoveryMaxThreadSize(int discoveryMaxThreadSize) {
        this.discoveryMaxThreadSize = discoveryMaxThreadSize;
    }

    public synchronized int getDiscoverySnmpTimeoutSeconds() {
        return discoverySnmpTimeoutSeconds;
    }

    public synchronized void setDiscoverySnmpTimeoutSeconds(int discoverySnmpTimeoutSeconds) {
        this.discoverySnmpTimeoutSeconds = discoverySnmpTimeoutSeconds;
    }

    public synchronized int getDiscoverySnmpRetryTimes() {
        return discoverySnmpRetryTimes;
    }

    public synchronized void setDiscoverySnmpRetryTimes(int discoverySnmpRetryTimes) {
        this.discoverySnmpRetryTimes = discoverySnmpRetryTimes;
    }

    public synchronized int getDiscoveryTelnetTimeoutSeconds() {
        return discoveryTelnetTimeoutSeconds;
    }

    public synchronized void setDiscoveryTelnetTimeoutSeconds(int discoveryTelnetTimeoutSeconds) {
        this.discoveryTelnetTimeoutSeconds = discoveryTelnetTimeoutSeconds;
    }

    public synchronized Date getDiscoverySchedulerBaseTime() {
        return discoverySchedulerBaseTime;
    }

    public synchronized void setDiscoverySchedulerBaseTime(Date discoverySchedulerBaseTime) {
        this.discoverySchedulerBaseTime = discoverySchedulerBaseTime;
    }

    public synchronized int getDiscoverySchedulerIntervalMinutes() {
        return discoverySchedulerIntervalMinutes;
    }

    public synchronized void setDiscoverySchedulerIntervalMinutes(
            int discoverySchedulerIntervalMinutes) {
        this.discoverySchedulerIntervalMinutes = discoverySchedulerIntervalMinutes;
    }

    public synchronized String getTypeNameFromDiscoveryType(String typeName) {
        for (FromToValueHolder value : this.discoveryTypeMappings) {
            if (value.from.equals(typeName)) {
                return value.to;
            }
        }
        return null;
    }

    public synchronized void setDiscoveryTypeMappings(List<FromToValueHolder> holders) {
        this.discoveryTypeMappings.clear();
        this.discoveryTypeMappings.addAll(holders);
    }

    public synchronized String getConvertedValue(String fromValue) {
        for (FromToValueHolder value : this.valueMappings) {
            if (value.from.equals(fromValue)) {
                return value.to;
            }
        }
        return null;
    }

    public synchronized void setValueMappings(List<FromToValueHolder> holders) {
        this.valueMappings.clear();
        this.valueMappings.addAll(holders);
    }

    public synchronized String getEventManagerURI() {
        return eventManagerURI;
    }

    public synchronized void setEventManagerURI(String eventManagerURI) {
        this.eventManagerURI = eventManagerURI;
    }

    public synchronized String getHttpAgentURI() {
        return httpAgentURI;
    }

    public synchronized void setHttpAgentURI(String httpAgentURI) {
        this.httpAgentURI = httpAgentURI;
    }

    public synchronized int getRttPingTimeout() {
        return rttPingTimeout;
    }

    public synchronized void setRttPingTimeout(int rttPingTimeout) {
        this.rttPingTimeout = rttPingTimeout;
    }

    public synchronized int getRttPingNumber() {
        return rttPingNumber;
    }

    public synchronized void setRttPingNumber(int rttPingNumber) {
        this.rttPingNumber = rttPingNumber;
    }

    public synchronized int getRttPingSize() {
        return rttPingSize;
    }

    public synchronized void setRttPingSize(int rttPingSize) {
        this.rttPingSize = rttPingSize;
    }

    public synchronized Date getRttSchedulerBaseTime() {
        return rttSchedulerBaseTime;
    }

    public synchronized void setRttSchedulerBaseTime(Date rttSchedulerBaseTime) {
        this.rttSchedulerBaseTime = rttSchedulerBaseTime;
    }

    public synchronized int getRttSchedulerIntervalMin() {
        return rttSchedulerIntervalMin;
    }

    public synchronized void setRttSchedulerIntervalMin(int rttSchedulerIntervalMin) {
        this.rttSchedulerIntervalMin = rttSchedulerIntervalMin;
    }

    public synchronized Date getOperStatusSchedulerBaseTime() {
        return operStatusSchedulerBaseTime;
    }

    public synchronized void setOperStatusSchedulerBaseTime(Date operStatusSchedulerBaseTime) {
        this.operStatusSchedulerBaseTime = operStatusSchedulerBaseTime;
    }

    public synchronized int getOperStatusSchedulerIntervalMin() {
        return operStatusSchedulerIntervalMin;
    }

    public synchronized void setOperStatusSchedulerIntervalMin(int operStatusSchedulerIntervalMin) {
        this.operStatusSchedulerIntervalMin = operStatusSchedulerIntervalMin;
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

    private static Logger log() {
        return LoggerFactory.getLogger(MplsNmsDiffConfiguration.class);
    }

    public static class FromToValueHolder implements Serializable {
        private static final long serialVersionUID = 1L;
        public String from;
        public String to;
    }

    public synchronized void setDiscoveryDisplayName(String discoveryDisplayName) {
        this.discoveryDisplayName = discoveryDisplayName;
    }

    public synchronized List<FromToValueHolder> getDiscoveryTypeMappings() {
        return discoveryTypeMappings;
    }

    public synchronized List<FromToValueHolder> getValueMappings() {
        return valueMappings;
    }


}