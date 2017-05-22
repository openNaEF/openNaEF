package voss.discovery.agent.cisco;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.mib.*;
import voss.discovery.agent.cisco.mib.CiscoImageMib;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.*;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.constant.DiscoveryParameterType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.*;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.*;

public class CiscoIosDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(CiscoIosDiscovery.class);
    private final Map<String, String> productList;
    private final EntityMibAnalyzer entityMib;
    private final InterfaceMibImpl ifMib;
    private final CiscoImageMib ciscoImageMib;
    private String textConfiguration = null;
    private ConfigurationStructure config = null;

    private final AgentConfiguration agentConfig;

    private ConsoleCommand show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
    private ConsoleCommand show_interface = new ConsoleCommand(new GlobalMode(), "show interface");
    private ConsoleCommand show_xconnect_all_atm_mpls
            = new ConsoleCommand(new GlobalMode(), "show xconnect all | include ATM | include mpls");
    private ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");

    public CiscoIosDiscovery(DeviceAccess access)
            throws IOException, AbortedException {
        super(access);
        this.agentConfig = AgentConfiguration.getInstance();
        this.productList = this.agentConfig.getDiscoveryParameter(
                DiscoveryParameterType.CISCO_PRODUCT_LIST);
        this.entityMib = new EntityMibAnalyzer(
                this.agentConfig.getDiscoveryParameter(
                        DiscoveryParameterType.CISCO_ENTITY_MAP));
        this.ifMib = new InterfaceMibImpl(access.getSnmpAccess());
        this.ciscoImageMib = new CiscoImageMib(access.getSnmpAccess());
    }

    private MplsVlanDevice device;

    public Device getDeviceInner() {
        return this.device;
    }

    public void getDeviceInformation() throws IOException, AbortedException {
        device = new MplsVlanDevice();
        String fqn = Mib2Impl.getSysName(getDeviceAccess().getSnmpAccess());
        String hostname = fqn;
        String domainName = null;
        int idx = fqn.indexOf('.');
        if (idx != -1) {
            hostname = fqn.substring(0, idx);
            domainName = fqn.substring(idx + 1);
        }
        String sysObjectId = Mib2Impl.getSysObjectId(getDeviceAccess().getSnmpAccess());
        device.setModelTypeName(productList.get(sysObjectId));
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_CISCO);
        device.setOsTypeName(OSType.IOS.caption);
        device.setDeviceName(hostname);
        device.setDomainName(domainName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getSnmpAccess().getSnmpAgentAddress().getAddress()
                .getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        if (this.config == null) {
            try {
                this.getConfiguration();
            } catch (Exception e) {
                throw new IOException("Cannot get configuration.", e);
            }
        }

        ciscoImageMib.setOsVersion(device);

        ConfigElementPathBuilder path = new ConfigElementPathBuilder().append("interface .*");
        List<ConfigElement> interfaces = this.config.getsByPath(path.toString());
        Set<String> ipAddresses = new HashSet<String>();
        for (ConfigElement interface_ : interfaces) {
            String ipAddress = interface_.getAttribute("ip address .*");
            if (ipAddress != null) {
                String ip = CiscoIosCommandParseUtil.getIpAddress(ipAddress);
                ipAddresses.add(ip);
            }
        }
        this.device.setIpAddresses(ipAddresses.toArray(new String[0]));

        try {
            String serial = SnmpUtil.getString(getDeviceAccess().getSnmpAccess(),
                    EntityMib.EntityMibEntPhysicalSerialNumEntry.OID + ".1");
            device.setSerialNumber(serial);
        } catch (IOException e) {
            log.warn("cannot get chassis serial number. (" + device.getDeviceName() + ")");
        } catch (NoSuchMibException e) {
            log.warn("cannot get chassis serial number. (" + device.getDeviceName() + ")");
        } catch (SnmpResponseException e) {
            log.warn("cannot get chassis serial number. (" + device.getDeviceName() + ")");
        }
        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (device == null) {
            throw new IllegalArgumentException();
        }
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }

        try {
            entityMib.addFilter(EntityType.CHASSIS);
            entityMib.addFilter(EntityType.CONTAINER);
            entityMib.addFilter(EntityType.MODULE);
            entityMib.addFilter(EntityType.PORT);
            entityMib.analyze(getDeviceAccess().getSnmpAccess());
            Map<Integer, PhysicalEntry> result = entityMib.getEntities();
            PhysicalEntry topmost = result.get(1);
            createPhysicalModel(topmost, null);
            complementSerialPort();
            setPortAttributes();
            ifMib.createPhysicalPorts(device);
            ifMib.setAllIfNames(device);
            addFeatures();
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }

        CiscoPagpMib pagpMib = new CiscoPagpMib(this.getDeviceAccess().getSnmpAccess());
        pagpMib.createAggregationGroup(device);

        for (EthernetPortsAggregator aggregator : device.getEthernetPortsAggregators()) {
            try {
                int channel = Integer.parseInt(aggregator.getIfName().replace("Po", ""));
                aggregator.initAggregationGroupId(channel);
                aggregator.setAggregationName(aggregator.getIfName());
            } catch (NumberFormatException e) {
                throw new IOException("illegal ifname on aggregator: " + aggregator.getIfName());
            }
        }
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    protected void createPhysicalModel(PhysicalEntry entry, VlanModel target) throws IOException, AbortedException {
        log.trace("target: " + (target == null ? "null" : target.getClass().getSimpleName()));
        log.trace("entry: " + entry.toString());
        VlanModel current = null;
        if (entry.type == EntityType.CHASSIS) {
            current = device;
        } else if (entry.type == EntityType.CONTAINER
                && target instanceof Device) {
            Slot slot = PhysicalConfigurationBuilder.createSlot(entry, device);
            current = slot;
        } else if (entry.type == EntityType.MODULE && target instanceof Device) {
            Slot slot = createNPESlot(device);
            Module module = PhysicalConfigurationBuilder.createModule(entry, slot);
            current = module;
        } else if (entry.type == EntityType.MODULE && target instanceof Slot) {
            if (isNpeWorkaroundNeeded(entry)) {
                log.warn("NPE workaround: " + entry.name);
                Slot slot = createNPESlot(device);
                Module module = PhysicalConfigurationBuilder.createModule(entry, slot);
                current = module;
            } else {
                Slot slot = (Slot) target;
                Module module = PhysicalConfigurationBuilder.createModule(entry, slot);
                current = module;
            }
        } else if (entry.type == EntityType.PORT && target instanceof Module) {
            Module module = (Module) target;
            String ifname = CiscoIosCommandParseUtil.getShortIfName(entry.physicalName);
            PhysicalPort port = CiscoIosCommandParseUtil.createCiscoPhysicalPort(getSnmpAccess(), this.device, ifname, entry.ifindex);
            module.addPort(port);
            current = port;
            log.debug("@ add port ifname='" + ifname + "'"
                    + " on device '" + device.getDeviceName() + "'");
        } else if (entry.type == EntityType.CONTAINER && target instanceof Module) {
            Module module = (Module) target;
            if (CiscoHardwareUtil.isSubSlot(entry)) {
                Slot slot = PhysicalConfigurationBuilder.createSlot(entry, module);
                current = slot;
            } else if (!CiscoHardwareUtil.isPort(entry)) {
                log.debug("ignored: " + entry.toString());
                return;
            } else {
                String portName = CiscoIosCommandParseUtil.guessIfName(entry.physicalName);
                PhysicalPort port = (PhysicalPort) device.getPortByIfName(portName);
                module.addPort(port);
                current = port;
                log.debug("@ add port '" + port.getIfIndex()
                        + " to module '" + module.getSlot().getSlotIndex()
                        + " on device '" + device.getDeviceName() + "'");
            }

        } else {
            log.debug("ignored: " + entry.toString());
            return;
        }
        for (PhysicalEntry child : entry.getChildren()) {
            createPhysicalModel(child, current);
        }
    }

    public Slot createNPESlot(Device device) {
        Slot slot = PhysicalConfigurationBuilder.createSlot(device, -1, "-", "NPE Slot");
        return slot;
    }

    private boolean isNpeWorkaroundNeeded(PhysicalEntry entry) {
        return entry.name.matches("cevCpu.*Npe.*");
    }

    private void addFeatures() throws AbortedException, IOException {
        Map<String, SerialPort> serialPorts = new HashMap<String, SerialPort>();
        for (PhysicalPort phy : this.device.getPhysicalPorts()) {
            try {
                if (phy instanceof SerialPort) {
                    int ifIndex = phy.getIfIndex();
                    serialPorts.put(InterfaceMib.ifType + "." + ifIndex, (SerialPort) phy);
                }
            } catch (NotInitializedException e) {
                log.warn("no ifindex: " + phy.getIfName(), e);
            }
        }
        try {
            List<String> oids = new ArrayList<String>(serialPorts.keySet());
            Collections.sort(oids);
            List<SnmpEntry> entries = SnmpUtil.getMultipleSnmpEntries(getSnmpAccess(), oids);
            for (SnmpEntry entry : entries) {
                IntSnmpEntry entry_ = IntSnmpEntry.getInstance(entry);
                String oid = entry_.getRootOidStr();
                SerialPort serialPort = serialPorts.get(oid);
                int ianaTypeID = entry_.intValue();
                switch (ianaTypeID) {
                    case 32:
                        FrameRelayFeature fr = new FrameRelayFeatureImpl();
                        serialPort.setLogicalFeature(fr);
                        fr.initPhysicalPort(serialPort);
                        break;
                    default:
                        break;
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }

    }

    private void complementSerialPort() throws AbortedException, IOException {
        if (this.config == null) {
            try {
                this.getConfiguration();
            } catch (Exception e) {
                throw new IOException("Cannot get configuration.", e);
            }
        }
        CiscoIosConfigAnalyzer.buildSerialPortOnController(this.device, getDeviceAccess(), this.config);
    }

    private void setPortAttributes() throws AbortedException, IOException {
        Map<String, Long> map = new HashMap<String, Long>();

        MibTable ifXTable = new MibTable(getSnmpAccess(), "", InterfaceMib.ifXTable);
        ifXTable.addColumn(InterfaceMib.ifName_SUFFIX, "ifName");
        ifXTable.addColumn(InterfaceMib.ifConnectorPresent_SUFFIX, "ifConnectorPresent");
        ifXTable.addColumn(InterfaceMib.ifHighSpeed_SUFFIX, "ifHighSpeed");
        ifXTable.addColumn(InterfaceMib.ifAlias_SUFFIX, "ifAlias");
        ifXTable.walk();

        Set<String> knownIfName = new HashSet<String>();
        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(ifXTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = ifXTable.getKeyAndRows().get(key);
            StringSnmpEntry ifNameEntry = row.getColumnValue(
                    InterfaceMib.ifName_SUFFIX,
                    SnmpHelper.stringEntryBuilder);
            String ifName = ifNameEntry.getValue();
            if (knownIfName.contains(ifName)) {
                continue;
            }
            knownIfName.add(ifName);

            int ifIndex = key.intValue(0);
            Port port = device.getPortByIfName(ifName);
            if (port == null) {
                log.warn("no port found: " + ifName);
                continue;
            } else if (!(port instanceof PhysicalPort)) {
                throw new IllegalStateException("not physical port: ifIndex=" + ifIndex);
            }
            PhysicalPort phy = (PhysicalPort) port;
            try {
                log.debug("@set port ifName='" + port.getIfName() + "' ifindex='" + ifIndex + "';");
                phy.initIfIndex(ifIndex);
            } catch (IllegalStateException e) {
                log.debug("already set ifindex: port ifName='" + port.getIfName() + "' ifindex='" + ifIndex + "';");
            }

            IntSnmpEntry ifHighSpeedEntry = row.getColumnValue(
                    InterfaceMib.ifHighSpeed_SUFFIX,
                    SnmpHelper.intEntryBuilder);
            if (ifHighSpeedEntry != null) {
                int highSpeed = ifHighSpeedEntry.intValue();
                long speed = ((long) highSpeed) * 1000L * 1000L;
                map.put(ifName, speed);
            }

            StringSnmpEntry ifAliasEntry = row.getColumnValue(
                    InterfaceMib.ifAlias_SUFFIX,
                    SnmpHelper.stringEntryBuilder);
            String ifAlias = ifAliasEntry.getValue();
            phy.setIfDescr(ifAlias);
            phy.setUserDescription(ifAlias);
        }

        MibTable ifTable = new MibTable(getSnmpAccess(), "", InterfaceMib.ifTable);
        ifTable.addColumn(InterfaceMib.ifDesc_SUFFIX, "ifDesc");
        ifTable.addColumn(InterfaceMib.ifAdminStatus_SUFFIX, "ifAdminStatus");
        ifTable.addColumn(InterfaceMib.ifOperStatus_SUFFIX, "ifOperStatus");
        ifTable.addColumn(InterfaceMib.ifSpeed_SUFFIX, "ifSpeed");
        ifTable.walk();
        for (TableRow row : ifTable.getRows()) {
            KeyHolder key = row.getKey();
            int ifIndex = key.intValue(0);

            Port port = device.getPortByIfIndex(ifIndex);
            if (port == null) {
                continue;
            } else if (!(port instanceof PhysicalPort)) {
                throw new IllegalStateException("not physical port: ifIndex=" + ifIndex);
            }
            PhysicalPort phy = (PhysicalPort) port;
            String ifDesc = row.getColumnValue(
                    InterfaceMib.ifDesc_SUFFIX,
                    SnmpHelper.stringEntryBuilder).getValue();

            phy.setPortName(ifDesc);
            phy.setSystemDescription(ifDesc);

            int speed = row.getColumnValue(
                    InterfaceMib.ifSpeed_SUFFIX,
                    SnmpHelper.intEntryBuilder).intValue();
            Long highSpeed_ = map.get(phy.getIfName());
            long highSpeed = (highSpeed_ == null ? 0L : highSpeed_.longValue());
            PortSpeedValue.Oper oper;
            if (highSpeed > speed) {
                oper = new PortSpeedValue.Oper(highSpeed);
            } else {
                oper = new PortSpeedValue.Oper(speed);
            }
            phy.setPortOperationalSpeed(oper);

            int adminStatus = row.getColumnValue(InterfaceMib.ifAdminStatus_SUFFIX, SnmpHelper.intEntryBuilder).intValue();
            int operStatus = row.getColumnValue(InterfaceMib.ifOperStatus_SUFFIX, SnmpHelper.intEntryBuilder).intValue();
            phy.setAdminStatus(InterfaceMibImpl.getAdminStatusString(adminStatus));
            phy.setOperationalStatus(InterfaceMibImpl.getOperStatusString(operStatus));
        }
    }

    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        if (getDeviceAccess().getConsoleAccess() == null) {
            return;
        }
        if (!isDiscoveryDone(DiscoveryStatus.TEXT_CONFIGURATION)) {
            getConfiguration();
        }

        CiscoIosConfigAnalyzer.buildVrf(device, getDeviceAccess(), config);

        CiscoIosConfigAnalyzer.buildLoopbackInterface(device, getDeviceAccess(), config);
        CiscoIosConfigAnalyzer.buildVlan(device, getDeviceAccess(), config);

        getFrameRelayConfiguration();

        getAtmConfiguration();

        CiscoIosConfigAnalyzer.buildVrfAttachment(device, getDeviceAccess(), config);

        getMplsConfiguration();

        OspfMibImpl.getOspfIfParameters(getSnmpAccess(), device);

        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void getFrameRelayConfiguration() throws AbortedException, IOException {
        CiscoFrameRelayMIB frMib = new CiscoFrameRelayMIB(this.device, getSnmpAccess());
        frMib.createFrameRelayDLCI();
    }

    private void getAtmConfiguration() throws AbortedException, IOException {
        AtmMibImpl atmMib = new AtmMibImpl(getSnmpAccess(), this.device);
        atmMib.createAtmVp();
        atmMib.createAtmVc();
        CiscoIosConfigAnalyzer.buildAtmPvcInterface(device, getDeviceAccess(), config);
        for (Port port : device.getPorts()) {
            if (port instanceof AtmPvc) {
                AtmPvc pvc = (AtmPvc) port;
                ifMib.setIfSpeed(pvc);
            }
        }
    }

    private void getMplsConfiguration() throws IOException, AbortedException {
        CiscoIetfPwMib ietfPwMib = new CiscoIetfPwMib(getDeviceAccess().getSnmpAccess(), device);
        ietfPwMib.getPseudoWires();

        CiscoMplsTeMibImpl teMib = new CiscoMplsTeMibImpl(getDeviceAccess().getSnmpAccess(), device);
        teMib.getMplsTunnels();

        CiscoIosConfigAnalyzer.buildMplsLspHops(device, getDeviceAccess(), config);

        CiscoIetfPwEnetMibImpl vcEnetMibImpl = new CiscoIetfPwEnetMibImpl(getSnmpAccess(), this.device);
        vcEnetMibImpl.connectPwAndEthernetPort();

        CiscoPwFrMibImpl frMib = new CiscoPwFrMibImpl(getSnmpAccess(), this.device);
        frMib.connectPwAndFrameRelayPvc();

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        try {
            try {
                console.connect();
                String showXconnectAll = console.getResponse(show_xconnect_all_atm_mpls);
                log.debug("show xconnect all -> [\r\n" + showXconnectAll + "]");
                CiscoShowXconnectCommandParser atmXconnectParser =
                        new CiscoShowXconnectCommandParser(showXconnectAll, this.device);
                atmXconnectParser.connectPwAndAtmPvc();
            } finally {
            }
        } catch (ConsoleException e) {
            throw new IOException("failed to get result of " + show_xconnect_all_atm_mpls.getCommand(), e);
        }

    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
        getDeviceInformation();
        getPhysicalConfiguration();
        CiscoIosConfigAnalyzer.buildVlan(device, getDeviceAccess(), config);
        getAtmConfiguration();
        ifMib.setAllIfOperStatus(device);

    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        ConsoleAccess console = null;
        try {
            console = this.getDeviceAccess().getConsoleAccess();
            if (console == null) {
                return "Cannot connect: no console access.";
            }
            console.connect();
            String res1 = console.getResponse(show_running_config);
            this.textConfiguration = res1;
            setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
            log.debug("getTextConfiguration(): ["
                    + this.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress()
                    + "] result:\r\n" + res1 + "\r\n----");
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        } finally {
        }
        setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
        return this.textConfiguration;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        if (this.textConfiguration == null) {
            getTextConfiguration();
        }

        SimpleCiscoIosConfigParser parser = new SimpleCiscoIosConfigParser(this.textConfiguration);
        parser.parse();

        this.config = parser.getConfigurationStructure();
        this.device.gainConfigurationExtInfo().put(
                ExtInfoNames.DEVICE_CONFIG_STRUCTURE, this.config);
        setDiscoveryStatusDone(DiscoveryStatus.CONFIGURATION);
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException, ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res1 = console.getResponse(show_running_config);
            entry.addConsoleResult(show_running_config, res1);

            String res2 = console.getResponse(show_interface);
            entry.addConsoleResult(show_interface, res2);

            String res3 = console.getResponse(show_xconnect_all_atm_mpls);
            entry.addConsoleResult(show_xconnect_all_atm_mpls, res3);

            String res4 = console.getResponse(show_version);
            entry.addConsoleResult(show_version, res4);

            console.close();
        }

        entry.close();
    }
}