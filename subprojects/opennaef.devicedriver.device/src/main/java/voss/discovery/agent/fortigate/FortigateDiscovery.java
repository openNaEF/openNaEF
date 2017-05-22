package voss.discovery.agent.fortigate;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.EntityMib;
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
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.discovery.utils.ListUtil;
import voss.model.*;
import voss.model.EthernetPort.Duplex;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.*;

public class FortigateDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(FortigateDiscovery.class);
    private final EntityMibAnalyzer entityMib;
    private final InterfaceMibImpl ifMib;
    private final FortigateEntityFactory factory;
    private String textConfiguration = null;
    private ConfigurationStructure config = null;

    private ConsoleCommand show_running_config = new ConsoleCommand(new GlobalMode(), "show");

    public FortigateDiscovery(DeviceAccess access)
            throws IOException, AbortedException {
        super(access);
        this.entityMib = new EntityMibAnalyzer(new HashMap<String, String>());
        this.ifMib = new InterfaceMibImpl(access.getSnmpAccess());
        int type = getLastSysObjectIDNumber();
        switch (type) {
            case FortigateDeviceType.FG1000C:
                this.factory = new Fortigate1000CEntityFactory();
                break;
            case FortigateDeviceType.FG1240B:
                this.factory = new Fortigate1240BEntityFactory();
                break;
            default:
                log.debug("unknown device type: Fortigate1240BPortFactory is selected as default factory.");
                this.factory = new Fortigate1240BEntityFactory();
                break;
        }
    }

    private MplsVlanDevice device;

    @Override
    public Device getDeviceInner() {
        return this.device;
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        device = new MplsVlanDevice();
        String fqn = getDeviceFqn();
        String hostname = getHostNamePart(fqn);
        String domainName = getDomainNamePart(fqn);
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_FORTINET);
        device.setOsTypeName(OSType.FORTIGATE.caption);
        device.setDeviceName(hostname);
        device.setDomainName(domainName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getSnmpAccess().getCommunityString());
        device.setIpAddress(getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getSnmpAccess()).longValue()));

        if (this.config == null) {
            try {
                this.getConfiguration();
            } catch (Exception e) {
                throw new IOException("Cannot get configuration.", e);
            }
        }

        String serial = SnmpUtil.getString(getDeviceAccess().getSnmpAccess(),
                EntityMib.EntityMibEntPhysicalSerialNumEntry.OID + ".1", "chassis serial number");
        if (serial != null) {
            device.setSerialNumber(serial);
        }
        String osVersion = SnmpUtil.getString(getDeviceAccess().getSnmpAccess(),
                ".1.3.6.1.4.1.12356.101.4.1.1.0", "os version");
        if (osVersion != null) {
            device.setOsVersion(osVersion);
        }
        String modelType = SnmpUtil.getString(getDeviceAccess().getSnmpAccess(),
                EntityMib.EntityMibEntPhysicalModelNameEntry.OID + ".1", "model type name");
        if (modelType != null) {
            device.setModelTypeName(modelType);
        }
        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
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
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
        createLag();
        setPortAttributes();
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    protected void createPhysicalModel(PhysicalEntry entry, VlanModel target) throws IOException, AbortedException {
        log.trace("target: " + (target == null ? "null" : target.getClass().getSimpleName()));
        log.trace("entry: " + entry.toString());
        VlanModel current = null;
        switch (entry.type) {
            case CHASSIS:
                current = device;
                break;
            case CONTAINER:
                if (target instanceof Device) {
                    Slot slot = PhysicalConfigurationBuilder.createSlot(entry, device);
                    current = slot;
                }
                break;
            case MODULE:
                if (target instanceof Slot) {
                    Slot slot = (Slot) target;
                    Module module = PhysicalConfigurationBuilder.createModule(entry, slot);
                    current = module;
                } else {
                    log.warn("- unexpected target class: " + target.getClass().getSimpleName());
                }
                break;
            case PORT:
                Port port = this.factory.createPort(entry, device);
                if (target instanceof Device) {
                    device.addPort(port);
                } else if (target instanceof Module) {
                    Module module = (Module) target;
                    module.addPort(port);
                } else {
                    log.warn("unexpected target class: " + target.getClass().getSimpleName());
                    return;
                }
                current = port;
                log.debug("@ add port ifname='" + port.getIfName() + "'" + " on device '" + this.device.getDeviceName() + "'");
                break;
            default:
                log.debug("- ignored: " + entry.toString());
                return;
        }
        for (PhysicalEntry child : entry.getChildren()) {
            createPhysicalModel(child, current);
        }
    }

    private void createLag() throws AbortedException, IOException {
        Map<Integer, Integer> ifTypeMap = new HashMap<Integer, Integer>();
        try {
            List<IntSnmpEntry> ifTypes = SnmpUtil.getIntSnmpEntries(getSnmpAccess(), InterfaceMib.ifType);
            for (IntSnmpEntry ifType : ifTypes) {
                int ifIndex = ifType.getLastOIDIndex().intValue();
                int ifTypeValue = ifType.intValue();
                ifTypeMap.put(Integer.valueOf(ifIndex), Integer.valueOf(ifTypeValue));
            }
        } catch (Exception e) {
            throw new IOException("failed to walk ifType.", e);
        }
        Map<Integer, List<Integer>> stackStatusMap = ifMib.getIfStackStatusMapGroupByLower(1);
        for (Map.Entry<Integer, List<Integer>> stackStatus : stackStatusMap.entrySet()) {
            int lagIfIndex = stackStatus.getKey().intValue();
            List<Integer> memberIfIndices = stackStatus.getValue();
            log.debug("lag ifIndex=" + lagIfIndex + ", members=" + memberIfIndices);
            if (!isLag(stackStatus)) {
                continue;
            }
            Integer ifType = ifTypeMap.get(Integer.valueOf(lagIfIndex));
            if (ifType == null || ifType.intValue() != 161) {
                continue;
            }
            Port p = this.device.getPortByIfIndex(lagIfIndex);
            EthernetPortsAggregator lag = null;
            if (p == null) {
                lag = new EthernetPortsAggregatorImpl();
                lag.initDevice(this.device);
                lag.initIfIndex(lagIfIndex);
                ifMib.setIfName(lag);
                lag.setAggregationName(lag.getIfName());
            } else if (!EthernetPortsAggregator.class.isInstance(p)) {
                log.debug("non-LAG port found: ifIndex=" + lagIfIndex);
                continue;
            } else {
                lag = (EthernetPortsAggregator) p;
            }
            for (Integer memberIfIndex : memberIfIndices) {
                if (memberIfIndex == 0) {
                    continue;
                }
                Port member = this.device.getPortByIfIndex(memberIfIndex.intValue());
                if (member == null) {
                    throw new IllegalStateException("mib broken: no lag member found: " +
                            "lag=" + lagIfIndex + ", member=" + memberIfIndex);
                } else if (!(member instanceof EthernetPort)) {
                    throw new IllegalStateException("mib broken: non-ethernet port found as of lag-member: " +
                            "lag=" + lagIfIndex + ", member=" + memberIfIndex);
                }
                EthernetPort e = (EthernetPort) member;
                lag.addPhysicalPort(e);
            }
        }
    }

    private boolean isLag(Map.Entry<Integer, List<Integer>> entry) {
        if (entry == null) {
            return false;
        }
        if (entry.getKey() == null || entry.getValue() == null) {
            return false;
        } else if (entry.getKey().intValue() == 0) {
            return false;
        } else if (entry.getValue().size() > 1) {
            return true;
        } else if (entry.getValue().size() == 0) {
            return false;
        }
        Integer i = entry.getValue().get(0);
        return i.intValue() != 0;
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
                continue;
            } else if (!(port instanceof PhysicalPort)) {
                continue;
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
            }
            String ifDesc = row.getColumnValue(
                    InterfaceMib.ifDesc_SUFFIX,
                    SnmpHelper.stringEntryBuilder).getValue();
            port.setSystemDescription(ifDesc);
            port.setIfDescr(ifDesc);

            if (!(port instanceof PhysicalPort)) {
                continue;
            }
            PhysicalPort phy = (PhysicalPort) port;
            phy.setPortName(ifDesc);
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

        String path = new ConfigElementPathBuilder()
                .append("config global")
                .append("config system interface")
                .append("edit .*").toString();
        List<ConfigElement> elements = this.config.getsByPath(path);
        for (ConfigElement element : elements) {
            log.debug("target port: " + element.getId());
            String portName = element.getAttributeValue("edit \"(.*)\"");
            if (portName == null) {
                log.warn("- no port-name found.");
                continue;
            }
            Port port = this.device.getPortByIfName(portName);
            if (port == null) {
                log.debug("- no port found: " + portName);
                continue;
            } else if (!EthernetPort.class.isInstance(port)) {
                log.debug("- no ethernet port found.");
                continue;
            }
            EthernetPort eth = EthernetPort.class.cast(port);
            String speedDuplex = element.getAttributeValue("set speed (.*)");
            if (speedDuplex == null) {
                log.debug("- no speed/duplex id: set value as auto.");
                eth.setPortAdministrativeSpeed(PortSpeedValue.Admin.AUTO);
                eth.setDuplex(Duplex.AUTO);
            } else {
                speedDuplex = speedDuplex.toLowerCase();
                if ("1000full".equals(speedDuplex)) {
                    eth.setPortAdministrativeSpeed(new PortSpeedValue.Admin(1000L * 1000L * 1000L));
                    eth.setDuplex(Duplex.FULL);
                } else if ("1000half".equals(speedDuplex)) {
                    eth.setPortAdministrativeSpeed(new PortSpeedValue.Admin(1000L * 1000L * 1000L));
                    eth.setDuplex(Duplex.HALF);
                } else if ("100full".equals(speedDuplex)) {
                    eth.setPortAdministrativeSpeed(new PortSpeedValue.Admin(100L * 1000L * 1000L));
                    eth.setDuplex(Duplex.FULL);
                } else if ("100half".equals(speedDuplex)) {
                    eth.setPortAdministrativeSpeed(new PortSpeedValue.Admin(100L * 1000L * 1000L));
                    eth.setDuplex(Duplex.HALF);
                } else if ("10full".equals(speedDuplex)) {
                    eth.setPortAdministrativeSpeed(new PortSpeedValue.Admin(10L * 1000L * 1000L));
                    eth.setDuplex(Duplex.FULL);
                } else if ("10half".equals(speedDuplex)) {
                    eth.setPortAdministrativeSpeed(new PortSpeedValue.Admin(10L * 1000L * 1000L));
                    eth.setDuplex(Duplex.HALF);
                } else if ("auto".equals(speedDuplex)) {
                    eth.setPortAdministrativeSpeed(PortSpeedValue.Admin.AUTO);
                    eth.setDuplex(Duplex.AUTO);
                } else {
                    log.warn("- unexpected speed/duplex value: " + speedDuplex);
                }
            }
        }
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        createVlan();
        createVdom();
        createHa();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void createVlan() throws IOException, AbortedException {
        try {
            Map<Integer, List<Integer>> stackStatusMap = ifMib.getIfStackStatusMapGroupByHigher(-1);
            List<IntSnmpEntry> ifTypes = SnmpUtil.getIntSnmpEntries(getSnmpAccess(), InterfaceMib.ifType);
            for (IntSnmpEntry ifType : ifTypes) {
                int ifIndex = ifType.getLastOIDIndex().intValue();
                int ifTypeValue = ifType.intValue();
                if (ifTypeValue != 135) {
                    continue;
                }
                String ifName = ifMib.getIfName(ifIndex);
                log.debug("vif found: " + ifName + "(" + ifIndex + ")");
                String path = new ConfigElementPathBuilder()
                        .append("config global")
                        .append("config system interface")
                        .append("edit \"" + ifName + "\"").toString();
                ConfigElement element = this.config.getByPath(path);
                if (element == null) {
                    log.warn("no vlan config: " + ifName + "(" + ifIndex + ")");
                    continue;
                }
                String vlanID_ = element.getAttributeValue("set vlanid ([0-9]+)");
                if (vlanID_ == null) {
                    log.warn("no vlan id: " + ifName + "(" + ifIndex + ")\r\n" + element.toString());
                }
                int vlanID = Integer.parseInt(vlanID_);
                List<Integer> parentIfIndices = stackStatusMap.get(Integer.valueOf(ifIndex));
                log.debug("parents: " + parentIfIndices);
                if (!isVlanEnable(parentIfIndices)) {
                    log.debug("vlan-if isn't valid.");
                    continue;
                }
                int parentIfIndex = parentIfIndices.get(0);
                Port parentPort = this.device.getPortByIfIndex(parentIfIndex);
                LogicalEthernetPort parent = null;
                if (parentPort instanceof LogicalEthernetPort) {
                    parent = (LogicalEthernetPort) parentPort;
                } else if (parentPort instanceof EthernetPort) {
                    EthernetPort eth = (EthernetPort) parentPort;
                    parent = ((VlanDevice) device).getLogicalEthernetPort(eth);
                } else {
                    log.debug("parent is not ethernet: " + parentPort.getFullyQualifiedName());
                    continue;
                }
                if (parent == null) {
                    log.debug("parent not found: parent ifindex=" + parentIfIndex);
                    continue;
                }
                RouterVlanIf vif = new RouterVlanIfImpl();
                vif.initDevice(device);
                vif.initVlanId(vlanID);
                vif.initRouterPort(parent);
                vif.initIfIndex(ifIndex);
                vif.initIfName(ifName);
                vif.addTaggedPort(parent);
                log.debug("vlan-if created: " + vif.getFullyQualifiedName());
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private boolean isVlanEnable(List<Integer> list) {
        if (list == null) {
            return false;
        }
        if (list.size() != 1) {
            return false;
        }
        Integer i = list.get(0);
        return i.intValue() != 0;
    }

    private void createVdom() throws IOException, AbortedException {
        this.factory.createVdom(getDeviceAccess(), this.device);
        this.factory.createVdomLink(this.config, this.device);
    }

    private void createHa() throws IOException, AbortedException {
        Integer haMode = null;
        try {
            haMode = SnmpUtil.getInteger(getSnmpAccess(), FortigateMib.fnSysHaMode);
            HaModeRenderer haModeRenderer = new HaModeRenderer(this.device);
            haModeRenderer.set(haMode);
        } catch (NoSuchMibException e) {
            log.info("no mib value: fnSysHaMode");
        } catch (SnmpResponseException e) {
            throw new IOException("Unexpected response.", e);
        }
        if (haMode == null) {
            return;
        }
        try {
            setHaAttributes();
        } catch (SnmpResponseException e) {
            throw new IOException("Unexpected response.", e);
        }
    }

    private void setHaAttributes() throws IOException, AbortedException, SnmpResponseException {
        try {
            Integer groupID = SnmpUtil.getInteger(getSnmpAccess(), FortigateMib.fnHaGroupId);
            HaGroupIdRenderer renderer1 = new HaGroupIdRenderer(this.device);
            renderer1.set(groupID);
        } catch (NoSuchMibException e) {
            log.info("no mib value: fnHaGroupId");
        }
        try {
            Integer priority = SnmpUtil.getInteger(getSnmpAccess(), FortigateMib.fnHaPriority);
            HaPriorityRenderer renderer1 = new HaPriorityRenderer(this.device);
            renderer1.set(priority);
        } catch (NoSuchMibException e) {
            log.info("no mib value: fnHaPriority");
        }
        try {
            Integer autoSync = SnmpUtil.getInteger(getSnmpAccess(), FortigateMib.fnHaAutoSync);
            HaAutoSyncRenderer renderer1 = new HaAutoSyncRenderer(this.device);
            renderer1.set(autoSync);
        } catch (NoSuchMibException e) {
            log.info("no mib value: fnHaAutoSync");
        }
        try {
            Integer schedule = SnmpUtil.getInteger(getSnmpAccess(), FortigateMib.fnHaSchedule);
            HaScheduleRenderer renderer1 = new HaScheduleRenderer(this.device);
            renderer1.set(schedule);
        } catch (NoSuchMibException e) {
            log.info("no mib value: fnHaSchedule");
        }
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
        getDeviceInformation();
        getPhysicalConfiguration();
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
            String temp = ListUtil.toContent(ListUtil.head(ListUtil.toLines(res1), 10));
            log.debug("getTextConfiguration(): ["
                    + this.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress()
                    + "] result:\r\n" + temp + "\r\n----");
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        }
        setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
        return this.textConfiguration;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        if (this.textConfiguration == null) {
            getTextConfiguration();
        }
        SimpleFortigateConfigParser parser = new SimpleFortigateConfigParser(this.textConfiguration);
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
            console.close();
        }
        entry.close();
    }
}