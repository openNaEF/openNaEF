package voss.discovery.iolib.simulation;

import net.snmp.SnmpClient;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.constant.AccessMode;
import voss.discovery.iolib.*;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleClient;
import voss.discovery.iolib.console.ConsoleClientFactory;
import voss.discovery.iolib.console.SimulatedConsoleClientFactory;
import voss.discovery.iolib.netconf.NetConfAccess;
import voss.discovery.iolib.netconf.NetConfClient;
import voss.discovery.iolib.netconf.NetConfClientFactory;
import voss.discovery.iolib.netconf.SimulatedNetConfClientFactory;
import voss.discovery.iolib.snmp.SimulatedSnmpClientFactory;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpAccessImpl;
import voss.discovery.iolib.snmp.SnmpClientFactory;
import voss.model.Device;
import voss.model.NodeInfo;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.io.InvalidClassException;
import java.net.InetAddress;
import java.util.List;

public class SimulatedDeviceAccessFactory implements DeviceAccessFactory {
    private static final Logger log = LoggerFactory.getLogger(SimulatedDeviceAccessFactory.class);
    private final SimulationArchive simulationArchive;
    private final DiscoveryTypeFactory typeFactory;
    private final SnmpClientFactory snmpClientFactory;
    private final ConsoleClientFactory consoleClientFactory;
    private final NetConfClientFactory netconfClientFactory;

    public SimulatedDeviceAccessFactory(SimulationArchive simulationArchive)
            throws IOException {
        this.typeFactory = new DiscoveryTypeFactory();
        this.simulationArchive = simulationArchive;
        this.snmpClientFactory = new SimulatedSnmpClientFactory(simulationArchive);
        this.consoleClientFactory = new SimulatedConsoleClientFactory(simulationArchive);
        this.netconfClientFactory = new SimulatedNetConfClientFactory(simulationArchive);
    }

    public DeviceAccess getDeviceAccess(NodeInfo nodeinfo) throws IOException, AbortedException {
        return getDeviceAccess(nodeinfo, nodeinfo.listIpAddress());
    }

    public DeviceAccess getDeviceAccess(NodeInfo nodeinfo, List<InetAddress> specified)
            throws IOException, AbortedException {
        InetAddress inetAddress = getEffectiveInetAddress(nodeinfo);
        if (inetAddress == null) {
            return null;
        } else if (nodeinfo.getPreferredSnmpMethod() == null) {
            return null;
        }
        ProgressMonitor monitor = new ProgressMonitor();
        ConsoleAccess console = getConsoleAccess(nodeinfo);
        console.setMonitor(monitor);
        NetConfAccess netconf = getNetConfAccess(nodeinfo);
        netconf.setMonitor(monitor);
        SnmpAccess snmp = getSnmpAccess(nodeinfo);
        snmp.setMonitor(monitor);
        SupportedDiscoveryType type = null;
        try {
            type = this.typeFactory.getType(snmp);
        } catch (UnknownTargetException e) {
            throw new IOException("unknown type: " + nodeinfo.getFirstIpAddress() + "->" + e.getSysObjectID());
        }
        DeviceAccess access = new DeviceAccess(nodeinfo, getMode(), monitor, type);
        access.setTargetAddress(inetAddress);
        access.setConsoleAccess(console);
        access.setSnmpAccess(snmp);
        access.setNetConfAccess(netconf);
        try {
            SimulationEntry entry = this.simulationArchive.getSimulationEntry(nodeinfo);
            if (entry != null) {
                Device d = entry.getDevice();
                if (d != null) {
                    log.debug("found archived-bin.");
                    access.addExtInfo(DeviceAccessExtInfoKey.DEVICE_DISCOVERY_RESULT, d);
                }
            }
        } catch (IOException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (InvalidClassException.class.isInstance(rootCause)) {
                log.debug("Incompatible serialized model (serialversionuid mismatch).");
            } else {
                log.debug("Simulation entry restoration failed.", e);
            }
        } catch (Exception e) {
            log.debug("Unexpected restoration failure.", e);
        }
        return access;
    }

    private ConsoleAccess getConsoleAccess(NodeInfo nodeinfo) throws IOException, AbortedException {
        ConsoleClient client = consoleClientFactory.getConsoleClient(nodeinfo, null, null, null);
        ConsoleAccess result = new ConsoleAccess(client);
        return result;
    }

    private NetConfAccess getNetConfAccess(NodeInfo nodeinfo) throws IOException, AbortedException {
        NetConfClient client = netconfClientFactory.getNetConfClient(nodeinfo, null, null, null);
        NetConfAccess result = new NetConfAccess(client);
        return result;
    }

    private SnmpAccess getSnmpAccess(NodeInfo nodeinfo) throws IOException {
        SnmpClient client = snmpClientFactory.createSnmpClient(null, nodeinfo);
        if (client == null) {
            return null;
        }
        SnmpAccess snmpAccess = new SnmpAccessImpl(client);
        return snmpAccess;
    }

    private InetAddress getEffectiveInetAddress(NodeInfo nodeinfo) throws IOException {
        SimulationEntry entry = this.simulationArchive.getSimulationEntry(nodeinfo);
        if (entry == null) {
            return null;
        }
        String targetId = entry.getTargetId();
        log.debug("selected targetId=" + targetId);
        InetAddress inetAddress = InetAddress.getByAddress(VossMiscUtility.getByteFormIpAddress(targetId));
        return inetAddress;
    }

    public AccessMode getMode() {
        return AccessMode.SIMULATED;
    }

}