package voss.discovery.agent.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.config.VossDiscoveryConfiguration;
import voss.discovery.constant.DiscoveryParameterType;
import voss.discovery.iolib.UnknownTargetException;
import voss.discovery.utils.FileUtils;
import voss.discovery.utils.ListUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;


public class AgentConfiguration {
    public enum Params {
        AGENT_TYPE("STANDALONE"),
        TELNET_CPU_LOAD_5MIN(50),
        TELNET_CPU_LOAD_1MIN(50),
        TELNET_CPU_LOAD(50),
        PING_TIMEOUT_SEC(5),
        SNMP_RETRY(3),
        SNMP_TIMEOUT_SEC(60),
        SNMP_WALK_INTERVAL_MS(0),
        TELNET_COMMAND_INTERVAL(100),
        TELNET_MORE_INTERVAL(100),
        TELNET_TIMEOUT_SEC(30),;

        private final Object defaultValue;

        private Params(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public Object getDefaultValue() {
            return this.defaultValue;
        }
    }

    public enum AgentMode {
        STANDALONE,
        CLIENT,;
    }

    public final static String AGENT_CONFIG_FILENAME
            = DiscoveryParameterType.AGENT_CONFIG.toString() + ".txt";

    private final static Logger log = LoggerFactory.getLogger(AgentConfiguration.class);
    private String configDirectory;
    private String configFileName;

    private static Map<String, Object> configParams = new HashMap<String, Object>();

    private static AgentConfiguration instance = null;

    public synchronized static AgentConfiguration getInstance() {
        if (instance == null) {
            instance = new AgentConfiguration();
        }
        return instance;
    }

    private AgentConfiguration() {
        String dir = System.getProperty(VossDiscoveryConfiguration.VOSS_DIR_KEY);
        if (dir == null) {
            configDirectory = "./config/";
        } else {
            if (!dir.endsWith("/")) {
                dir = dir + "/";
            }
            configDirectory = dir + "config/";
        }
        configFileName = configDirectory + AGENT_CONFIG_FILENAME;
        log.info("configFileName: " + configFileName);

        refresh();
    }

    public synchronized void refresh() {
        try {
            loadDiscoveryParameters();
        } catch (IOException e) {
            log.error("refresh(): failed to update configs.", e);
        } catch (ParseException e) {
            log.error("refresh(): failed to update configs.", e);
        }

    }

    private synchronized void loadDiscoveryParameters() throws IOException, ParseException {
        String configDir = VossDiscoveryConfiguration.getInstance().getConfigDirectory();
        for (DiscoveryParameterType mapType : DiscoveryParameterType.values()) {
            File file = new File(configDir, mapType.toString() + ".txt");
            log.debug("loading " + file.getAbsolutePath());
            if (!file.exists()) {
                log.debug("file not found: " + file.getAbsolutePath());
                continue;
            }
            List<String> lines = FileUtils.loadFile(file);
            setDiscoveryParameter(mapType, lines);
        }
    }

    public synchronized void updateDiscoveryParameter(DiscoveryParameterType mapType,
                                                      List<String> contents) throws IOException, ParseException {
        setDiscoveryParameter(mapType, contents);
        dumpParameterToFile(mapType.toString() + ".txt", contents);
    }

    public synchronized void updateDiscoveryParameterAgentConfig() throws IOException, ParseException {
        setDiscoveryParameter(DiscoveryParameterType.AGENT_CONFIG, null);
    }

    private synchronized void dumpParameterToFile(String fileName, List<String> contents) throws IOException {
        VossDiscoveryConfiguration config = VossDiscoveryConfiguration.getInstance();
        String basedir = config.getConfigDirectory();
        File backupFile = new File(basedir, fileName + "_backup");
        File targetFile = new File(basedir, fileName);
        File updatedFile = new File(basedir, fileName + "_updated");
        if (updatedFile.exists()) {
            boolean r = updatedFile.delete();
            if (!r) {
                throw new IOException("failed to delete old file: "
                        + updatedFile.getAbsolutePath());
            }
        }
        FileUtils.saveFile(contents, updatedFile);
        FileUtils.rename(targetFile, backupFile, true);
        FileUtils.rename(updatedFile, targetFile, false);
    }

    public synchronized AgentMode getAgentMode() {
        Object o = configParams.get(Params.AGENT_TYPE.toString());
        if (o == null) {
            o = Params.AGENT_TYPE.getDefaultValue();
        }
        if (o instanceof AgentMode) {
            return (AgentMode) o;
        }
        if (o instanceof String) {
            try {
                return AgentMode.valueOf((String) o);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("AGENT_TYPE is not known: " + o.toString());
            }
        }
        throw new IllegalStateException("AGENT_TYPE is resolved: " + o.toString());
    }

