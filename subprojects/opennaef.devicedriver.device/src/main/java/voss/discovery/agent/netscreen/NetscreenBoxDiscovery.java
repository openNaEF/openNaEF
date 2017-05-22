package voss.discovery.agent.netscreen;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.UnknownTargetException;
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
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetscreenBoxDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(NetscreenBoxDiscovery.class);
    private final MplsVlanDevice device;
    private final InterfaceMibImpl ifMib;
    private String textConfiguration = null;
    private ConfigurationStructure config = null;
    private final NetScreenZoneRenderer renderer;

    private ConsoleCommand show_config = new ConsoleCommand(new GlobalMode(), "get config");

    public NetscreenBoxDiscovery(DeviceAccess access) throws IOException, AbortedException {
        super(access);
        this.device = new MplsVlanDevice();
        this.renderer = new NetScreenZoneRenderer(this.device);
        this.ifMib = new InterfaceMibImpl(access.getSnmpAccess());
    }

    public Device getDeviceInner() {
        return this.device;
    }

    public void getDeviceInformation() throws IOException, AbortedException {
        String fqn = Mib2Impl.getSysName(getDeviceAccess().getSnmpAccess());
        String hostname = fqn;
        String domainName = null;
        int idx = fqn.indexOf('.');
        if (idx != -1) {
            hostname = fqn.substring(0, idx);
            domainName = fqn.substring(idx + 1);
        }
        String modelName = null;
        try {
            modelName = AgentConfiguration.getInstance().getDeviceType(Mib2Impl.getSysObjectId(getSnmpAccess()));
        } catch (UnknownTargetException e) {
        }
        device.setModelTypeName(modelName);
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_JUNIPER);
        device.setOsTypeName(OSType.SCREENOS.caption);
        device.setDeviceName(hostname);
        device.setDomainName(domainName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getSnmpAccess().getSnmpAgentAddress().getAddress()
                .getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        try {
            String osVersion = SnmpUtil.getString(getSnmpAccess(), ScreenOSMib.nsSetGenSwVer);
            device.setOsVersion(osVersion);
        } catch (Exception e) {
            log.warn("", e);
        }
        try {
            String serial = SnmpUtil.getString(getSnmpAccess(), ScreenOSMib.nsSlotSN + ".1");
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
        if (!isDiscoveryDone(DiscoveryStatus.CONFIGURATION)) {
            getConfiguration();
        }
        createEthernetPorts();
        createLag();
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    public static final Pattern portIndexPattern = Pattern.compile("ethernet0/([0-9]+)");

    private void createEthernetPorts() throws IOException, AbortedException {
        try {
            List<StringSnmpEntry> ifNames = SnmpUtil.getStringSnmpEntries(getSnmpAccess(), InterfaceMib.ifName);
            for (StringSnmpEntry ifNameEntry : ifNames) {
                int ifIndex = ifNameEntry.getLastOIDIndex().intValue();
                String ifName = ifNameEntry.getValue();
                Matcher matcher = portIndexPattern.matcher(ifName);
                if (!matcher.matches()) {
                    log.debug("not ethernet-port ifName: " + ifName);
                    continue;
                }
                String index = matcher.group(1);
                int portIndex = Integer.parseInt(index);
                EthernetPort eth = new EthernetPortImpl();
                eth.initDevice(this.device);
                eth.initIfIndex(ifIndex);
                eth.initIfName(ifName);
                eth.initPortIndex(portIndex);
            }
            setPortAttributes();
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void createLag() throws IOException, AbortedException {
    }

    protected void setPortAttributes() throws AbortedException, IOException {
        Map<String, Long> map = new HashMap<String, Long>();

        MibTable ifXTable = new MibTable(getSnmpAccess(), "ifXTable", InterfaceMib.ifXTable);
        ifXTable.addColumn(InterfaceMib.ifName_SUFFIX, "ifName");
        ifXTable.addColumn(InterfaceMib.ifHighSpeed_SUFFIX, "ifHighSpeed");
        ifXTable.addColumn(InterfaceMib.ifAlias_SUFFIX, "ifAlias");
        ifXTable.walk();

        Set<String> knownIfName = new HashSet<String>();
        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(ifXTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        for (KeyHolder key : keys) {
            TableRow row = ifXTable.getKeyAndRows().get(key);
            int ifIndex = key.intValue(0);

            StringSnmpEntry ifNameEntry = row.getColumnValue(
                    InterfaceMib.ifName_SUFFIX,
                    SnmpHelper.stringEntryBuilder);
            if (ifNameEntry == null) {
                log.warn("no ifName.");
                continue;
            }

            String ifName = ifNameEntry.getValue();
            if (knownIfName.contains(ifName)) {
                continue;
            }
            knownIfName.add(ifName);
            Port port = device.getPortByIfName(ifName);
            if (port == null) {
                log.warn("no port found: " + ifName);
                continue;
            }

            StringSnmpEntry ifAliasEntry = row.getColumnValue(
                    InterfaceMib.ifAlias_SUFFIX,
                    SnmpHelper.stringEntryBuilder);
            if (ifAliasEntry != null) {
                String ifAlias = ifAliasEntry.getValue();
                port.setIfDescr(ifAlias);
                port.setUserDescription(ifAlias);
            }

            if (!(port instanceof PhysicalPort)) {
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

            int adminStatus = row.getColumnValue(InterfaceMib.ifAdminStatus_SUFFIX, SnmpHelper.intEntryBuilder).intValue();
            int operStatus = row.getColumnValue(InterfaceMib.ifOperStatus_SUFFIX, SnmpHelper.intEntryBuilder).intValue();
            port.setAdminStatus(InterfaceMibImpl.getAdminStatusString(adminStatus));
            port.setOperationalStatus(InterfaceMibImpl.getOperStatusString(operStatus));

            if (!(port instanceof PhysicalPort)) {
                continue;
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
        }
    }

    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        createVlan();
        createZone();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void createVlan() throws IOException, AbortedException {
        createVlanIf();
    }

    public static final String vlanInterfaceRegex = "set interface \"(ethernet0/[0-9]+\\.[0-9]+)\" tag ([0-9]+) .*";
    public static final Pattern vlanInterfacePattern = Pattern.compile(vlanInterfaceRegex);

    private void createVlanIf() throws IOException, AbortedException {
        try {
            ConfigElement root = this.config.getRootElement();
            for (String line : root.getAttributes(vlanInterfaceRegex)) {
                log.debug("found vlan: " + line);
                Matcher matcher = vlanInterfacePattern.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }
                String ifName = matcher.group(1);
                int vlanID = Integer.parseInt(matcher.group(2));
                LogicalEthernetPort parent = getParentPort(ifName);
                if (parent == null) {
                    throw new IllegalStateException("no parent port: " + ifName);
                }
                RouterVlanIf vif = new RouterVlanIfImpl();
                vif.initDevice(this.device);
                vif.initVlanId(vlanID);
                vif.initIfName(ifName);
                vif.initRouterPort(parent);
                vif.setVlanName(ifName);
                vif.addTaggedPort(parent);

                final String ipRegex = "set interface \"?" + ifName + "\"? ip ([0-9a-f.:]+)/([0-9]+)";
                final Pattern ptr = Pattern.compile(ipRegex);
                List<String> ipAddressSettings = root.getAttributes(ipRegex);
                for (String ipAddressSetting : ipAddressSettings) {
                    Matcher m = ptr.matcher(ipAddressSetting);
                    if (!m.matches()) {
                        continue;
                    }
                    String ip = m.group(1);
                    String mask = m.group(2);
                    log.debug("found ip: " + ifName + "->" + ip + "/" + mask);
                    try {
                        InetAddress ipAddr = InetAddress.getByName(ip);
                        int maskLength = Integer.parseInt(mask);
                        CidrAddress addr = new CidrAddress(ipAddr, maskLength);
                        this.device.addIpAddressToPort(addr, vif);
                    } catch (Exception e) {
                        log.warn("failed to process ip-address.", e);
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("failed to get mib", e);
        }
    }

    private LogicalEthernetPort getParentPort(String vlanIfName) {
        int index = vlanIfName.indexOf('.');
        if (index == -1) {
            return null;
        }
        String ifName = vlanIfName.substring(0, index);
        Port p = this.device.getPortByIfName(ifName);
        if (p == null || !(p instanceof EthernetPort)) {
            return null;
        }
        EthernetPort eth = (EthernetPort) p;
        return this.device.getLogicalEthernetPort(eth);
    }

    private void createZone() throws IOException, AbortedException {
        createZoneObject();
        bindPortAndZone();
    }

    private void createZoneObject() throws IOException, AbortedException {
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(getSnmpAccess(), ScreenOSMib.nsZoneCfgName);
            if (entries.size() == 0) {
                return;
            }
            List<NetScreenZone> zones = new ArrayList<NetScreenZone>();
            for (StringSnmpEntry entry : entries) {
                String name = entry.getValue();
                int id = entry.getLastOIDIndex().intValue();
                if (id == 0) {
                    continue;
                }
                NetScreenZone zone = new NetScreenZone(this.device, name, id);
                zones.add(zone);
                log.debug("create zone: " + zone.toString());
            }
            renderer.set(zones);
        } catch (SnmpResponseException e) {
            throw new IOException(e.getMessage(), e);
        } catch (RepeatedOidException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void bindPortAndZone() throws IOException, AbortedException {
        MibTable table = new MibTable(getSnmpAccess(), "nsIfEntry", ScreenOSMib.nsIfEntry);
        table.addColumn("2", "nsIfName");
        table.addColumn("4", "nsIfZone");
        table.walk();
        List<TableRow> rows = table.getRows();
        if (rows.size() == 0) {
            return;
        }
        for (TableRow row : rows) {
            String key = row.getKey().toString();
            StringSnmpEntry ifNameEntry = row.getColumnValue("2", SnmpHelper.stringEntryBuilder);
            if (ifNameEntry == null) {
                log.warn("- no nsIfName: " + key);
                continue;
            }
            String ifName = ifNameEntry.getValue();
            IntSnmpEntry ifZoneEntry = row.getColumnValue("4", SnmpHelper.intEntryBuilder);
            if (ifZoneEntry == null) {
                log.warn("- no nsIfZone: " + key);
                continue;
            }
            int ifZone = ifZoneEntry.intValue();
            NetScreenZone zone = renderer.getById(ifZone);
            if (zone == null) {
                log.warn("- zone not found: " + ifZone);
                continue;
            }
            Port port = this.device.getPortByIfName(ifName);
            if (port == null) {
                log.warn("- no port found: " + ifName);
                continue;
            }
            zone.addZoneMemberPort(port);
            port.gainConfigurationExtInfo().put(NetScreenZone.KEY, zone);
            log.debug("- set zone to port: " + ifName + " > " + zone.toString());
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
            String res1 = console.getResponse(show_config);
            this.textConfiguration = res1;
            List<String> lines = ListUtil.toLines(res1);
            String head = ListUtil.toContent(ListUtil.head(lines, 10));
            setDiscoveryStatusDone(DiscoveryStatus.TEXT_CONFIGURATION);
            log.debug("getTextConfiguration(): ["
                    + this.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress()
                    + "] result:\r\n" + head + "\r\n----");
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
        SimpleScreenOSCommandParser parser = new SimpleScreenOSCommandParser(this.textConfiguration);
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
            String res1 = console.getResponse(show_config);
            entry.addConsoleResult(show_config, res1);
            console.close();
        }
        entry.close();
    }
}