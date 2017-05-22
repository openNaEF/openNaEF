package voss.discovery.agent.juniper;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.HostResourceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.mib.OspfMibImpl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.agent.util.DiscoveryUtils;
import voss.discovery.constant.DiscoveryParameterType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.NullMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.*;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JuniperJunosDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private static final Logger log = LoggerFactory.getLogger(JuniperJunosDiscovery.class);
    private final Map<String, String> jnxChassisDef = new HashMap<String, String>();

    private final MplsVlanDevice device;
    private final SnmpAccess snmp;
    private final MplsModelBuilder builder;
    private final Mib2Impl mib2;
    private final MplsTeStdMibImpl mplsTeStdMib;
    private final JuniperMibImpl juniperMib;
    private final JuniperIfMibImpl juniperIfMib;
    private final MplsMibImpl mplsMib;
    private final JuniperVpnMibImpl juniperVpnMib;
    private final ApsMibImpl apsMib;

    private String textConfiguration;
    private ConfigurationStructure config;
    private final AgentConfiguration agentConfig;

    private final Map<Port, String> vpnAddressMap = new HashMap<Port, String>();

    private ConsoleCommand show_configuration = new ConsoleCommand(new NullMode(), "show configuration");
    private ConsoleCommand show_configuration_nomore = new ConsoleCommand(new NullMode(), "show configuration | no-more");
    private ConsoleCommand show_interface = new ConsoleCommand(new NullMode(), "show interface");

    private final String OPTION_NOMORE = "no-more";

    public JuniperJunosDiscovery(DeviceAccess access) {
        super(access);
        this.agentConfig = AgentConfiguration.getInstance();
        this.device = new MplsVlanDevice();
        this.snmp = access.getSnmpAccess();
        this.builder = new MplsModelBuilder(this.device);
        this.device.setIpAddress(getDeviceAccess().getTargetAddress().getHostAddress());
        this.jnxChassisDef.putAll(agentConfig.getDiscoveryParameter(DiscoveryParameterType.JUNIPER_ENTITY_MAP));
        this.mib2 = new Mib2Impl(snmp);
        this.mplsTeStdMib = new MplsTeStdMibImpl(snmp, device);
        this.juniperMib = new JuniperMibImpl(snmp, device);
        this.juniperIfMib = new JuniperIfMibImpl(snmp, device);
        this.mplsMib = new MplsMibImpl(snmp, device);
        this.juniperVpnMib = new JuniperVpnMibImpl(snmp, device);
        this.apsMib = new ApsMibImpl(this);
    }

    public JuniperIfMibImpl getJuniperIfMib() {
        return juniperIfMib;
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
            NodeInfo nodeinfo = getDeviceAccess().getNodeInfo();
            if (nodeinfo != null && nodeinfo.hasOption(OPTION_NOMORE)) {
                log.debug("get configuration with no-more.");
                this.textConfiguration = console.getResponse(show_configuration_nomore);
            } else {
                this.textConfiguration = console.getResponse(show_configuration);
            }
            log.trace("got configuration: \r\n" + this.textConfiguration);
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        }
        setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
        return this.textConfiguration;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.TEXT_CONFIGURATION)) {
            getTextConfiguration();
        }
        SimpleJunosConfigurationParser parser = new SimpleJunosConfigurationParser(this.textConfiguration);
        parser.parse();
        this.config = parser.getConfigurationStructure();
        this.device.gainConfigurationExtInfo().put(
                ExtInfoNames.DEVICE_CONFIG_STRUCTURE, this.config);
    }

    @Override
    public Device getDeviceInner() {
        supplementRealIpAddress();
        return this.device;
    }

    @Override
    protected boolean needIpAddressSupplement() {
        return false;
    }

    private void supplementRealIpAddress() {
        Set<Port> vrfPorts = new HashSet<Port>();
        for (VrfInstance vrf : this.device.getVrfs()) {
            for (Port ac : vrf.getAttachmentPorts()) {
                vrfPorts.add(ac);
            }
        }
        for (Map.Entry<Port, String> entry : this.vpnAddressMap.entrySet()) {
            Port p = entry.getKey();
            String addrMask = entry.getValue();
            if (p == null || addrMask == null) {
                continue;
            } else if (vrfPorts.contains(p)) {
                continue;
            }

            try {
                CidrAddress address = DiscoveryUtils.toCidrAddress(addrMask);
                if (address != null) {
                    this.device.addIpAddressToPort(address, p);
                }
            } catch (Exception e) {
                log.debug("failed to parse ip-address: " + addrMask, e);
            }
        }
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {

        String sysName = Mib2Impl.getSysName(snmp);
        this.device.setDeviceName(sysName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        String sysContact = Mib2Impl.getSysContact(snmp);
        this.device.setContactInfo(sysContact);
        String sysDescr = Mib2Impl.getSysDescr(snmp);
        device.setVendorName(Constants.VENDOR_JUNIPER);
        device.setModelTypeName(this.juniperMib.getBoxDescr());
        device.setSerialNumber(this.juniperMib.getBoxSerialNo());
        device.setDescription(sysDescr);
        device.setCommunityRO(snmp.getCommunityString());
        device.setIpAddress(snmp.getSnmpAgentAddress().getAddress().getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(snmp).longValue()));

        setOs();

        mib2.setDefaultGateway(device);
        mib2.setIpAddresses(device);
        mib2.setSnmpTrapReceiverAddress(device);
        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    public void setOs() throws IOException, AbortedException {
        List<String> names = HostResourceMib.getHrSWInstalledName(getSnmpAccess());
        for (String name : names) {
            if (name.contains("JUNOS")) {
                device.setOsTypeName(OSType.JUNOS.caption);
                device.setOsVersion(getOsVersion(name));
                break;
            }
        }
    }

    final Pattern osPattern = Pattern.compile(".*\\[([0-9SR.]+)\\].*");

    private String getOsVersion(String s) {
        Matcher matcher = osPattern.matcher(s);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
        getDeviceInformation();
        getConfiguration();
        juniperMib.createSlotAndModule();
        juniperIfMib.createPhysicalPorts();
        juniperIfMib.setPortIfNames();
        createLogicalPorts();
        InterfaceMibImpl interfaceMib = new InterfaceMibImpl(snmp);
        for (PhysicalPort physical : device.getPhysicalPorts()) {
            try {
                interfaceMib.setIfOperStatus(physical);
            } catch (NotInitializedException e) {
                continue;
            }
        }
        for (LogicalPort logical : device.getLogicalPorts()) {
            try {
                interfaceMib.setIfOperStatus(logical);
            } catch (NotInitializedException e) {
                continue;
            }
        }

        getLsp();
        mplsMib.setStatus();

        getPseudoWire();
        getPseudoWire2();
        getBgpVpn();
        juniperVpnMib.setPseudoWireStatusAndRemoteAddress();
        juniperVpnMib.setLowerLayerLsp();
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        juniperMib.createSlotAndModule();
        juniperIfMib.createPhysicalPorts();
        juniperIfMib.setPortIfNames();
        juniperIfMib.setPortAttributes();

        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        if (this.config == null) {
            this.getConfiguration();
        }
        assert this.config != null;

        createLogicalPorts();
        setLogicalPortAttributes();

        OspfMibImpl.getOspfIfParameters(snmp, device);

        getLsp();
        mplsMib.setupLsp();
        getPseudoWire();
        getPseudoWire2();
        getBgpVpn();
        juniperVpnMib.setPseudoWireStatusAndRemoteAddress();

        getLocalSwitching();

        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    public int getVlanId(String ifName) {
        String[] ifNameSplit = ifName.split("\\.");
        return getVlanId(ifNameSplit[0], ifNameSplit[1]);
    }

    public Integer getVlanId(String physicalIfName, String unitName) {
        ConfigElement element = getInterfaceUnit(physicalIfName, unitName);
        if (element == null) {
            return null;
        }
        return getVlanId(element);
    }

    public Integer getOuterVlanId(String physicalIfName, String unitName) {
        ConfigElement element = getInterfaceUnit(physicalIfName, unitName);
        if (element == null) {
            return null;
        }
        return getOuterVlanId(element);
    }

    public ConfigElement getInterfaceUnit(String physicalIfName, String unitName) {
        String path = new ConfigElementPathBuilder()
                .append("interfaces")
                .append(physicalIfName)
                .append("unit " + unitName).toString();

        ConfigElement unitElement = config.getByPath(path);
        return unitElement;
    }

    public boolean isRegularVlan(ConfigElement unitElement) {
        return unitElement != null && unitElement.hasAttribute("vlan-id .*");
    }

    public boolean isTagStackVlan(ConfigElement unitElement) {
        return unitElement != null && unitElement.hasAttribute("vlan-tags outer [0-9]+ inner [0-9]+");
    }

    public Integer getVlanId(ConfigElement unitElement) {
        if (isRegularVlan(unitElement)) {
            return Integer.parseInt(unitElement.getAttributeValue("vlan-id (.*)"));
        }
        return null;
    }

    public Integer getInnerVlanId(ConfigElement unitElement) {
        if (isTagStackVlan(unitElement)) {
            return Integer.parseInt(unitElement.getAttributeValue("vlan-tags outer .* inner ([0-9]+)"));
        }
        return null;
    }

    public Integer getOuterVlanId(ConfigElement unitElement) {
        if (isTagStackVlan(unitElement)) {
            return Integer.parseInt(unitElement.getAttributeValue("vlan-tags outer ([0-9]+) inner .*"));
        }
        return null;
    }

    private void createLogicalPorts() throws IOException, AbortedException {

        String optionsPath = new ConfigElementPathBuilder()
                .append("interfaces")
                .append(".*")
                .append("gigether-options").toString();
        List<ConfigElement> optionsElementList = config.getsByPath(optionsPath);
        for (ConfigElement optionsElement : optionsElementList) {

            String ifName = optionsElement.getParent().getId();
            if (ifName.startsWith("inactive:")) {
                continue;
            }
            int ifIndex = juniperIfMib.getIfIndex(ifName);
            if (ifIndex == 0) {
                log.warn("no member-port[" + ifName + "] found on mib.");
                continue;
            }
            Port physical = device.getPortByIfIndex(ifIndex);

            IanaIfType ianaIfType = juniperIfMib.getIfType(ifIndex);
            log.debug("Interface '" + ifName + "' " + ianaIfType);

            String ae = optionsElement.getAttributeValue("802.3ad (.*)");
            if (ae != null) {
                addMemberToLAG(ae, physical);
                log.debug("@device '" + device.getDeviceName() + "'; add port ifName:" + ifName + " to LAG:" + ae + "';");
            }
        }
        apsMib.createAPS();

        createAtmVp();

        createSubInterface();

        EthernetPort[] ethPorts = this.device.selectPorts(EthernetPort.class);
        if (ethPorts != null) {
            for (EthernetPort eth : ethPorts) {
                createDefaultLogicalEthernetPort(eth);
            }
        }
    }

    private void createAtmVp() {
        String unitPath = new ConfigElementPathBuilder()
                .append("interfaces")
                .append(".*")
                .append("atm-options").toString();
        List<ConfigElement> unitElementList = config.getsByPath(unitPath);
        for (ConfigElement unitElement : unitElementList) {
            String ifName = unitElement.getParent().getId();
            log.debug("parent ifName=" + ifName);
            if (ifName.startsWith("inactive:")) {
                continue;
            }
            Port parentPort = this.device.getPortByIfName(ifName);
            if (parentPort == null) {
                throw new IllegalStateException("no parent port: " + ifName);
            }
            if (!AtmPort.class.isInstance(parentPort)) {
                throw new IllegalStateException("parent port is not atm-port: "
                        + parentPort.getFullyQualifiedName() + "(" + parentPort.getClass().getName() + ")"
                        + "\r\n" + unitElement.toString());
            }
            AtmPort atm = AtmPort.class.cast(parentPort);
            createAtmVp(atm, unitElement);
            List<ConfigElement> promiscuousElements = unitElement.getElementsById("promiscuous-mode");
            for (ConfigElement promiscuousElement : promiscuousElements) {
                createAtmVp(atm, promiscuousElement);
            }
        }
    }

    private void createAtmVp(AtmPort atm, ConfigElement e) {
        boolean isPromiscuous = e.getId().startsWith("promiscuous");
        List<String> vpiValues = e.getAttributes("vpi ([0-9]+)");
        for (String vpiValue : vpiValues) {
            vpiValue = vpiValue.replace("vpi ", "");
            int vpi = Integer.parseInt(vpiValue);
            AtmVp vp = atm.getVp(vpi);
            if (vp == null) {
                vp = new AtmVp(atm, vpi);
                vp.initIfName(atm.getIfName() + ".vp" + vpi);
            }
            if (isPromiscuous) {
                PromiscuousModeRenderer renderer = new PromiscuousModeRenderer(vp);
                renderer.set(Boolean.TRUE);
            }
        }
    }

    private void createSubInterface() throws IOException, AbortedException {
        String unitPath = new ConfigElementPathBuilder()
                .append("interfaces")
                .append(".*")
                .append("unit.*").toString();
        List<ConfigElement> unitElementList = config.getsByPath(unitPath);
        for (ConfigElement unitElement : unitElementList) {
            String ifName = unitElement.getParent().getId();
            if (ifName.startsWith("inactive:")) {
                continue;
            }
            int ifIndex = juniperIfMib.getIfIndex(ifName);

            String unitName = unitElement.getId().replace("unit ", "");
            String subIfName = ifName + "." + unitName;
            int subIfIndex = juniperIfMib.getIfIndex(subIfName);

            if (ifIndex == 0 || subIfIndex == 0) {
                log.debug("Skip '" + subIfName + "' ifIndex:" + ifIndex + " subIfIndex:" + subIfIndex + "");
                continue;
            }
            IanaIfType subIanaIfType = juniperIfMib.getIfType(subIfIndex);

            Port physical = device.getPortByIfIndex(ifIndex);
            IanaIfType ianaIfType = juniperIfMib.getIfType(ifIndex);

            Port port = device.getPortByIfIndex(subIfIndex);
            if (port != null) {
                device.removePort(port);
                log.debug("@device '" + device.getDeviceName() + "'; remove ifName:" + port.getIfName() + " ifIndex:" + subIfIndex + ";");
            }
            Port subIf = null;
            if (ianaIfType == IanaIfType.softwareLoopback) {
                subIf = createLoopbackSubInterface(subIfIndex, subIfName);
            } else if (ianaIfType == IanaIfType.ethernetCsmacd) {
                subIf = createEthernetSubInterface(unitElement, (EthernetPort) physical, ifName, subIfIndex, subIfName);
            } else if (ianaIfType == IanaIfType.ieee8023adLag) {
                int groupId = Integer.parseInt(ifName.replace("ae", ""));
                EthernetPortsAggregator aggregator = device.getEthernetPortsAggregatorByAggregationGroupId(groupId);
                if (aggregator == null) {
                    log.debug("'" + ifName + "' does not exist.");
                    continue;
                }
                subIf = createEthernetLagSubInterface(aggregator, unitElement, ifIndex, ifName, ianaIfType, subIfName, subIfIndex);
            } else if (ianaIfType == IanaIfType.atm) {
                AtmPort atm = (AtmPort) physical;
                subIf = createAtmSubInterface(unitElement, atm, ifName, subIfIndex, subIfName, subIanaIfType);
            } else if (ifName.matches("ci[0-9]+")) {
                AtmPort atm = (AtmPort) physical;
                subIf = createAtmApsSubInterface(unitElement, atm, ifName, subIfIndex, subIfName, subIanaIfType);
            } else {
                log.debug("Skip interface '" + subIfName + "' " + subIanaIfType);
            }
            discoverVpnIpAddress(subIf, unitElement);
        }
    }

    private void discoverVpnIpAddress(Port port, ConfigElement unitElement) {
        if (port == null) {
            return;
        }
        ConfigElement inetElement = unitElement.getElementById("family inet");
        if (inetElement == null) {
            return;
        }
        String address = inetElement.getAttributeValue("address ([0-9a-f.:/]+)");
        if (address == null) {
            return;
        }
        this.vpnAddressMap.put(port, address);
    }

    private Port createLoopbackSubInterface(int subIfIndex, String subIfName) {
        LoopbackInterface lo = new LoopbackInterface();
        lo.initDevice(device);
        lo.initIfIndex(subIfIndex);
        lo.initIfName(subIfName);
        log.debug("@device '" + device.getDeviceName() + "'; add ifName:" + subIfName + " ifIndex:" + subIfIndex + ";");
        return lo;
    }

    private Port createEthernetSubInterface(ConfigElement unitElement, EthernetPort ether, String ifName, int subIfIndex,
                                            String subIfName) {
        Integer vlanId = getVlanId(unitElement);
        if (vlanId != null && vlanId.intValue() > 0) {
            RouterVlanIf vlanIf = new RouterVlanIfImpl();
            vlanIf.initDevice(device);
            vlanIf.initIfIndex(subIfIndex);
            vlanIf.initIfName(subIfName);
            vlanIf.initVlanId(vlanId);
            LogicalEthernetPort logical = device.getLogicalEthernetPort(ether);
            if (logical == null) {
                logical = new DefaultLogicalEthernetPortImpl();
                logical.initDevice(device);
                logical.initIfName("[logical]" + ifName);
                ((DefaultLogicalEthernetPort) logical).initPhysicalPort(ether);
                log.debug("@device '" + device.getDeviceName() + "'; add ifName:" + logical.getIfName() + " ifIndex:-;");
            }
            vlanIf.initRouterPort(logical);
            vlanIf.addTaggedPort(logical);
            log.debug("@device '" + device.getDeviceName() + "'; add ifName:" + vlanIf.getIfName() + " ifIndex:" + subIfIndex + ";");
            return vlanIf;
        } else {
            LogicalEthernetPort logical = new DefaultLogicalEthernetPortImpl();
            logical.initDevice(device);
            logical.initIfIndex(subIfIndex);
            logical.initIfName(subIfName + ".0");
            ((DefaultLogicalEthernetPort) logical).initPhysicalPort(ether);
            log.debug("@device '" + device.getDeviceName() + "'; add ifName:" + logical.getIfName() + " ifIndex:" + subIfIndex + ";");
            return logical;
        }
    }

    private Port createEthernetLagSubInterface(EthernetPortsAggregator aggregator, ConfigElement unitElement,
                                               int ifIndex, String ifName, IanaIfType ianaIfType, String subIfName, int subIfIndex) {
        if (isRegularVlan(unitElement)) {
            Integer vlanId = getVlanId(unitElement);
            RouterVlanIf vlanIf = new RouterVlanIfImpl();
            vlanIf.initDevice(device);
            vlanIf.initIfIndex(subIfIndex);
            vlanIf.initIfName(subIfName);
            vlanIf.initVlanId(vlanId);

            if (aggregator.getIfName() == null) {
                aggregator.initIfIndex(ifIndex);
                aggregator.initIfName(ifName);
                log.debug("@device '" + device.getDeviceName() + "'; init lag '" + ifName + "';");
            }
            vlanIf.initRouterPort(aggregator);
            vlanIf.addTaggedPort(aggregator);
            log.debug("@device '" + device.getDeviceName() + "'; add '" + vlanIf.getIfName() + "' (" + subIfIndex + ");");
            return vlanIf;
        } else if (isTagStackVlan(unitElement)) {
            Integer outerVlanId = getOuterVlanId(unitElement);
            Integer innerVlanId = getInnerVlanId(unitElement);
            log.debug("@device '" + device.getDeviceName() + "'; skip tag-stack '" + outerVlanId + "." + innerVlanId + "' (" + subIfIndex + ");");
            return null;
        } else {
            log.debug("unitElement: \r\n" + unitElement.toString());
            if (aggregator.getIfName() == null) {
                aggregator.initIfIndex(subIfIndex);
                aggregator.initIfName(subIfName);
                log.debug("@device '" + device.getDeviceName() + "'; init lag '" + subIfName + "';");
                return null;
            } else if (!aggregator.getIfName().equals(subIfName)) {
                GenericLogicalPort associated = new GenericLogicalPort();
                associated.initDevice(this.device);
                associated.initIfIndex(subIfIndex);
                associated.initIfName(subIfName);
                associated.setAssociatePort(aggregator);
                aggregator.setAssociatedPort(associated);
                return associated;
            } else {
                try {
                    int lagIfIndex = aggregator.getIfIndex();
                    if (lagIfIndex != subIfIndex) {
                        aggregator.initAlternativeIfIndex(subIfIndex);
                        log.debug("@device '" + device.getDeviceName() + "'; init-alt-ifindex lag " +
                                "ifIndex " + aggregator.getIfIndex() + ";" + subIfIndex +
                                " '" + aggregator.getIfName() + ";" + subIfName + "';");
                    }
                } catch (NotInitializedException e) {
                    aggregator.initIfIndex(subIfIndex);
                    log.debug("@device '" + device.getDeviceName() + "'; init-ifindex lag " +
                            "ifIndex " + subIfIndex + " '" + aggregator.getIfName() + "';");
                } catch (Exception e) {
                }
                return null;
            }
        }
    }

    private Port createAtmSubInterface(ConfigElement unitElement, AtmPort atm, String ifName, int subIfIndex, String subIfName,
                                       IanaIfType subIanaIfType) {
        if (unitElement.hasAttribute("vpi (.*)")) {
            String vpiString = unitElement.getAttributeValue("vpi (.*)");
            int vpi = Integer.parseInt(vpiString);
            AtmVp atmVp = atm.getVp(vpi);
            if (atmVp == null) {
                atmVp = new AtmVp(atm, vpi);
            }
            atmVp.initIfIndex(subIfIndex);
            atmVp.initIfName(subIfName);
            setAtmVpShaping(atmVp, unitElement);
            log.debug("@device '" + device.getDeviceName() + "'; add  '" + subIfName + "';");
            return atmVp;
        } else if (unitElement.hasAttribute("vci (.*)")) {
            String[] vciStringSplit = unitElement.getAttributeValue("vci (.*)").split("\\.");
            int vpi = Integer.parseInt(vciStringSplit[0]);
            int vci = Integer.parseInt(vciStringSplit[1]);

            AtmVp atmVp = atm.getVp(vpi);
            if (atmVp == null) {
                atmVp = new AtmVp(atm, vpi);
                atmVp.initDevice(device);
                atmVp.initIfName(ifName + ".vp" + vpi);
            }
            setAtmVpShaping(atmVp, unitElement);
            AtmPvc atmPvc = new AtmPvc(atmVp, vci);
            atmPvc.initDevice(device);
            atmPvc.initIfIndex(subIfIndex);
            atmPvc.initIfName(subIfName);
            setAtmPvcShaping(atmPvc, unitElement);
            log.debug("@device '" + device.getDeviceName() + "'; add  '" + subIfName + "';");
            return atmPvc;
        } else {
            log.debug("Skip interface '" + subIfName + "' " + subIanaIfType);
            return null;
        }
    }

    private Port createAtmApsSubInterface(ConfigElement unitElement, AtmPort atm, String ifName, int subIfIndex,
                                          String subIfName, IanaIfType subIanaIfType) {
        String encap = unitElement.getParent().getAttributeValue("encapsulation (.*)");
        if (encap.equals("atm-pvc")) {
            if (unitElement.hasAttribute("vpi (.*)")) {
                String vpiString = unitElement.getAttributeValue("vpi (.*)");
                int vpi = Integer.parseInt(vpiString);
                AtmVp atmVp = atm.getVp(vpi);
                if (atmVp == null) {
                    atmVp = new AtmVp(atm, vpi);
                    atmVp.initDevice(device);
                }
                atmVp.initIfIndex(subIfIndex);
                atmVp.initIfName(subIfName);
                setAtmVpShaping(atmVp, unitElement);
                log.debug("@device '" + device.getDeviceName() + "'; add  '" + subIfName + "';");
                return atmVp;
            } else if (unitElement.hasAttribute("vci (.*)")) {
                String[] vciStringSplit = unitElement.getAttributeValue("vci (.*)").split("\\.");
                int vpi = Integer.parseInt(vciStringSplit[0]);
                int vci = Integer.parseInt(vciStringSplit[1]);

                AtmVp atmVp = atm.getVp(vpi);
                if (atmVp == null) {
                    atmVp = new AtmVp(atm, vpi);
                    atmVp.initIfName(ifName + ".vp" + vpi);
                }
                setAtmVpShaping(atmVp, unitElement);
                AtmPvc atmPvc = new AtmPvc(atmVp, vci);
                atmPvc.initDevice(device);
                atmPvc.initIfIndex(subIfIndex);
                atmPvc.initIfName(subIfName);
                setAtmPvcShaping(atmPvc, unitElement);
                log.debug("@device '" + device.getDeviceName() + "'; add  '" + subIfName + "';");
                return atmPvc;
            } else {
                log.debug("Skip interface '" + subIfName + "' " + subIanaIfType);
                return null;
            }
        } else {
            log.debug("Skip interface '" + subIfName + "' " + subIanaIfType);
            return null;
        }
    }

    private void setLogicalPortAttributes() throws IOException, AbortedException {
        InterfaceMibImpl interfaceMib = new InterfaceMibImpl(snmp);
        for (LogicalPort logical : device.getLogicalPorts()) {
            try {
                interfaceMib.setIfOperStatus(logical);
                interfaceMib.setIfAdminStatus(logical);
                interfaceMib.setIfSpeed(logical);
                interfaceMib.setIfType(logical);
                interfaceMib.setIfAlias(logical);
                interfaceMib.setIfDescription(logical);
            } catch (NotInitializedException e) {
                continue;
            }
        }
    }

    private void createDefaultLogicalEthernetPort(EthernetPort eth) throws IOException, AbortedException {
        LogicalEthernetPort le = device.getLogicalEthernetPort(eth);
        if (le == null) {
            String ifName = eth.getIfName() + ".0";
            DefaultLogicalEthernetPortImpl dle = new DefaultLogicalEthernetPortImpl();
            dle.initDevice(device);
            dle.initIfName(ifName);
            dle.initPhysicalPort(eth);
            log.debug("@device '" + device.getDeviceName() + "'; create default-logical-eth ifName:"
                    + ifName + " for ifName:" + eth.getIfName() + ";");
        }
    }

    private void addMemberToLAG(String lagID, Port physical) {
        int groupId = Integer.parseInt(lagID.replace("ae", ""));
        EthernetPortsAggregator aggregator = device.getEthernetPortsAggregatorByAggregationGroupId(groupId);
        if (aggregator == null) {
            aggregator = new EthernetPortsAggregatorImpl();
            aggregator.initDevice(device);
            aggregator.initAggregationGroupId(groupId);
            aggregator.initIfName(lagID);
            aggregator.setAggregationName(lagID);
            try {
                int ifIndex = juniperIfMib.getIfIndex(lagID);
                aggregator.initIfIndex(ifIndex);
            } catch (Exception e) {
            }
            log.debug("@device '" + device.getDeviceName() + "'; create lag groupId '" + groupId + "';");
        }
        aggregator.addPhysicalPort((EthernetPort) physical);
    }

    private void setAtmPvcShaping(AtmPvc atmPvc, ConfigElement unitElement) {

        ConfigElement shapingElement = unitElement.getElementById("shaping");
        if (shapingElement != null) {
            if (shapingElement.hasAttribute("cbr .*")) {
                String pcrString = shapingElement.getAttributeValue("cbr (.*)");
                long pcr = 0L;
                if (pcrString.endsWith("m")) {
                    pcr = Long.parseLong(pcrString.replace("m", "")) * 1000L * 1000L;
                } else if (pcrString.endsWith("k")) {
                    pcr = Long.parseLong(pcrString.replace("k", "")) * 1000L;
                } else if (pcrString.matches("[0-9]+")) {
                    pcr = Long.parseLong(pcrString);
                } else {
                    log.warn("unexpected pattern: " + pcrString);
                }
                atmPvc.setAtmQos(AtmQosType.CBR);
                atmPvc.setPcr(pcr);
                atmPvc.setBandwidth(pcr);
            }
        }
    }

    private void setAtmVpShaping(AtmVp atmVp, ConfigElement unitElement) {
        log.debug("setAtmVpShaping: " + atmVp.getFullyQualifiedName());
        if (atmVp.getPcr() != null) {
            return;
        }
        ConfigElement shapingElement = unitElement.getElementById("shaping");
        if (shapingElement != null) {
            if (shapingElement.hasAttribute("cbr .*")) {
                String pcrString = shapingElement.getAttributeValue("cbr (.*)");
                long pcr = 0L;
                if (pcrString.endsWith("m")) {
                    pcr = Long.parseLong(pcrString.replace("m", "")) * 1000L * 1000L;
                } else if (pcrString.endsWith("k")) {
                    pcr = Long.parseLong(pcrString.replace("k", "")) * 1000L;
                } else if (pcrString.matches("[0-9]+")) {
                    pcr = Long.parseLong(pcrString);
                } else {
                    log.warn("unexpected pattern: " + pcrString);
                }
                atmVp.setBandwidth(pcr);
                atmVp.setPcr(pcr);
            }
        }

        String shapingPath = new ConfigElementPathBuilder()
                .append(unitElement.getParent().getCurrentPath())
                .append("atm-options")
                .append("vpi " + atmVp.getVpi())
                .append("shaping").toString();
        shapingElement = config.getByPath(shapingPath);
        if (shapingElement != null) {
            if (shapingElement.hasAttribute("cbr .*")) {
                String pcrString = shapingElement.getAttributeValue("cbr (.*)");
                long pcr = 0L;
                if (pcrString.endsWith("m")) {
                    pcr = Long.parseLong(pcrString.replace("m", "")) * 1000L * 1000L;
                } else if (pcrString.endsWith("k")) {
                    pcr = Long.parseLong(pcrString.replace("k", "")) * 1000L;
                } else if (pcrString.matches("[0-9]+")) {
                    pcr = Long.parseLong(pcrString);
                } else {
                    log.warn("unexpected pattern: " + pcrString);
                }
                if (atmVp.getPcr() > 0 && atmVp.getPcr() > pcr) {
                    atmVp.setPcr(pcr);
                }
            }
        }
    }

    private void getLsp() throws IOException, AbortedException {

        String lspPath = new ConfigElementPathBuilder()
                .append("protocols")
                .append("mpls")
                .append("label-switched-path .*").toString();

        List<ConfigElement> lspElementList = this.config.getsByPath(lspPath);
        log.debug("found lsp(s): " + lspElementList.size());
        for (ConfigElement lspElement : lspElementList) {
            String lspName = lspElement.getId().replace("label-switched-path ", "");

            MplsTunnel mplsTunnel = new MplsTunnel();
            mplsTunnel.initDevice(device);
            device.addTeTunnel(mplsTunnel);
            mplsTunnel.initIfName(lspName);
            log.debug("Add MplsTunnel " + lspName);

            List<String> lines = lspElement.getAttributes("primary.*|secondary.*");
            for (ConfigElement elem : lspElement.getElementsByAttribute("primary.*|secondary.*")) {
                lines.add(elem.getId());
            }

            for (String line : lines) {
                boolean isPrimary = line.startsWith("primary ");
                String pathName = line.replaceAll("^primary |^secondary ", "");
                if (isPrimary) {
                    log.debug("\tprimary=" + pathName);
                } else {
                    log.debug("\tsecondary=" + pathName);
                }

                LabelSwitchedPathEndPoint lsp = new LabelSwitchedPathEndPoint();

                lsp.initDevice(device);
                lsp.initIfName(lspName + "::" + pathName);
                mplsTunnel.addMemberLsp(mplsTunnel.getMemberLsps().size(), lsp);
                lsp.setLspName(lspName + "::" + pathName);
                lsp.setBandwidth(0L);

                mplsTeStdMib.setupMplsTunnel(lspName, pathName, isPrimary);
            }
        }
    }

    public void getPseudoWire() throws IOException, AbortedException {
        String pwPath = new ConfigElementPathBuilder()
                .append(JuniperJunosCommand.KEY_PROTOCOLS)
                .append(JuniperJunosCommand.KEY_L2CIRCUIT)
                .append("neighbor.*")
                .append("interface.*")
                .toString();

        List<ConfigElement> l2circuits = this.config.getsByPath(pwPath);
        log.trace("found l2circuit(s): " + l2circuits.size());
        for (ConfigElement l2circuit : l2circuits) {
            log.debug(l2circuit.toString());
            String from = l2circuit.getId().replace("interface ", "");
            String parentIfName = null;
            String subId = null;
            if (JuniperJunosCommandParseUtil.isSubInterface(from)) {
                parentIfName = JuniperJunosCommandParseUtil.getParentInterfaceName(from);
                subId = JuniperJunosCommandParseUtil.getSubInterfaceId(from);
            } else {
                parentIfName = from;
            }

            String neighbor = l2circuit.getParent().getId().replace("neighbor ", "");
            int pwID = Integer.parseInt(l2circuit.getAttributeValue("virtual-circuit-id (.*)"));
            String pwDescription = l2circuit.getAttributeValue("description (.*)");
            boolean pwControlWord = (l2circuit.getAttribute("control-word") != null);

            log.debug("PseudoWire id:" + pwID);
            log.debug("\tparent: " + parentIfName);
            log.debug("\tsub id: " + subId);
            log.debug("\tneighbor: " + neighbor);
            log.debug("\tdescription: " + pwDescription);
            log.debug("\tcontrol-word: " + pwControlWord);

            this.builder.buildPseudoWire(pwID);
            this.builder.setPseudoWireName(pwID, pwDescription);
            this.builder.setPseudoWirePeerAddress(pwID,
                    InetAddress.getByAddress(
                            VossMiscUtility.getByteFormIpAddress(neighbor)));
            PseudoWirePortImpl pw = (PseudoWirePortImpl) device.getPseudoWirePortByPwId(pwID);
            pw.setControlWord(pwControlWord);

            Port parent = this.device.getPortByIfName(parentIfName);
            if (parent == null) {
                throw new IllegalArgumentException("port not found: " + parentIfName);
            }
            if (!(parent instanceof EthernetPort)) {
                throw new IllegalArgumentException();
            }
            LogicalEthernetPort logical = device.getLogicalEthernetPort((EthernetPort) parent);

            int vlanId = 0;
            if (subId != null) {
                try {
                    vlanId = Integer.parseInt(subId);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("illegal sub-id: " + subId);
                }
            }

            if (vlanId > 0) {
                this.builder.setPseudoWireType(pwID, PseudoWireType.ethernetVLAN);
                this.builder.buildVlanConnection(pwID, vlanId, logical);

            } else {
                this.builder.setPseudoWireType(pwID, PseudoWireType.ethernet);
                this.builder.buildDirectConnection(pwID, logical);
            }
        }
    }

    Pattern pwdIDPattern = Pattern.compile("remote-interface-switch (.*)");
    Pattern intPwdIDPattern = Pattern.compile(".*([0-9]+).*");

    public void getPseudoWire2() throws IOException, AbortedException {
        String pwPath = new ConfigElementPathBuilder()
                .append(JuniperJunosCommand.KEY_PROTOCOLS)
                .append(JuniperJunosCommand.KEY_CONNECTIONS)
                .append("remote-interface-switch .*")
                .toString();

        List<ConfigElement> remoteInterfaces = this.config.getsByPath(pwPath);
        log.trace("found remote-interface-switch(s): " + remoteInterfaces.size());
        for (ConfigElement remoteInterface : remoteInterfaces) {
            log.debug(remoteInterface.toString());
            String from = remoteInterface.getAttributeValue("interface (.*)");
            String parentIfName = null;
            String subId = null;
            if (JuniperJunosCommandParseUtil.isSubInterface(from)) {
                parentIfName = JuniperJunosCommandParseUtil.getParentInterfaceName(from);
                subId = JuniperJunosCommandParseUtil.getSubInterfaceId(from);
            } else {
                parentIfName = from;
            }

            String pwID_ = null;
            int pwID = 0;
            Matcher matcher = pwdIDPattern.matcher(remoteInterface.getId());
            if (matcher.matches()) {
                pwID_ = matcher.group(1);
                Matcher matcher2 = intPwdIDPattern.matcher(pwID_);
                if (matcher2.matches()) {
                    pwID = Integer.parseInt(matcher2.group(1));
                    pwID = 0 - pwID;
                }
            }
            this.builder.buildPseudoWire(pwID);
            String transmitLspName = remoteInterface.getAttributeValue("transmit-lsp (.*)");
            log.debug("transmit-lsp: " + transmitLspName);
            MplsTunnel lsp = (MplsTunnel) device.getPortByIfName(transmitLspName);
            log.debug("* " + lsp);
            String receiveLspName = remoteInterface.getAttribute("receive-lsp (.*)");
            log.debug("receive-lsp: " + receiveLspName);
            this.builder.setPseudoWireTransmitLsp(pwID, lsp);
            this.builder.setPseudoWirePeerAddress(pwID, InetAddress.getByName(lsp.getTo()));
            this.builder.setPseudoWireReceiveLspName(pwID, receiveLspName);
            Port parent = this.device.getPortByIfName(parentIfName);
            if (parent == null) {
                throw new IllegalArgumentException("port not found: " + parentIfName);
            }
            if (!(parent instanceof EthernetPort)) {
                throw new IllegalArgumentException();
            }
            LogicalEthernetPort logical = device.getLogicalEthernetPort((EthernetPort) parent);

            int vlanId = 0;
            if (subId != null) {
                try {
                    vlanId = Integer.parseInt(subId);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("illegal sub-id: " + subId);
                }
            }

            if (vlanId > 0) {
                this.builder.setPseudoWireType(pwID, PseudoWireType.ethernetVLAN);
                this.builder.buildVlanConnection(pwID, vlanId, logical);

            } else {
                this.builder.setPseudoWireType(pwID, PseudoWireType.ethernet);
                this.builder.buildDirectConnection(pwID, logical);
            }
        }
    }

    public void getBgpVpn() throws IOException, AbortedException {
        String lspPolicyPath = new ConfigElementPathBuilder()
                .append(JuniperJunosCommand.KEY_POLICY_OPTIONS)
                .append(JuniperJunosCommand.KEY_POLICY_LSP_SELECTION)
                .append("term .*")
                .toString();
        List<ConfigElement> lspPolicies = this.config.getsByPath(lspPolicyPath);
        log.debug("found lspPolicy: " + lspPolicies.size());
        for (ConfigElement lspPolicy : lspPolicies) {
            String id = lspPolicy.getId();
            log.debug("- testing: " + id);
            String termNumber = id.substring("term".length()).trim();
            ConfigElement then = lspPolicy.getElementById("then");
            if (then == null) {
                continue;
            }
            String lspNames = then.getAttributeValue("install-nexthop strict lsp (.*)");
            if (lspNames == null) {
                continue;
            }
            String[] lspNameList = lspNames.split(" ");
            for (String lspName : lspNameList) {
                Port lsp_ = this.device.getPortByIfName(lspName);
                if (lsp_ == null) {
                    log.warn("- [" + lspName + "] not found.");
                    continue;
                } else if (!MplsTunnel.class.isInstance(lsp_)) {
                    log.warn("- [" + lspName + "] is not lsp object: " + lsp_.getClass().getName());
                    continue;
                }
                MplsTunnel lsp = (MplsTunnel) lsp_;
                log.debug(" lsp found: " + lspName);
                @SuppressWarnings("unchecked")
                List<String> termNumbers = (ArrayList<String>) lsp.gainConfigurationExtInfo().get(JuniperJunosExtInfoNames.LSP_TERM_NUMBER);
                if (termNumbers == null) {
                    termNumbers = new ArrayList<String>();
                    lsp.gainConfigurationExtInfo().put(JuniperJunosExtInfoNames.LSP_TERM_NUMBER, termNumbers);
                }
                if (!termNumbers.contains(termNumber)) {
                    termNumbers.add(termNumber);
                }
            }
        }

        String pwPath = new ConfigElementPathBuilder()
                .append(JuniperJunosCommand.KEY_ROUTING_INSTANCE)
                .append(".*")
                .toString();

        List<ConfigElement> routingInstances = this.config.getsByPath(pwPath);
        log.debug("found routingInstance(s): " + routingInstances.size());
        for (ConfigElement routingInstance : routingInstances) {
            log.debug("testing: " + routingInstance.getId());
            if (routingInstance.getId().startsWith("inactive:")) {
                continue;
            }
            if (!routingInstance.hasAttribute(JuniperJunosCommand.ATTR_INSTANCE_TYPE)) {
                continue;
            }
            String instanceType = routingInstance.getAttributeValue(JuniperJunosCommand.ATTR_INSTANCE_TYPE);
            if (!instanceType.toLowerCase().matches("l2vpn|vpls|vrf")) {
                log.debug(" Unknown instance-type '" + instanceType + "'");
                continue;
            }
            String rd = routingInstance.getAttributeValue("route-distinguisher (.*)");
            String rt = routingInstance.getAttributeValue("vrf-target (.*)");
            if (rt != null) {
                rt = rt.replace("target:", "");
            }

            String instanceName = routingInstance.getId();
            if (instanceType.equals("l2vpn")) {

                BgpVpnPseudoWirePortImpl pw = new BgpVpnPseudoWirePortImpl();
                pw.initDevice(device);
                pw.setPwName(instanceName);
                pw.setAdminStatus("up");
                pw.setOperationalStatus(juniperVpnMib.getOperationalStatus(instanceName));
                pw.setRouteDistinguisher(rd);
                pw.setRouteTarget(rt);

                ConfigElement protocols = routingInstance.getElementById("protocols");
                if (protocols == null) {
                    continue;
                }
                ConfigElement l2vpn = protocols.getElementById("l2vpn");
                String encapsulation = l2vpn.getAttributeValue("encapsulation-type (.*)");
                ConfigElement site = l2vpn.getElementById("site .*");
                String siteName = site.getId();
                if (siteName != null) {
                    siteName = siteName.replace("site", "").trim();
                }
                String interfaceName = routingInstance.getAttributeValue("interface (.*)");
                log.debug(" found bgp-type-pw: " + instanceName + "/"
                        + interfaceName + "/" + rd + "/" + encapsulation + "/" + siteName);
                PseudoWireType type = null;
                if (encapsulation != null) {
                    if (encapsulation.equals("atm-aal5")) {
                        type = PseudoWireType.atmAal5Vcc;
                    } else if (encapsulation.equals("atm-cell-vp-mode")) {
                        type = PseudoWireType.atmVpcCell;
                    } else if (encapsulation.equals("atm-cell-vc-mode")) {
                        type = PseudoWireType.atmVccCell;
                    }
                }
                pw.setPseudoWireType(type);
                pw.setSiteName(siteName);
                pw.setControlWord(routingInstance.getAttribute("control-word") != null);

                Port ac = device.getPortByIfIndex(juniperIfMib.getIfIndex(interfaceName));
                if (ac != null) {
                    pw.setAttachedCircuitPort(ac);
                    log.debug(" attachment-circuit found: " + ac.getIfName() + "");
                } else {
                    log.debug(" Interface '" + interfaceName + "' is not defined.");
                }
            } else if (instanceType.equals("vpls")) {
                VplsInstanceImpl vpls = new VplsInstanceImpl();
                vpls.initDevice(device);
                vpls.initIfName(instanceName);
                vpls.initVplsID(instanceName);
                vpls.setAdminStatus("up");
                vpls.setOperationalStatus(juniperVpnMib.getOperationalStatus(instanceName));
                log.debug("@" + this.device.getDeviceName() + " new-port vpls " + instanceName);

                for (String attribute : routingInstance.getAttributes("interface .*")) {
                    String interfaceName = attribute.replace("interface ", "");
                    Port ap = device.getPortByIfIndex(juniperIfMib.getIfIndex(interfaceName));
                    if (ap != null) {
                        vpls.addAttachmentPort(ap);
                        log.debug(" attachment-port found: " + ap.getIfName() + "");
                    } else {
                        log.debug(" Interface '" + interfaceName + "' is not defined.");
                    }
                }
            } else if (instanceType.equals("vrf")) {
                VrfInstanceImpl vrf = new VrfInstanceImpl();
                vrf.initDevice(device);
                vrf.initIfName(instanceName);
                vrf.initVrfID(instanceName);
                vrf.setAdminStatus("up");
                vrf.setOperationalStatus(juniperVpnMib.getOperationalStatus(instanceName));
                log.debug("@" + this.device.getDeviceName() + " new-port vrf " + instanceName);

                for (String attribute : routingInstance.getAttributes("interface .*")) {
                    String interfaceName = attribute.replace("interface ", "");
                    Port ap = device.getPortByIfIndex(juniperIfMib.getIfIndex(interfaceName));
                    if (ap != null) {
                        vrf.addAttachmentPort(ap);
                        log.debug("@" + this.device.getDeviceName() + " vrf " + instanceName
                                + " add attachment-port " + interfaceName);
                        String address = this.vpnAddressMap.get(ap);
                        if (address != null) {
                            try {
                                CidrAddress cidr = DiscoveryUtils.toCidrAddress(address);
                                vrf.addVpnIpAddress(ap, cidr);
                                log.debug("@" + this.device.getDeviceName() + " vrf "
                                        + instanceName + " port " + interfaceName + " set ip-address " + address + "");
                            } catch (Exception e) {
                                log.debug("failed to parse vpn-ip: " + address, e);
                            }
                        }
                    } else {
                        log.debug(" Interface '" + interfaceName + "' is not defined.");
                    }
                }
            } else {
                log.debug(" Skip routing-instance " + instanceName);
            }
        }
    }

    public static final String KEY_LOCAL_SWITCHING = "";

    private void getLocalSwitching() throws IOException, AbortedException {
        String pwPath = new ConfigElementPathBuilder()
                .append(JuniperJunosCommand.KEY_PROTOCOLS)
                .append(JuniperJunosCommand.KEY_L2CIRCUIT)
                .append(JuniperJunosCommand.KEY_LOCAL_SWITCHING)
                .append("interface .*")
                .toString();

        List<ConfigElement> l2circuits = this.config.getsByPath(pwPath);
        log.trace("found local l2circuit(s): " + l2circuits.size());
        for (ConfigElement l2circuit : l2circuits) {
            log.trace(l2circuit.toString());
            ConfigElement endInterface = l2circuit.getElementById(JuniperJunosCommand.KEY_END_INTERFACE);
            String from = l2circuit.getId().replace("interface ", "");
            String to = endInterface.getAttributeValue("interface (.*)");
            log.debug(" found local switching: " + from + "<->" + to);
            Port fromPort = this.device.getPortByIfName(from);
            Port toPort = this.device.getPortByIfName(to);
            if (fromPort == null || toPort == null) {
                log.warn("port not found:" + (fromPort == null ? "[" + from + "] not found." : "")
                        + (toPort == null ? "[" + to + "] not found." : ""));
                continue;
            }
            NodePipe<Port> pipe = new NodePipeImpl<Port>();
            pipe.initDevice(this.device);
            String name = "l2circuit:" + from + ":" + to;
            pipe.initIfName(name);
            pipe.setPipeName(name);
            pipe.setAttachmentCircuit1(fromPort);
            pipe.setAttachmentCircuit2(toPort);
            log.debug("@" + this.device.getDeviceName() + " new-port node-pipe '" + name + "'");
        }
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res1 = console.getResponse(show_configuration);
            entry.addConsoleResult(show_configuration, res1);

            String res2 = console.getResponse(show_interface);
            entry.addConsoleResult(show_interface, res2);

            console.close();
        }
        entry.close();
    }
}