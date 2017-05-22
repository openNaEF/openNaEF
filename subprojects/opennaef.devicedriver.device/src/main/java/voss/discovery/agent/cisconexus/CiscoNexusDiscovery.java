package voss.discovery.agent.cisconexus;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.mib.CiscoImageMib;
import voss.discovery.agent.cisco.mib.CiscoVtpMibImpl;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.*;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.constant.DiscoveryParameterType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.netconf.NetConfAccess;
import voss.discovery.iolib.netconf.NetConfException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.*;
import voss.model.EthernetPort.AutoNego;
import voss.model.EthernetPort.Duplex;
import voss.model.value.PortSpeedValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CiscoNexusDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(CiscoNexusDiscovery.class);
    private final EntityMibAnalyzer entityMib;
    private final InterfaceMibImpl ifMib;
    private final CiscoImageMib ciscoImageMib;
    private String textConfiguration = null;
    private ConfigurationStructure config = null;

    private final AgentConfiguration agentConfig;

    private ConsoleCommand show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
    private ConsoleCommand show_interface = new ConsoleCommand(new GlobalMode(), "show interface");
    private ConsoleCommand show_interface_status = new ConsoleCommand(new GlobalMode(), "show interface status");
    private ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");
    private ConsoleCommand show_interface_fex_fabric = new ConsoleCommand(new GlobalMode(), "show interface fex-fabric");
    private ConsoleCommand show_fex = new ConsoleCommand(new GlobalMode(), "show fex detail");
    private ConsoleCommand show_vpc = new ConsoleCommand(new GlobalMode(), "show vpc");

    private final Pattern portChannelPattern = Pattern.compile("channel-group ([0-9]+) ?(.*)?");

    public CiscoNexusDiscovery(DeviceAccess access)
            throws IOException, AbortedException {
        super(access);
        this.agentConfig = AgentConfiguration.getInstance();
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
        String lastOID = sysObjectId.substring(sysObjectId.lastIndexOf('.') + 1);
        if (lastOID.equals("1038")) {
            device.setModelTypeName("Nexus 5596");
        } else if (lastOID.equals("1084")) {
            device.setModelTypeName("Nexus 5548");
        } else {
            device.setModelTypeName("Unknown Nexus Switch(" + lastOID + ")");
        }
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_CISCO);
        device.setOsTypeName(OSType.NX_OS.caption);
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

        ConfigElementPathBuilder path = new ConfigElementPathBuilder().append("interface mgmt0");
        List<ConfigElement> interfaces = this.config.getsByPath(path.toString());
        Set<String> ipAddresses = new HashSet<String>();
        for (ConfigElement interface_ : interfaces) {
            String ipAddress = interface_.getAttribute("ip address .*");
            if (ipAddress != null) {
                String ip = CiscoNxOsCommandParseUtil.getIpAddress(ipAddress);
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
            entityMib.addFilter(EntityType.STACK);
            entityMib.addFilter(EntityType.CHASSIS);
            entityMib.addFilter(EntityType.CONTAINER);
            entityMib.addFilter(EntityType.MODULE);
            entityMib.addFilter(EntityType.PORT);
            entityMib.analyze(getDeviceAccess().getSnmpAccess());
            PhysicalEntry topmost = entityMib.getTopmostEntity();
            createPhysicalModel(topmost, null);
            setPortAttributes();
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }

        String path = new ConfigElementPathBuilder().append("interface Ethernet.+").toString();
        List<ConfigElement> elements = config.getsByPath(path);
        createLAGs(elements);
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        setPortsAttribute(elements);
        setFexUplink();
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void createLAGs(List<ConfigElement> elements) throws IOException, AbortedException {
        for (ConfigElement element : elements) {
            createLAG(element);
        }
    }

    private void createLAG(ConfigElement element) throws IOException, AbortedException {
        String ifName = element.getId().replace("interface ", "");
        EthernetPort eth = (EthernetPort) device.getPortByIfName(ifName);
        if (eth == null) {
            log.warn("no port found: " + ifName);
            return;
        }
        setPortChannel(element, eth);
    }

    private void setPortChannel(ConfigElement element, EthernetPort eth) throws IOException, AbortedException {
        String portChannelAttr = element.getAttribute("channel-group .*");
        if (portChannelAttr == null) {
            return;
        }
        Matcher matcher = portChannelPattern.matcher(portChannelAttr);
        if (!matcher.matches()) {
            log.warn("illegal port channel: " + portChannelAttr + " on " + eth.getIfName());
            return;
        }
        String _channelID = matcher.group(1);
        try {
            int channelID = Integer.parseInt(_channelID);
            EthernetPortsAggregator lag = device.getEthernetPortsAggregatorByAggregationGroupId(channelID);
            if (lag == null) {
                lag = createPortChannel(channelID);
            }
            lag.addPhysicalPort(eth);
        } catch (NumberFormatException e) {
            log.warn("illegal port channel id: " + portChannelAttr + " on " + eth.getIfName());
            return;
        }
    }

    private EthernetPortsAggregator createPortChannel(int channelID) throws IOException, AbortedException {
        EthernetPortsAggregator lag;
        lag = new EthernetPortsAggregatorImpl();
        lag.initDevice(this.device);
        lag.initAggregationGroupId(channelID);
        String lagName = "port-channel" + channelID;
        lag.initIfName(lagName);
        lag.setAggregationName(lagName);
        Mib2Impl.setIfIndex(getSnmpAccess(), lag, lag.getIfName());
        log.debug("@ add port-channel ifname='" + lagName + "'" + " on device '" + device.getDeviceName() + "'");
        return lag;
    }

    private void setPortsAttribute(List<ConfigElement> elements) throws IOException, AbortedException {
        for (ConfigElement ethernetElement : elements) {
            setPortAttributes(ethernetElement);
        }
        for (EthernetPort eth : this.device.getEthernetPorts()) {
            LogicalEthernetPort le = this.device.getLogicalEthernetPort(eth);
            if (le == null) {
                continue;
            }
            if (eth.getPortAdministrativeSpeed() == null) {
                eth.setPortAdministrativeSpeed(PortSpeedValue.Admin.AUTO);
            }
            if (eth.getDuplex() == null) {
                eth.setDuplex(Duplex.AUTO);
            }
            if (eth.getIfName().toLowerCase().startsWith("uplink")) {
                le.setVlanPortUsage(VlanPortUsage.TRUNK);
            } else if (le.getVlanPortUsage() == null) {
                le.setVlanPortUsage(VlanPortUsage.ACCESS);
            }
        }
    }

    private void setPortAttributes(ConfigElement element) throws IOException, AbortedException {
        String ifName = element.getId().replace("interface ", "");
        log.debug("ifName=" + ifName);
        EthernetPort eth = (EthernetPort) device.getPortByIfName(ifName);
        if (eth == null) {
            log.warn("no port found: " + ifName);
            return;
        }
        setDuplex(element, eth);
        setAutoNego(element, eth);
        setSwitchPortMode(element, eth);
    }

    private void setDuplex(ConfigElement element, EthernetPort eth) {
        String _duplex = element.getAttributeValue("duplex (.*)");
        log.debug("_duplex=" + _duplex);
        Duplex value;
        if (_duplex == null) {
            value = Duplex.AUTO;
            ;
        } else if (_duplex.equals("full")) {
            value = Duplex.FULL;
        } else if (_duplex.equals("half")) {
            value = Duplex.HALF;
        } else {
            log.debug("- unexpected duplex value: " + _duplex);
            value = null;
        }
        if (value != null) {
            eth.setDuplex(value);
        }
    }

    private void setAutoNego(ConfigElement element, EthernetPort eth) {
        String _duplex = element.getAttribute("speed (.*)");
        AutoNego value;
        if (_duplex == null) {
            value = AutoNego.ON;
        } else if (_duplex.equals("auto")) {
            value = AutoNego.ON;
        } else {
            value = AutoNego.OFF;
        }
        eth.setAutoNego(value);
    }

    private void setSwitchPortMode(ConfigElement element, EthernetPort eth) {
        String switchPortMode = element.getAttribute("switchport mode (.*)");
        if (switchPortMode == null) {
            return;
        }
        switchPortMode = switchPortMode.replace("switchport mode ", "");
        LogicalEthernetPort le = this.device.getLogicalEthernetPort(eth);
        switchPortMode = switchPortMode.toLowerCase();
        VlanPortUsage mode = VlanPortUsage.ACCESS;
        if ("trunk".equals(switchPortMode)) {
            mode = VlanPortUsage.TRUNK;
        } else if ("dot1qtunnel".equals(switchPortMode)) {
            mode = VlanPortUsage.TUNNEL;
        } else if ("fex-fabric".equals(switchPortMode)) {
            mode = VlanPortUsage.TRUNK;
            eth.setConnectorTypeName("fex-fablic");
        }
        log.debug("@ set switchport-mode " + mode + " to " + le.getIfName());
        le.setVlanPortUsage(mode);
    }

    protected void createPhysicalModel(PhysicalEntry entry, VlanModel target) throws IOException, AbortedException {
        log.trace("target: " + (target == null ? "null" : target.getClass().getSimpleName()));
        log.trace("entry: " + entry.toString());
        VlanModel current = null;
        if (entry.type == EntityType.STACK) {
            current = device;
            device.setSerialNumber(entry.serialNumber);
            EthernetPort mgmt = new EthernetPortImpl();
            mgmt.initDevice(device);
            mgmt.initIfName("mgmt0");
            mgmt.initPortIndex(0);
            addManagementIpAddress(mgmt);
            Mib2Impl.setIfIndex(getSnmpAccess(), mgmt, "mgmt0");
            log.debug("create device.");
        } else if (entry.type == EntityType.CHASSIS && Device.class.isInstance(target)) {
            Slot slot = PhysicalConfigurationBuilder.createSlot(entry, device);
            current = slot;
            log.debug("create slot.");
        } else if (entry.type == EntityType.CONTAINER && Slot.class.isInstance(target)) {
            current = target;
            log.debug("skip container.");
        } else if (entry.type == EntityType.MODULE && Slot.class.isInstance(target)) {
            Slot slot = (Slot) target;
            Module module = PhysicalConfigurationBuilder.createModule(entry, slot);
            current = module;
            log.debug("create module.");
        } else if (entry.type == EntityType.PORT && target instanceof Module) {
            Module module = (Module) target;
            String ifname = CiscoNxOsCommandParseUtil.getIfName(entry.physicalName);
            PhysicalPort port = CiscoNxOsCommandParseUtil.createCiscoPhysicalPort(getSnmpAccess(), this.device, ifname);
            port.initModule(module);
            port.initPortIndex(entry.position);
            module.addPort(port);
            current = port;
            log.debug("@ add port ifname='" + ifname + "'"
                    + " on device '" + device.getDeviceName() + "'");
        } else {
            log.debug("ignored: target="
                    + (target == null ? "<None>" : target.getClass().getSimpleName())
                    + " entry=" + entry.toString());
            return;
        }
        for (PhysicalEntry child : entry.getChildren()) {
            createPhysicalModel(child, current);
        }
    }

    private void addManagementIpAddress(EthernetPort mgmt) {
        ConfigElementPathBuilder path = new ConfigElementPathBuilder().append("interface mgmt0");
        List<ConfigElement> interfaces = this.config.getsByPath(path.toString());
        for (ConfigElement interface_ : interfaces) {
            String ipAddress = interface_.getAttribute("ip address .*");
            if (ipAddress == null) {
                continue;
            }
            try {
                String ip = CiscoNxOsCommandParseUtil.getIpAddress(ipAddress);
                InetAddress _addr = InetAddress.getByName(ip);
                int maskLength = CiscoNxOsCommandParseUtil.getIpAddressMaskLength(ipAddress);
                CidrAddress addr = new CidrAddress(_addr, maskLength);
                this.device.addIpAddressToPort(addr, mgmt);
            } catch (Exception e) {
            }
        }
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

    public static final Pattern showInterfaceFexFablicPattern =
            Pattern.compile("([0-9]+) +(Eth[0-9/]+) +([0-9A-z]+) +([0-9]+) +.*");

    private void setFexUplink() throws IOException, AbortedException {
        if (getConsoleAccess() == null) {
            return;
        }
        ConsoleAccess console = getConsoleAccess();
        try {
            if (!console.isConnected()) {
                console.connect();
            }
            String result = console.getResponse(show_interface_fex_fabric);
            if (result == null) {
                return;
            }
            BufferedReader br = new BufferedReader(new StringReader(result));
            String line = null;
            while ((line = br.readLine()) != null) {
                Matcher matcher = showInterfaceFexFablicPattern.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }
                String fexID = matcher.group(1);
                String downPortName = matcher.group(2);
                String status = matcher.group(3);
                String uplinkPortName = matcher.group(4);
                log.debug("fex-uplink found: " + downPortName + " -- " + fexID + ":" + uplinkPortName + " status=" + status);
                Slot slot = this.device.getSlotBySlotId(fexID);
                if (slot == null) {
                    log.warn("no FEX slot found: id=" + fexID);
                    continue;
                }
                Module fexModule = slot.getModule();
                if (fexModule == null) {
                    log.warn("no FEX module found: id=" + fexID);
                    continue;
                }
                PhysicalPort uplinkPort = null;
                for (PhysicalPort port : fexModule.getPhysicalPorts()) {
                    if (!port.getIfName().equals("Uplink" + fexID + "/1/" + uplinkPortName)) {
                        continue;
                    }
                    log.debug("uplink port found: " + port.getFullyQualifiedName());
                    uplinkPort = port;
                }
                if (uplinkPort == null) {
                    log.debug("no uplink port found: " + uplinkPortName);
                    continue;
                }
                downPortName = CiscoNxOsCommandParseUtil.getFullyQualifiedInterfaceName(downPortName);
                PhysicalPort downlinkPort = (PhysicalPort) this.device.getPortByIfName(downPortName);
                if (downlinkPort == null) {
                    log.debug("no downlink port found: " + downPortName);
                    continue;
                }
                Link link = new LinkImpl();
                link.initPorts(uplinkPort, downlinkPort);
            }
            br.close();
        } catch (ConsoleException e) {
            throw new IOException("command failed: " + show_interface_fex_fabric.getCommand(), e);
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
        CiscoNxOsConfigAnalyzer.buildLoopbackInterface(device, getDeviceAccess(), config);
        CiscoVtpMibImpl vtpMib = new CiscoVtpMibImpl(getSnmpAccess(), this.device);
        vtpMib.createVlanIf();
        vtpMib.createTaggedVlan();
        createUntaggedVlan();
        OspfMibImpl.getOspfIfParameters(getSnmpAccess(), device);
        setVlanIpAddress();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void createUntaggedVlan() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> vmVlans = SnmpUtil.getIntSnmpEntries(getSnmpAccess(), ".1.3.6.1.4.1.9.9.68.1.2.2.1.2");
            for (LogicalEthernetPort le : this.device.getLogicalEthernetPorts()) {
                VlanPortUsage usage = le.getVlanPortUsage();
                if (usage != null && usage == VlanPortUsage.TRUNK) {
                    continue;
                }
                int ifindex;
                if (le.isAggregated()) {
                    if (le.getRawIfIndex() != null && le.getIfIndex() > 0) {
                        ifindex = le.getIfIndex();
                    } else {
                        continue;
                    }
                } else {
                    EthernetPort eth = ((DefaultLogicalEthernetPort) le).getPhysicalPort();
                    if (eth.getRawIfIndex() != null && eth.getIfIndex() > 0) {
                        ifindex = eth.getIfIndex();
                    } else {
                        continue;
                    }
                }
                for (IntSnmpEntry vmVlan : vmVlans) {
                    if (vmVlan.getLastOIDIndex().intValue() == ifindex) {
                        int vlanID = vmVlan.intValue();
                        VlanIf vif = this.device.getVlanIfByVlanId(vlanID);
                        if (vif == null) {
                            throw new IllegalStateException("no vlan-if found: " + vlanID);
                        }
                        vif.addUntaggedPort(le);
                        break;
                    }
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e.getMessage(), e);
        } catch (RepeatedOidException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void setVlanIpAddress() {
        ConfigElementPathBuilder path = new ConfigElementPathBuilder().append("interface Vlan([0-9]+)");
        List<ConfigElement> interfaces = this.config.getsByPath(path.toString());
        for (ConfigElement interface_ : interfaces) {
            try {
                String _vid = interface_.getId().replace("interface Vlan", "").trim();
                int vlanID = Integer.parseInt(_vid);
                VlanIf vif = this.device.getVlanIfByVlanId(vlanID);
                if (vif == null) {
                    log.warn("[unexpected problem] no vlan-if: vid=" + vlanID);
                    continue;
                }
                String ipAddress = interface_.getAttribute("ip address .*");
                if (ipAddress == null) {
                    continue;
                }
                String ip = CiscoNxOsCommandParseUtil.getIpAddress(ipAddress);
                InetAddress _addr = InetAddress.getByName(ip);
                int maskLength = CiscoNxOsCommandParseUtil.getIpAddressMaskLength(ipAddress);
                CidrAddress addr = new CidrAddress(_addr, maskLength);
                this.device.addIpAddressToPort(addr, vif);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
        getDeviceInformation();
        getPhysicalConfiguration();
        CiscoNxOsConfigAnalyzer.buildVlan(device, getDeviceAccess(), config);
        ifMib.setAllIfOperStatus(device);
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        NetConfAccess netconf = null;
        try {
            netconf = this.getDeviceAccess().getNetConfAccess();
            if (netconf == null) {
                return "Cannot connect: no netconf access.";
            }
            netconf.connect();
            String res1 = netconf.getResponse(show_running_config.getCommand());
            this.textConfiguration = res1;
            setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
            log.debug("getTextConfiguration(): ["
                    + this.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress()
                    + "] result:\r\n" + res1 + "\r\n----");
        } catch (NetConfException ce) {
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

        SimpleCiscoNxOsConfigParser parser = new SimpleCiscoNxOsConfigParser(this.textConfiguration);
        parser.parse();

        this.config = parser.getConfigurationStructure();
        this.device.gainConfigurationExtInfo().put(
                ExtInfoNames.DEVICE_CONFIG_STRUCTURE, this.config);
        setDiscoveryStatusDone(DiscoveryStatus.CONFIGURATION);
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException, ConsoleException, NetConfException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            addConsoleResult(console, show_running_config, entry);
            addConsoleResult(console, show_interface, entry);
            addConsoleResult(console, show_interface_fex_fabric, entry);
            addConsoleResult(console, show_interface_status, entry);
            addConsoleResult(console, show_fex, entry);
            addConsoleResult(console, show_vpc, entry);
            addConsoleResult(console, show_version, entry);
            console.close();
        }

        NetConfAccess netconf = getDeviceAccess().getNetConfAccess();
        if (netconf != null) {
            netconf.connect();
            addNetConfResult(netconf, show_running_config, entry);
            addNetConfResult(netconf, show_interface, entry);
            addNetConfResult(netconf, show_interface_fex_fabric, entry);
            addNetConfResult(netconf, show_interface_status, entry);
            addNetConfResult(netconf, show_fex, entry);
            addNetConfResult(netconf, show_vpc, entry);
            netconf.close();
        }

        entry.close();
    }

    private void addConsoleResult(ConsoleAccess console, ConsoleCommand cmd, SimulationEntry entry)
            throws AbortedException, IOException, ConsoleException {
        String res = console.getResponse(cmd);
        entry.addConsoleResult(cmd, res);
    }

    private void addNetConfResult(NetConfAccess netconf, ConsoleCommand cmd, SimulationEntry entry)
            throws AbortedException, IOException, NetConfException {
        String res = netconf.getResponse(cmd.getCommand());
        entry.addNetConfResult(cmd.getCommand(), res);
    }
}