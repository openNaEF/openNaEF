package voss.discovery.agent.cisco;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.mib.*;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.IntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;
import voss.model.value.PortSpeedValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.integerKeyCreator;
import static voss.discovery.iolib.snmp.SnmpHelper.stringEntryBuilder;

public class CiscoCatalyst3750Discovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private static final Logger log = LoggerFactory.getLogger(CiscoCatalyst3750Discovery.class);
    private final ConsoleCommand show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
    private final ConsoleCommand show_interface = new ConsoleCommand(new GlobalMode(), "show interface");
    private final ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");

    private final CiscoStackMibImpl ciscoStackMib;
    private final CiscoImageMib ciscoImageMib;
    private String textConfiguration = null;
    private ConfigurationStructure config = null;
    protected final SnmpAccess snmp;
    protected final VlanDevice device;
    protected final InterfaceMibImpl ifmib;

    public CiscoCatalyst3750Discovery(DeviceAccess access) throws IOException, AbortedException {
        super(access);
        this.snmp = access.getSnmpAccess();
        this.device = new GenericEthernetSwitch();
        this.ifmib = new InterfaceMibImpl(this.getDeviceAccess().getSnmpAccess());
        this.ciscoStackMib = new CiscoStackMibImpl(snmp, device);
        this.ciscoImageMib = new CiscoImageMib(snmp);
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        device.setIpAddress(snmp.getSnmpAgentAddress().getAddress().getHostAddress());
        device.setCommunityRO(snmp.getCommunityString());
        device.setDeviceName(Mib2Impl.getSysName(snmp));
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setVendorName(Constants.VENDOR_CISCO);
        device.setDescription(Mib2Impl.getSysDescr(snmp));
        device.setContactInfo(Mib2Impl.getSysContact(snmp));
        device.setLocation(Mib2Impl.getSysLocation(snmp));
        device.setIpAddresses(Mib2Impl.getIpAddresses(snmp).toArray(new String[0]));
        device.setIpAddress(snmp.getSnmpAgentAddress().getAddress().getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(snmp).longValue()));

        ciscoStackMib.setModelTypeName(device);
        device.setOsTypeName(OSType.IOS.caption);
        ciscoImageMib.setOsVersion(device);
        device.setBasePhysicalAddress(Mib2Impl.getSystemMacAddress(snmp));
        ciscoStackMib.setGatewayAddress(device);
        ciscoStackMib.setSnmpTrapReceiverAddress(device);
        ciscoStackMib.setSyslogServerAddress(device);

        try {
            ciscoStackMib.setChassisSerial(device);
        } catch (IOException e) {
            log.warn("cannot get chassis serial number. (" + device.getDeviceName() + ")");
        }
        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        boolean isSpeedDuplexSourceConfig = getDeviceAccess().getConsoleAccess() != null;
        ciscoStackMib.createSlotAndModule(device);
        ciscoStackMib.createPhysicalPorts(device, isSpeedDuplexSourceConfig);
        getSpecialFa0();

        InterfaceMibImpl ifmib = new InterfaceMibImpl(snmp);
        CiscoPagpMib pagpMib = new CiscoPagpMib(this.snmp);
        pagpMib.createAggregationGroup(device);
        for (EthernetPortsAggregator aggregator : device.getEthernetPortsAggregators()) {
            EthernetPort[] ports = aggregator.getPhysicalPorts();
            if (ports.length == 0) {
                continue;
            }

            int ifindex = ifmib.getIfStackStatusHigherValue(ports[0].getIfIndex());
            if (ifindex > 0) {
                aggregator.initIfIndex(ifindex);
            }

            int channelId = ciscoStackMib.getCrossIndex(ports[0]);
            aggregator.initAggregationGroupId(channelId);
        }

        ifmib.supplementAggregationInterface(device, "aggregated interface");
        ifmib.supplementAggregationInterface(device, "Port-channel");

        Map<IntegerKey, StringSnmpEntry> ifNames =
                SnmpUtil.getWalkResult(snmp, InterfaceMib.ifName, stringEntryBuilder, integerKeyCreator);
        for (Map.Entry<IntegerKey, StringSnmpEntry> entry : ifNames.entrySet()) {
            int ifindex = entry.getKey().getInt();
            String ifname = entry.getValue().getValue();
            Port port = device.getPortByIfIndex(ifindex);
            if (port == null) {
                log.trace("ignored: " + device.getDeviceName() + " ifindex=" + ifindex + ", name=" + ifname);
                continue;
            }
            if (port instanceof EthernetPortsAggregator) {
                ((EthernetPortsAggregator) port).setAggregationName(ifname);
                log.debug("@ set port " + ifindex + " ifName [" + ifname + "] on device [" + device.getDeviceName() + "]");
                port.initIfName(ifname);
            } else {
                log.debug("@ set port " + ifindex + " ifName [" + ifname + "] on device [" + device.getDeviceName() + "]");
                port.initIfName(ifname);
            }
        }
        setUserDescriptions();
        ifmib.setAllIfNames(device);
        ifmib.setAllIfAdminStatus(device);
        ifmib.setAllIfOperStatus(device);
        DeviceInfoUtil.supplementEthernetPortsAggregatorName(device);
        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);
        CiscoStormControlMib.setStormControl(getDeviceAccess().getSnmpAccess(), this.device);
        if (getDeviceAccess().getConsoleAccess() != null) {
            getEthernetPortAttributesFromConfig();
        } else {
            getEthernetPortAttributesFromMib();
        }
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void getSpecialFa0() throws IOException, AbortedException {
        Port fa0 = this.device.getPortByIfName("Fa0");
        if (fa0 != null) {
            return;
        }
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(this.snmp, InterfaceMib.ifName);
            for (StringSnmpEntry entry : entries) {
                String ifName = entry.getValue();
                if (!"Fa0".equals(ifName)) {
                    continue;
                }
                int ifIndex = entry.getLastOIDIndex().intValue();
                EthernetPort _fa0 = new EthernetPortImpl();
                _fa0.initDevice(this.device);
                _fa0.initIfName("Fa0");
                _fa0.initIfIndex(ifIndex);
                log.debug("found special 'Fa0'");
                log.debug("@ create port " + ifIndex + " 'Fa0' on device[" + this.device.getDeviceName() + "]");
                break;
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private void setUserDescriptions() throws IOException, AbortedException {
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(this.snmp, InterfaceMib.ifAlias);
            for (StringSnmpEntry entry : entries) {
                String description = entry.getValue();
                int ifIndex = entry.getLastOIDIndex().intValue();
                Port p = this.device.getPortByIfIndex(ifIndex);
                if (p == null) {
                    continue;
                }
                p.setUserDescription(description);
                log.debug("@ set port " + ifIndex + " isAlias as user-description ["
                        + description + "] on device[" + this.device.getDeviceName() + "]");
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private void getEthernetPortAttributesFromMib() throws IOException, AbortedException {
        ifmib.setAllIfSpeed(device);
        ciscoStackMib.setVlanPortUsage(device);
    }

    private void getEthernetPortAttributesFromConfig() throws IOException, AbortedException {
        if (this.config == null) {
            getConfiguration();
        }
        String path = new ConfigElementPathBuilder().append("interface .*").toString();
        for (ConfigElement element : this.config.getsByPath(path)) {
            String longIfName = element.getAttributeValue("interface (.*)");
            log.debug("config element: " + longIfName);
            String ifName = CiscoIosCommandParseUtil.getShortIfName(longIfName);
            Port p = this.device.getPortByIfName(ifName);
            if (p == null) {
                log.debug("- no port found.");
                continue;
            } else if (!EthernetPort.class.isInstance(p)) {
                log.debug("- non ethernet port found.");
                continue;
            }
            EthernetPort eth = EthernetPort.class.cast(p);
            String speedValue = element.getAttributeValue("speed (.*)");
            PortSpeedValue.Admin speed;
            if (speedValue == null || speedValue.isEmpty()) {
                speed = PortSpeedValue.Admin.AUTO;
            } else if (speedValue.toLowerCase().equals("auto")) {
                speed = PortSpeedValue.Admin.AUTO;
            } else {
                try {
                    long _speed = Long.parseLong(speedValue);
                    speed = new PortSpeedValue.Admin(_speed);
                } catch (Exception e) {
                    log.warn("* unexpected speed value: " + ifName + "->" + speedValue);
                    speed = null;
                }
            }
            String duplexValue = element.getAttributeValue("duplex (.*)");
            EthernetPort.Duplex duplex;
            if (duplexValue == null || duplexValue.isEmpty()) {
                duplex = EthernetPort.Duplex.AUTO;
            } else if (duplexValue.toLowerCase().equals("auto")) {
                duplex = EthernetPort.Duplex.AUTO;
            } else if (duplexValue.toLowerCase().equals("full")) {
                duplex = EthernetPort.Duplex.FULL;
            } else if (duplexValue.toLowerCase().equals("half")) {
                duplex = EthernetPort.Duplex.HALF;
            } else {
                log.warn("* unexpected duplex value: " + ifName + "->" + duplexValue);
                duplex = null;
            }
            if (speed != null) {
                String s = (speed.isAuto() ? "auto" : speed.getValueAsMega() + "Mbps");
                log.debug("@" + this.device.getDeviceName() + ":" + ifName + "; speed=" + s + "(" + speedValue + ")");
                eth.setPortAdministrativeSpeed(speed);
            } else {
                log.debug("[unexpected] @" + this.device.getDeviceName() + ":" + ifName + "; speed=" + speedValue);
            }
            if (duplex != null) {
                String s = (duplex.isAuto() ? "auto" : duplex.getValue().getId());
                log.debug("@" + this.device.getDeviceName() + ":" + ifName + "; duplex=" + s + "(" + duplexValue + ")");
                eth.setDuplex(duplex);
            } else {
                log.debug("[unexpected] @" + this.device.getDeviceName() + ":" + ifName + "; duplex=" + duplexValue);
            }
            LogicalEthernetPort le = this.device.getLogicalEthernetPort(eth);
            if (le == null) {
                throw new IllegalStateException("no logical-eth: physical=" + eth.getFullyQualifiedName());
            }
            VlanPortUsage switchPortMode;
            String switchPortModeValue = element.getAttributeValue("switchport mode (.*)");
            if (switchPortModeValue == null || switchPortModeValue.isEmpty()) {
                switchPortMode = VlanPortUsage.ACCESS;
            } else if (switchPortModeValue.toLowerCase().equals("access")) {
                switchPortMode = VlanPortUsage.ACCESS;
            } else if (switchPortModeValue.toLowerCase().equals("trunk")) {
                switchPortMode = VlanPortUsage.TRUNK;
            } else if (switchPortModeValue.toLowerCase().contains("tunnel")) {
                switchPortMode = VlanPortUsage.TUNNEL;
            } else {
                log.warn("* unexpected switchPortModeValue value: " + ifName + "->" + switchPortModeValue);
                switchPortMode = VlanPortUsage.ACCESS;
            }
            log.debug("@" + this.device.getDeviceName() + ":" + ifName + "; switch-port-mode=[" + switchPortMode + "]");
            le.setVlanPortUsage(switchPortMode);
        }
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }

        CiscoVtpMibImpl vtpMib = new CiscoVtpMibImpl(this.snmp, this.device);
        vtpMib.createVlanIf();
        vtpMib.createTaggedVlan();
        ciscoStackMib.createUntaggedVlan(device);

        CiscoStpExtensionsMIB stpxMib = new CiscoStpExtensionsMIB(this.snmp);
        SpanningTreeType type = stpxMib.getSpanningTreeType(device);
        switch (type) {
            case MISTP:
            case MISTP_PVSTplus:
                stpxMib.createMistpInstanceAndVlanMapping(device);
                break;
            case MST:
            case RSTP_PVSTplus:
            case PVSTplus:
                break;
            default:
                throw new IllegalStateException("unknown stp type");
        }
        setUserDescriptions();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        if (this.textConfiguration == null) {
            getTextConfiguration();
            assert this.textConfiguration != null;
        }
        SimpleCiscoIosConfigParser parser = new SimpleCiscoIosConfigParser(this.textConfiguration);
        parser.parse();
        this.config = parser.getConfigurationStructure();
        this.device.gainConfigurationExtInfo().put(
                ExtInfoNames.DEVICE_CONFIG_STRUCTURE, this.config);
        setDiscoveryStatusDone(DiscoveryStatus.CONFIGURATION);
    }

    @Override
    public Device getDeviceInner() {
        return this.device;
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        ConsoleAccess console = null;
        try {
            console = getDeviceAccess().getConsoleAccess();
            if (console == null) {
                return "Cannot connect: no console access.";
            }
            console.connect();
            String res1 = console.getResponse(show_running_config);
            this.textConfiguration = res1;
            setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
            log.info("got text configuration. " + res1.length() + " bytes.");
            if (log.isDebugEnabled()) {
                log.debug("--- Begin Text Configuration ---");
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new StringReader(res1));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        log.debug(line);
                    }
                } catch (IOException e) {
                    log.warn(e.toString());
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            log.warn("can be ignorable: ", e);
                        }
                    }
                }
                log.debug("--- End Text Configuration ---");
            }
            return res1;
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        }
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        log.info("record: start. [" + this.getDeviceAccess().getTargetAddress().getHostAddress() + "]");
        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res1 = console.getResponse(show_running_config);
            entry.addConsoleResult(show_running_config, res1);

            String res2 = console.getResponse(show_interface);
            entry.addConsoleResult(show_interface, res2);

            String res3 = console.getResponse(show_version);
            entry.addConsoleResult(show_version, res3);

            console.close();
        }
        log.info("record: completed. [" + this.getDeviceAccess().getTargetAddress().getHostAddress() + "]");
        entry.close();
    }
}