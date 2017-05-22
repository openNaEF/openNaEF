package voss.discovery.iolib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.constant.AccessMode;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.netconf.NetConfAccess;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.NodeInfo;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class DeviceAccess implements Access {
    private final static Logger log = LoggerFactory.getLogger(DeviceAccess.class);
    private InetAddress inetAddress;
    private ProgressMonitor monitor;
    private final NodeInfo nodeinfo;
    private ConsoleAccess consoleAccess;
    private SnmpAccess snmpAccess;
    private NetConfAccess netconfAccess;
    private final AccessMode isSimulationMode;
    private final SupportedDiscoveryType discoveryType;
    private final Map<String, Serializable> extinfo = new HashMap<String, Serializable>();

    public DeviceAccess(NodeInfo nodeinfo, AccessMode mode, ProgressMonitor monitor,
                        SupportedDiscoveryType discoveryType) {
        assert monitor != null;
        this.nodeinfo = nodeinfo;
        this.monitor = monitor;
        this.isSimulationMode = mode;
        this.discoveryType = discoveryType;
        log.debug("DeviceAccess created.");
    }

    public void setTargetAddress(InetAddress targetAddress) {
        if (targetAddress == null) {
            return;
        }
        this.inetAddress = targetAddress;
    }

    public void setConsoleAccess(ConsoleAccess console) {
        if (console == null) {
            this.consoleAccess = null;
        } else {
            this.consoleAccess = console;
            this.consoleAccess.setMonitor(this.monitor);
        }
    }

    public void setSnmpAccess(SnmpAccess snmp) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmpAccess = snmp;
        this.snmpAccess.setMonitor(this.monitor);
    }

    public void setNetConfAccess(NetConfAccess netconf) {
        if (netconf == null) {
            this.netconfAccess = null;
        } else {
            this.netconfAccess = netconf;
            this.netconfAccess.setMonitor(this.monitor);
        }
    }

    public InetAddress getTargetAddress() {
        return this.inetAddress;
    }

    public ConsoleAccess getConsoleAccess() {
        return this.consoleAccess;
    }

    public SnmpAccess getSnmpAccess() {
        return this.snmpAccess;
    }

    public NetConfAccess getNetConfAccess() {
        return this.netconfAccess;
    }

    public NodeInfo getNodeInfo() {
        return this.nodeinfo;
    }

    public AccessMode getMode() {
        return this.isSimulationMode;
    }

    public ProgressMonitor getMonitor() {
        return this.monitor;
    }

    public SupportedDiscoveryType getDiscoveryType() {
        return discoveryType;
    }

    public void setMonitor(ProgressMonitor monitor) {
        this.monitor = monitor;
        if (this.snmpAccess != null) {
            this.snmpAccess.setMonitor(monitor);
        }
        if (this.consoleAccess != null) {
            this.consoleAccess.setMonitor(monitor);
        }
        if (this.netconfAccess != null) {
            this.netconfAccess.setMonitor(monitor);
        }
    }

    public Serializable getExtInfo(String key) {
        if (key == null) {
            return null;
        }
        return this.extinfo.get(key);
    }

    public void addExtInfo(String key, Serializable obj) {
        if (key == null) {
            return;
        }
        this.extinfo.put(key, obj);
    }

    public void close() throws IOException, ConsoleException {
        if (this.consoleAccess != null) {
            try {
                this.consoleAccess.close();
                log.debug("DeviceAccess#consoleAccess closed.");
            } catch (IOException e) {
                log.debug("DeviceAccess#consoleAccess cannot close: " + e.getMessage(), e);
            }
        }
        if (this.netconfAccess != null) {
            try {
                this.netconfAccess.close();
                log.debug("DeviceAccess#netconfAccess closed.");
            } catch (IOException e) {
                log.debug("DeviceAccess#netconfAccess cannot close: " + e.getMessage(), e);
            }
        }
        if (this.snmpAccess != null) {
            try {
                this.snmpAccess.close();
                log.debug("DeviceAccess#snmpAccess closed.");
            } catch (IOException e) {
                log.debug("DeviceAccess#snmpAccess cannot close: " + e.getMessage(), e);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(inetAddress.getHostAddress()).append("]");
        if (this.snmpAccess != null) {
            sb.append(" snmp");
        }
        if (this.consoleAccess != null) {
            sb.append(" console");
        }
        if (this.netconfAccess != null) {
            sb.append(" netconf");
        }
        sb.append(" nodeinfo=").append(this.nodeinfo.toString());
        return sb.toString();
    }

}