package voss.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Pattern;

public class NodeInfo implements Serializable {
    private static final long serialVersionUID = 3L;
    private static final Pattern SITE_ALLOWED_PATTERN = Pattern.compile("[A-Z0-9_]+");
    public static final String DEFAULT = "DEFAULT";
    private String nodeIdentifier;
    private String nodeAlias;
    private String userAccount;
    private String userPassword;
    private String adminAccount;
    private String adminPassword;
    private String communityStringRO;
    private String communityStringRW;
    private String site;
    private String note;
    private int cpuLoadLimit = -1;
    private int memoryLoadLimit = -1;
    private int pingIntervalMilliSec = -1;
    private int walkIntervalMilliSec = -1;
    private int snmpTimeoutSec = -1;
    private int snmpRetryTimes = -1;
    private int consoleIntervalMilliSec = -1;
    private int consoleTimeoutSec = -1;
    private int moreIntervalMilliSec = -1;

    private String deviceName_memo;
    private String deviceType_memo;
    private String osType_memo;
    private String osVersion_memo;

    private NodeIdentifierType nodeIdentifierType = NodeIdentifierType.MANAGEMENT_IP_ADDRESS;
    private List<InetAddress> addresses = new ArrayList<InetAddress>();
    private List<ProtocolPort> protocolPorts = new ArrayList<ProtocolPort>();

    private final Map<String, String> options = new HashMap<String, String>();

    private transient InetAddress effectiveAddress = null;
    private transient ProtocolPort effectiveSnmpProtocol = null;
    private transient ProtocolPort effectiveConsoleProtocol = null;

    public NodeInfo() {
        this.nodeIdentifier = "DEFAULT_NODE_ID";
        this.site = DEFAULT;
    }

    public NodeInfo(final String nodeIdentifier) {
        assert nodeIdentifier != null;
        this.nodeIdentifier = nodeIdentifier;
        this.site = DEFAULT;
    }

    public NodeInfo(final InetAddress addr) {
        assert addr != null;
        this.nodeIdentifier = addr.getHostAddress();
        this.site = DEFAULT;
        addresses.add(addr);
    }

    public String getNodeIdentifier() {
        return this.nodeIdentifier;
    }

    public void setNodeIdentifier(String newIdentifier) {
        assert newIdentifier != null;
        this.nodeIdentifier = newIdentifier;
    }

    public String getSiteName() {
        return site;
    }

    public void setSiteName(final String siteName) {
        if (siteName != null) {
            if (!SITE_ALLOWED_PATTERN.matcher(siteName).matches()) {
                throw new IllegalArgumentException("Invalid site name: " + siteName);
            }
            this.site = siteName.toUpperCase();
        } else {
            this.site = DEFAULT;
        }
    }

    public String getNodeAlias() {
        return this.nodeAlias;
    }

    public void setNodeAlias(String alias) {
        this.nodeAlias = alias;
    }

    public NodeIdentifierType getNodeIdentifierType() {
        return this.nodeIdentifierType;
    }

