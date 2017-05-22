package voss.discovery.agent.cisco;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.mib.*;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.EntityMib.EntityMibEntPhysicalModelNameEntry;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.constant.AccessMode;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.integerKeyCreator;
import static voss.discovery.iolib.snmp.SnmpHelper.stringEntryBuilder;

public class CiscoCatalystDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(CiscoCatalystDiscovery.class);
    private final ConsoleCommand show_running_config;
    private final ConsoleCommand show_interface;
    private final ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");

    private final CiscoStackMibImpl ciscoStackMib;
    private final String osType;
    private final CiscoImageMib ciscoImageMib;
    private String textConfiguration = null;
    private ConfigurationStructure config = null;
    protected final SnmpAccess snmp;
    protected final MplsVlanDevice device;


    public CiscoCatalystDiscovery(DeviceAccess access) throws IOException, AbortedException {
        super(access);
        this.snmp = access.getSnmpAccess();
        this.device = new MplsVlanDevice();
        this.ciscoStackMib = new CiscoStackMibImpl(snmp, device);
        this.ciscoImageMib = new CiscoImageMib(snmp);

        this.osType = CiscoImageMib.getOSType(snmp).caption;
        log.info("detected os type: " + this.osType);

        if (this.osType.equals(OSType.IOS.caption)) {
            this.show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
            this.show_interface = new ConsoleCommand(new GlobalMode(), "show interface");
        } else {
            this.show_running_config = new ConsoleCommand(new GlobalMode(), "show config");
            this.show_interface = new ConsoleCommand(new GlobalMode(), "show port");
        }
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

        try {
            String modelTypeName = SnmpUtil.getString(snmp, EntityMibEntPhysicalModelNameEntry.OID + ".1");
            device.setModelTypeName(modelTypeName);
        } catch (Exception e) {
            ciscoStackMib.setModelTypeName(device);

        }

        setOsType();
        try {
            device.setBasePhysicalAddress(Mib2Impl.getSystemMacAddress(snmp));
        } catch (IOException e) {
            log.warn("cannot get system mac address: " + device.getIpAddress()
                    + " (" + device.getDeviceName() + ")");
        }
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

    protected void setOsType() throws IOException, AbortedException {
        device.setOsTypeName(osType);
        ciscoImageMib.setOsVersion(device);
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        ciscoStackMib.createSlotAndModule(device);
        ciscoStackMib.createPhysicalPorts(device);

        InterfaceMibImpl ifmib = new InterfaceMibImpl(snmp);
        CiscoPagpMib pagpMib = new CiscoPagpMib(this.snmp);

        pagpMib.createAggregationGroup(device);

        for (EthernetPortsAggregator aggregator : device.getEthernetPortsAggregators()) {
            EthernetPort[] ports = aggregator.getPhysicalPorts();
            if (ports.length == 0) {
                continue;
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
                ifname = "[EtherChannel]" + ifname;
                ((EthernetPortsAggregator) port).setAggregationName(ifname);
                log.debug("@ set port " + ifindex + " ifName [" + ifname + "] on device [" + device.getDeviceName() + "]");
                port.initIfName(ifname);
            } else {
                log.debug("@ set port " + ifindex + " ifName [" + ifname + "] on device [" + device.getDeviceName() + "]");
                port.initIfName(ifname);
            }
        }

        ifmib.setAllIfDescriptions(device);
        ifmib.setAllIfAliases(device);
        ifmib.setAllIfAdminStatus(device);
        ifmib.setAllIfOperStatus(device);
        ifmib.setAllIfSpeed(device);

        CiscoStormControlMib.setStormControl(getSnmpAccess(), device);

        DeviceInfoUtil.supplementEthernetPortsAggregatorName(device);
        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);

        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
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
                log.info("unknown stp type");
        }

        if (this.osType.equals(OSType.IOS.caption)) {
            if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
                getPhysicalConfiguration();
            }

            try {
                if (!isDiscoveryDone(DiscoveryStatus.TEXT_CONFIGURATION)) {
                    getConfiguration();
                }
                CiscoIosConfigAnalyzer.buildLoopbackInterface(device, getDeviceAccess(), config);
                getMplsConfiguration();
            } catch (RuntimeException e) {
                if (getDeviceAccess().getMode() == AccessMode.SIMULATED) {
                    System.err.println("ignored: getConfiguration(): " + e.toString());
                    e.printStackTrace();
                }
            }
        }

        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void getMplsConfiguration() throws IOException, AbortedException {
        CiscoIetfPwMib ietfPwMib = new CiscoIetfPwMib(getDeviceAccess().getSnmpAccess(), device);
        ietfPwMib.getPseudoWires();
        for (PseudoWirePort pw : device.getPseudoWirePorts()) {
            System.out.println("** " + pw.toString());
        }

        CiscoMplsTeMibImpl teMib = new CiscoMplsTeMibImpl(getDeviceAccess().getSnmpAccess(), device);
        teMib.getMplsTunnels();

        CiscoIosConfigAnalyzer.buildMplsLspHops(device, getDeviceAccess(), config);

        CiscoIetfPwEnetMibImpl vcEnetMibImpl = new CiscoIetfPwEnetMibImpl(getDeviceAccess().getSnmpAccess(), this.device);
        vcEnetMibImpl.connectPwAndEthernetPort();

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

        if (this.osType.equals(OSType.IOS.caption)) {
            SimpleCiscoIosConfigParser parser = new SimpleCiscoIosConfigParser(this.textConfiguration);
            parser.parse();
            this.config = parser.getConfigurationStructure();
            this.device.gainConfigurationExtInfo().put(
                    ExtInfoNames.DEVICE_CONFIG_STRUCTURE, this.config);
        } else {
            log.error("getConfiguration(): not supported os type: " + this.osType);
        }

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