    public synchronized int getSnmpTimeoutSeconds() {
        Object o = configParams.get(Params.SNMP_TIMEOUT_SEC.toString());
        if (o == null) {
            o = Params.SNMP_TIMEOUT_SEC.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("SNMP_TIMEOUT_SEC is not Integer: " + o.toString());
    }

    public synchronized int getSnmpRetryTimes() {
        Object o = configParams.get(Params.SNMP_RETRY.toString());
        if (o == null) {
            o = Params.SNMP_RETRY.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("SNMP_RETRY is not Integer: " + o.toString());
    }

    public synchronized int getWalkIntervalMilliSeconds() {
        Object o = configParams.get(Params.SNMP_WALK_INTERVAL_MS.toString());
        if (o == null) {
            o = Params.SNMP_WALK_INTERVAL_MS.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("SNMP_WALK_INTERVAL_MS is not Integer: " + o.toString());
    }

    public synchronized int getTelnetTimeoutSeconds() {
        Object o = configParams.get(Params.TELNET_TIMEOUT_SEC.toString());
        if (o == null) {
            o = Params.TELNET_TIMEOUT_SEC.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("TELNET_TIMEOUT_SEC is not Integer: " + o.toString());
    }

    public synchronized int getTelnetCommandIntervalMilliSeconds() {
        Object o = configParams.get(Params.TELNET_COMMAND_INTERVAL.toString());
        if (o == null) {
            o = Params.TELNET_COMMAND_INTERVAL.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("TELNET_COMMAND_INTERVAL is not Long: " + o.toString());
    }

    public synchronized int getMoreIntervalMilliSeconds() {
        Object o = configParams.get(Params.TELNET_MORE_INTERVAL.toString());
        if (o == null) {
            o = Params.TELNET_MORE_INTERVAL.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("TELNET_MORE_INTERVAL is not Long: " + o.toString());
    }

    public synchronized int getPingTimeoutSeconds() {
        Object o = configParams.get(Params.PING_TIMEOUT_SEC.toString());
        if (o == null) {
            o = Params.PING_TIMEOUT_SEC.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("PING_TIMEOUT_SEC is not Integer: " + o.toString());
    }

    public synchronized int getCpuLoadLimitIn5min() {
        Object o = configParams.get(Params.TELNET_CPU_LOAD_5MIN.toString());
        if (o == null) {
            o = Params.TELNET_CPU_LOAD_5MIN.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("TELNET_CPU_LOAD_5MIN is not Integer: " + o.toString());
    }

    public synchronized int getCpuLoadLimitIn1min() {
        Object o = configParams.get(Params.TELNET_CPU_LOAD_1MIN.toString());
        if (o == null) {
            o = Params.TELNET_CPU_LOAD_1MIN.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("TELNET_CPU_LOAD_1MIN is not Integer: " + o.toString());
    }

    public synchronized int getCpuLoadLimitCurrent() {
        Object o = configParams.get(Params.TELNET_CPU_LOAD.toString());
        if (o == null) {
            o = Params.TELNET_CPU_LOAD.getDefaultValue();
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt((String) o);
        }
        throw new IllegalStateException("TELNET_CPU_LOAD is not Integer: " + o.toString());
    }

    private final Map<String, String> DISCOVERY_TYPES = new HashMap<String, String>();
    private final Map<String, String> DEVICE_TYPES = new HashMap<String, String>();
    private final Map<DiscoveryParameterType, Map<String, String>> DISCOVERY_PARAM_MAPS
            = new HashMap<DiscoveryParameterType, Map<String, String>>();

    public synchronized String getAgentType(String sysObjectID) throws UnknownTargetException {
        return getValue(sysObjectID, DISCOVERY_TYPES);
    }

    public synchronized String getDeviceType(String sysObjectID) throws UnknownTargetException {
        return getValue(sysObjectID, DEVICE_TYPES);
    }

    private String getValue(String key, Map<String, String> map) throws UnknownTargetException {
        String value = map.get(key);
        log.debug("getType():strict match; " + value);
        if (value != null) {
            return value;
        }

        for (String def : map.keySet()) {
            if (def.length() < 3) {
                continue;
            }
            if (def.endsWith("*")) {
                String base = def.substring(0, def.length() - 2);
                if (key.startsWith(base)) {
                    value = map.get(def);
                    log.debug("getValue():wild card match; " + value);
                    break;
                }
            }
        }

        if (value == null) {
            throw new UnknownTargetException("discovery class undefined: " +
                    "sysObjectID=" + key, key);
        }
        return value;
    }

    private synchronized Map<String, String> parseMap(List<String> lines) throws ParseException {
        Map<String, String> temp = new HashMap<String, String>();
        List<String> summary = new ArrayList<String>();
        int i = 0;
        for (String line : lines) {
            i++;
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }
            if (line.length() == 0) {
                continue;
            }
            String[] e = line.split("[ \t]+");
            if (e.length < 2) {
                log.warn("illegal line: " + line);
                throw new ParseException("illegal line: " + line, i);
            }
            String e0 = e[0].trim();
            String e1 = e[1].trim();
            temp.put(e0, e1);
            summary.add(e0 + "=" + e1);
        }

        if (temp.size() == 0) {
            throw new ParseException("no contents.", 0);
        } else {
            log.debug("parsed: \r\n" + ListUtil.toContent(ListUtil.headAndTail(summary, 10)));
        }
        return temp;
    }

    private synchronized void parseMap(List<String> lines, Map<String, String>... maps) throws ParseException {
        List<String> summary = new ArrayList<String>();
        int i = 0;
        for (String line : lines) {
            i++;
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }
            if (line.length() == 0) {
                continue;
            }
            String[] e = line.split("[ \t]+");
            if (e.length < 2) {
                log.warn("illegal line: " + line);
                throw new ParseException("illegal line: " + line, i);
            }
            String key = e[0].trim();
            for (int j = 0; j < maps.length; j++) {
                String value = null;
                if (e.length > (j + 1)) {
                    value = e[j + 1].trim();
                }
                maps[j].put(key, value);
                summary.add(key + "=" + value);
            }
        }
        if (summary.size() == 0) {
            throw new ParseException("no contents.", 0);
        } else {
            log.debug("parsed: \r\n" + ListUtil.toContent(ListUtil.headAndTail(summary, 10)));
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void setDiscoveryParameter(DiscoveryParameterType mapType, List<String> contents) throws ParseException {
        log.info("setMap() started: mapType=" + mapType.toString());

        switch (mapType) {
            case AGENT_CONFIG:
                Properties prop = new Properties();
                FileReader reader = null;
                try {
                    reader = new FileReader(configFileName);
                    prop.load(reader);
                    for (Params param : Params.values()) {
                        Object value = prop.getProperty(param.toString());
                        if (value == null) {
                            value = param.getDefaultValue();
                        }
                        configParams.put(param.toString(), value);
                        log.debug("AgentConfig loaded: [" + param.toString() + "]=[" + value + "]");
                    }
                } catch (IOException e) {
                    log.error(e.toString());
                } catch (IllegalArgumentException e) {
                    log.error(e.toString());
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            log.warn(e.toString());
                        }
                    }
                }
                break;
            case AGENT_SELECTOR_LIST:
                Map<String, String> map1 = new HashMap<String, String>();
                Map<String, String> map2 = new HashMap<String, String>();
                parseMap(contents, map1, map2);
                DISCOVERY_TYPES.clear();
                DISCOVERY_TYPES.putAll(map1);
                DEVICE_TYPES.clear();
                DEVICE_TYPES.putAll(map2);
                break;
            case ALCATEL_PRODUCT_LIST:
                Map<String, String> alcatelProductList = parseMap(contents);
                DISCOVERY_PARAM_MAPS.put(mapType, alcatelProductList);
                break;
            case CISCO_ENTITY_MAP:
                Map<String, String> ciscoEntityMap = parseMap(contents);
                DISCOVERY_PARAM_MAPS.put(mapType, ciscoEntityMap);
                break;
            case CISCO_PRODUCT_LIST:
                Map<String, String> ciscoProductList = parseMap(contents);
                DISCOVERY_PARAM_MAPS.put(mapType, ciscoProductList);
                break;
            case JUNIPER_ENTITY_MAP:
                Map<String, String> juniperChassisDefMap = parseMap(contents);
                DISCOVERY_PARAM_MAPS.put(mapType, juniperChassisDefMap);
                break;
            case EXTRA_DISCOVERY_FACTORY:
                Map<String, String> extraDiscoveryFactoryMap = parseMap(contents);
                DISCOVERY_PARAM_MAPS.put(mapType, extraDiscoveryFactoryMap);
                break;
            case ADHOC_COMMAND_DEF:
                break;
            default:
                log.error("unknown type: " + mapType.toString());
        }
        log.info("setMap() completed: mapType=" + mapType.toString());
    }

    public synchronized Map<String, String> getDiscoveryParameter(DiscoveryParameterType type) {
        Map<String, String> result = new HashMap<String, String>();
        Map<String, String> map = DISCOVERY_PARAM_MAPS.get(type);
        if (map != null) {
            result.putAll(map);
        }
        return Collections.unmodifiableMap(result);
    }

}