    public void setNodeIdentifierType(NodeIdentifierType type) {
        this.nodeIdentifierType = type;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(final String account) {
        this.userAccount = account;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(final String pass) {
        this.userPassword = pass;
    }

    public String getAdminAccount() {
        return adminAccount;
    }

    public void setAdminAccount(final String account) {
        this.adminAccount = account;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(final String pass) {
        this.adminPassword = pass;
    }

    public String getCommunityStringRO() {
        return communityStringRO;
    }

    public void setCommunityStringRO(final String community) {
        this.communityStringRO = community;
    }

    public String getCommunityStringRW() {
        return communityStringRW;
    }

    public void setCommunityStringRW(final String community) {
        this.communityStringRW = community;
    }

    public List<InetAddress> getIpAddressList() {
        return addresses;
    }

    public void addIpAddress(final InetAddress addr) {
        this.addresses.add(addr);
    }

    public InetAddress getFirstIpAddress() {
        if (this.addresses.size() > 0) {
            return this.addresses.get(0);
        }
        return null;
    }

    public List<InetAddress> listIpAddress() {
        return Collections.unmodifiableList(this.addresses);
    }

    public void clearIpAddresses() {
        addresses.clear();
    }

    public void addSupportedProtocol(final ProtocolPort protocol) {
        protocolPorts.add(protocol);
    }

    public boolean isSupported(final Protocol protocol) {
        return null != getProtocolPort(protocol);
    }

    public Protocol getPreferredSnmpMethod() {
        if (this.isSupported(Protocol.SNMP_V2C_GETBULK)) {
            return Protocol.SNMP_V2C_GETBULK;
        } else if (this.isSupported(Protocol.SNMP_V2C_GETNEXT)) {
            return Protocol.SNMP_V2C_GETNEXT;
        } else if (this.isSupported(Protocol.SNMP_V1)) {
            return Protocol.SNMP_V1;
        }
        return Protocol.UNKNOWN;
    }

    public Protocol getPreferredConsoleProtocol() {
        if (this.isSupported(Protocol.SSH2)) {
            return Protocol.SSH2;
        } else if (this.isSupported(Protocol.SSH2_PUBLICKEY)) {
            return Protocol.SSH2_PUBLICKEY;
        } else if (this.isSupported(Protocol.SSH2_INTERACTIVE)) {
            return Protocol.SSH2_INTERACTIVE;
        } else if (this.isSupported(Protocol.TELNET)) {
            return Protocol.TELNET;
        }
        return null;
    }

    public ProtocolPort getProtocolPort(final Protocol protocol) {
        for (ProtocolPort pp : protocolPorts) {
            if (pp.getProtocol().equals(protocol)) {
                return pp;
            }
        }
        return null;
    }

    public void clearSupportedProtocolPort() {
        this.protocolPorts.clear();
    }

    public int getCpuLoadLimit() {
        return cpuLoadLimit;
    }

    public void setCpuLoadLimit(int cpuLoadLimit) {
        this.cpuLoadLimit = cpuLoadLimit;
    }

    public int getMemoryLoadLimit() {
        return memoryLoadLimit;
    }

    public void setMemoryLoadLimit(int memoryLoadLimit) {
        this.memoryLoadLimit = memoryLoadLimit;
    }

    public int getPingIntervalMilliSec() {
        return pingIntervalMilliSec;
    }

    public void setPingIntervalMilliSec(int pingInterval) {
        this.pingIntervalMilliSec = pingInterval;
    }

    public int getWalkIntervalMilliSec() {
        return walkIntervalMilliSec;
    }

    public void setWalkIntervalMilliSec(int walkInterval) {
        this.walkIntervalMilliSec = walkInterval;
    }

    public int getSnmpTimeoutSec() {
        return snmpTimeoutSec;
    }

    public void setSnmpTimeoutSec(int snmpTimeoutSec) {
        this.snmpTimeoutSec = snmpTimeoutSec;
    }

    public int getSnmpRetryTimes() {
        return snmpRetryTimes;
    }

    public void setSnmpRetryTimes(int snmpRetry) {
        this.snmpRetryTimes = snmpRetry;
    }

    public int getConsoleIntervalMilliSec() {
        return consoleIntervalMilliSec;
    }

    public void setConsoleIntervalMilliSec(int interval) {
        this.consoleIntervalMilliSec = interval;
    }

    public int getConsoleTimeoutSec() {
        return consoleTimeoutSec;
    }

    public void setConsoleTimeoutSec(int consoleTimeout) {
        this.consoleTimeoutSec = consoleTimeout;
    }

    public int getMoreIntervalMilliSec() {
        return moreIntervalMilliSec;
    }

    public void setMoreIntervalMilliSec(int moreIntervalMilliSec) {
        this.moreIntervalMilliSec = moreIntervalMilliSec;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return this.note;
    }

    public String getDeviceName_memo() {
        return deviceName_memo;
    }

    public void setDeviceName_memo(String deivceName_memo) {
        this.deviceName_memo = deivceName_memo;
    }

    public String getDeviceType_memo() {
        return deviceType_memo;
    }

    public void setDeviceType_memo(String deviceType_memo) {
        this.deviceType_memo = deviceType_memo;
    }

    public String getOsType_memo() {
        return osType_memo;
    }

    public void setOsType_memo(String osType_memo) {
        this.osType_memo = osType_memo;
    }

    public String getOsVersion_memo() {
        return osVersion_memo;
    }

    public void setOsVersion_memo(String osVersion_memo) {
        this.osVersion_memo = osVersion_memo;
    }

    public InetAddress getEffectiveAddress() {
        return effectiveAddress;
    }

    public void setEffectiveAddress(InetAddress effectiveAddress) {
        this.effectiveAddress = effectiveAddress;
    }

    public ProtocolPort getEffectiveSnmpProtocol() {
        return effectiveSnmpProtocol;
    }

    public void setEffectiveSnmpProtocol(ProtocolPort effectiveSnmpProtocol) {
        this.effectiveSnmpProtocol = effectiveSnmpProtocol;
    }

    public ProtocolPort getEffectiveConsoleProtocol() {
        return effectiveConsoleProtocol;
    }

    public void setEffectiveConsoleProtocol(ProtocolPort effectiveConsoleProtocol) {
        this.effectiveConsoleProtocol = effectiveConsoleProtocol;
    }

    public Map<String, String> getOptions() {
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(this.options);
        return result;
    }

    public String getOption(String key) {
        if (key == null) {
            return null;
        }
        return this.options.get(key);
    }

    public boolean hasOption(String key) {
        if (key == null) {
            return false;
        }
        return this.options.containsKey(key);
    }

    public void addOption(String key, String value) {
        if (key == null) {
            return;
        }
        this.options.put(key, value);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NodeInfo)) {
            return false;
        }
        NodeInfo other = (NodeInfo) obj;
        if (this.site == null) {
            return this.nodeIdentifier.equals(other.nodeIdentifier);
        } else {
            return this.nodeIdentifier.equals(other.nodeIdentifier)
                    && this.site.equals(other.site);
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder().append(this.nodeIdentifier).append(site).toString().hashCode();
    }

    public synchronized void updateStatus(NodeInfo nodeinfo) {
        if (!this.equals(nodeinfo)) {
            throw new IllegalArgumentException("not same nodeinfo: this="
                    + this.site + ":" + this.nodeIdentifier
                    + ", other=" + nodeinfo.site + ":" + nodeinfo.nodeIdentifier);
        }
        this.deviceName_memo = nodeinfo.deviceName_memo;
        this.deviceType_memo = nodeinfo.deviceType_memo;
        this.osType_memo = nodeinfo.osType_memo;
        this.osVersion_memo = nodeinfo.osVersion_memo;
    }

    public synchronized void update(NodeInfo nodeinfo) {
        if (!this.equals(nodeinfo)) {
            throw new IllegalArgumentException("not same nodeinfo: this="
                    + this.site + ":" + this.nodeIdentifier
                    + ", other=" + nodeinfo.site + ":" + nodeinfo.nodeIdentifier);
        }
        this.nodeAlias = nodeinfo.nodeAlias;
        this.communityStringRO = nodeinfo.communityStringRO;
        this.communityStringRW = nodeinfo.communityStringRW;
        this.userAccount = nodeinfo.userAccount;
        this.userPassword = nodeinfo.userPassword;
        this.adminAccount = nodeinfo.adminAccount;
        this.adminPassword = nodeinfo.adminPassword;
        this.site = nodeinfo.site.toUpperCase();
        this.addresses.clear();
        this.addresses.addAll(nodeinfo.addresses);
        this.protocolPorts.clear();
        this.protocolPorts.addAll(nodeinfo.protocolPorts);
        this.note = nodeinfo.note;
        this.deviceName_memo = nodeinfo.deviceName_memo;
        this.deviceType_memo = nodeinfo.deviceType_memo;
        this.osType_memo = nodeinfo.osType_memo;
        this.osVersion_memo = nodeinfo.osVersion_memo;
        this.options.clear();
        this.options.putAll(nodeinfo.getOptions());
    }

    @Override
    public String toString() {
        String CRLF = "\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getCanonicalName()).append(":").append(this.hashCode()).append(CRLF);
        sb.append("\t").append("site=").append(this.site).append(CRLF);
        sb.append("\t").append("nodeIdentifier=").append(this.nodeIdentifier).append(CRLF);
        sb.append("\t").append("nodeAlias=").append(this.nodeAlias).append(CRLF);
        sb.append("\t").append("communityStringRO=").append(this.communityStringRO).append(CRLF);
        sb.append("\t").append("communityStringRW=").append(this.communityStringRW).append(CRLF);
        sb.append("\t").append("nodeIdentifierType=").append(this.nodeIdentifierType).append(CRLF);
        sb.append("\t").append("userAccount=").append(this.userAccount).append(CRLF);
        sb.append("\t").append("userPassword=").append(this.userPassword).append(CRLF);
        sb.append("\t").append("adminAccount=").append(this.adminAccount).append(CRLF);
        sb.append("\t").append("adminPassword=").append(this.adminPassword).append(CRLF);
        sb.append("\t").append("addresses:").append(CRLF);
        for (InetAddress inetAddress : this.addresses) {
            sb.append("\t  ").append("inetAddress:").append(inetAddress.getHostAddress()).append(CRLF);
        }
        sb.append("\t").append("protocolPorts:").append(CRLF);
        for (ProtocolPort port : this.protocolPorts) {
            sb.append("\t  ").append("port:").append(port.toString()).append(CRLF);
        }
        sb.append("\t").append("note:").append(this.note).append(CRLF);
        sb.append("\t").append("deviceName_memo:").append(this.deviceName_memo).append(CRLF);
        sb.append("\t").append("deviceType_memo:").append(this.deviceType_memo).append(CRLF);
        sb.append("\t").append("osType_memo:").append(this.osType_memo).append(CRLF);
        sb.append("\t").append("osVersion_memo:").append(this.osVersion_memo).append(CRLF);
        sb.append("\toptions: ");
        if (this.options != null) {
            for (Map.Entry<String, String> entry : this.options.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
        } else {
            sb.append("<null>");
        }
        sb.append(CRLF);
        sb.append("----");
        return sb.toString();
    }

}