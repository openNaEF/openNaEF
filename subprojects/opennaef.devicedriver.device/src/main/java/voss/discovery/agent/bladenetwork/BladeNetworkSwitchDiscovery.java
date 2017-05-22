package voss.discovery.agent.bladenetwork;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.SimpleCiscoIosConfigParser;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.*;
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
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.*;
import voss.model.EthernetPort.AutoNego;
import voss.model.EthernetPort.Duplex;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.*;

public class BladeNetworkSwitchDiscovery extends GenericSwitchDiscovery implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(BladeNetworkSwitchDiscovery.class);
    private final InterfaceMibImpl ifMib;
    @SuppressWarnings("unused")
    private ConfigurationStructure config = null;

    private ConsoleCommand show_running_config = new ConsoleCommand(new GlobalMode(), "show running-config");
    private ConsoleCommand show_interface = new ConsoleCommand(new GlobalMode(), "show interface");
    private ConsoleCommand show_version = new ConsoleCommand(new GlobalMode(), "show version");

    public BladeNetworkSwitchDiscovery(DeviceAccess access) throws IOException, AbortedException {
        super(access);
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
        device.setModelTypeName("BladeNetwork Switch");
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_BLADENETWORK);
        device.setOsTypeName(OSType.BLADENETWORKOS.caption);
        device.setDeviceName(hostname);
        device.setDomainName(domainName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getSnmpAccess().getSnmpAgentAddress().getAddress()
                .getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        try {
            String osVersion = SnmpUtil.getString(getSnmpAccess(), BladeNetworkMib.agSoftwareVersion);
            device.setOsVersion(osVersion);
        } catch (Exception e) {
            log.warn("", e);
        }
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
        createEthernetPorts();
        createLag();
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        setTaggingState();
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void createEthernetPorts() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> dot1dBasePorts = SnmpUtil.getIntSnmpEntries(getSnmpAccess(), BridgeMib.dot1dBasePortIfIndex);
            List<StringSnmpEntry> ifNames = SnmpUtil.getStringSnmpEntries(getSnmpAccess(), InterfaceMib.ifName);
            Map<Integer, StringSnmpEntry> ifNameMap = SnmpUtil.toIndexedMap(ifNames);
            for (IntSnmpEntry dot1dBasePort : dot1dBasePorts) {
                int ifIndex = dot1dBasePort.intValue();
                int portIndex = dot1dBasePort.getLastOIDIndex().intValue();
                StringSnmpEntry e = ifNameMap.get(Integer.valueOf(ifIndex));
                if (e == null) {
                    log.warn("no ifName: " + ifIndex);
                    continue;
                }
                EthernetPort eth = new EthernetPortImpl();
                eth.initDevice(this.device);
                eth.initIfIndex(ifIndex);
                eth.initIfName(e.getValue());
                eth.initPortIndex(portIndex);
            }
            setPortAttributes();
            setExtraPortAttributes();
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private void setExtraPortAttributes() throws IOException, AbortedException {
        MibTable portCurrentConfigTable = new MibTable(getSnmpAccess(),
                "agPortCurCfgTableEntry", BladeNetworkMib.agPortCurCfgTableEntry);
        portCurrentConfigTable.addColumn(BladeNetworkMib.SUFFIX_AutoNego, "autoNego");
        portCurrentConfigTable.addColumn(BladeNetworkMib.SUFFIX_Description, "name");
        portCurrentConfigTable.addColumn(BladeNetworkMib.SUFFIX_Duplex, "duplex");
        portCurrentConfigTable.addColumn(BladeNetworkMib.SUFFIX_Speed, "speed");
        portCurrentConfigTable.walk();

        List<KeyHolder> keys = new ArrayList<KeyHolder>();
        keys.addAll(portCurrentConfigTable.getKeyAndRows().keySet());
        Collections.sort(keys);
        log.debug("agPortCurCfgTableEntry: found " + keys.size() + " entries.");
        for (KeyHolder key : keys) {
            TableRow row = portCurrentConfigTable.getKeyAndRows().get(key);
            int index = key.intValue(0);
            PhysicalPort p = getPortByPortIndex(index);
            if (p == null) {
                continue;
            }
            log.debug("setExtraAttributes: " + p.getFullyQualifiedName());
            StringSnmpEntry nameEntry = row.getColumnValue(BladeNetworkMib.SUFFIX_Description, SnmpHelper.stringEntryBuilder);
            if (nameEntry != null) {
                String name = nameEntry.getValue();
                p.setUserDescription(name);
                log.debug("@ set port " + p.getIfName() + " user-description [" + name + "]");
            }
            if (!EthernetPort.class.isInstance(p)) {
                continue;
            }
            EthernetPort eth = (EthernetPort) p;
            IntSnmpEntry autoNegoEntry = row.getColumnValue(BladeNetworkMib.SUFFIX_AutoNego, SnmpHelper.intEntryBuilder);
            if (autoNegoEntry != null) {
                int autoNego = autoNegoEntry.intValue();
                switch (autoNego) {
                    case 1:
                        eth.setAutoNego(AutoNego.OFF);
                        log.debug("@ set port " + p.getIfName() + " auto-nego [off]");
                        break;
                    case 2:
                        eth.setAutoNego(AutoNego.ON);
                        log.debug("@ set port " + p.getIfName() + " auto-nego [ON]");
                        break;
                    default:
                        log.debug("unexpected auto-nego value: " + autoNego);
                }
            }
            IntSnmpEntry duplexEntry = row.getColumnValue(BladeNetworkMib.SUFFIX_Duplex, SnmpHelper.intEntryBuilder);
            if (duplexEntry != null) {
                int duplex = duplexEntry.intValue();
                switch (duplex) {
                    case 2:
                        eth.setDuplex(Duplex.FULL);
                        log.debug("@ set port " + p.getIfName() + " duplex [full]");
                        break;
                    case 4:
                        eth.setDuplex(Duplex.AUTO);
                        log.debug("@ set port " + p.getIfName() + " duplex [auto]");
                        break;
                    default:
                        log.debug("unexpected duplex value: " + duplex);
                }
            }
            IntSnmpEntry speedEntry = row.getColumnValue(BladeNetworkMib.SUFFIX_Speed, SnmpHelper.intEntryBuilder);
            if (speedEntry != null) {
                int speedType = speedEntry.intValue();
                switch (speedType) {
                    case 4:
                        eth.setPortAdministrativeSpeed(new PortSpeedValue.Admin(1000L * 1000L * 1000L));
                        log.debug("@ set port " + p.getIfName() + " speed [1000Mbps]");
                        break;
                    case 5:
                        eth.setPortAdministrativeSpeed(PortSpeedValue.Admin.AUTO);
                        log.debug("@ set port " + p.getIfName() + " speed [auto]");
                        break;
                    default:
                        log.debug("unexpected speed value: " + speedType);
                }
            }
        }
    }

    private PhysicalPort getPortByPortIndex(int index) {
        for (Port port : this.device.getPorts()) {
            if (!PhysicalPort.class.isInstance(port)) {
                continue;
            }
            PhysicalPort phy = (PhysicalPort) port;
            int _index = phy.getPortIndex();
            if (_index == index) {
                return phy;
            }
        }
        return null;
    }

    private void setTaggingState() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(getSnmpAccess(), BladeNetworkMib.portInfoVlanTag);
            for (IntSnmpEntry entry : entries) {
                int portIndex = entry.getLastOIDIndex().intValue();
                int taggingValue = entry.intValue();
                EthernetPort eth = getEthernetPort(portIndex);
                if (eth == null) {
                    continue;
                }
                LogicalEthernetPort le = this.device.getLogicalEthernetPort(eth);
                switch (taggingValue) {
                    case 0:
                        le.setVlanPortUsage(VlanPortUsage.ACCESS);
                        break;
                    case 1:
                        le.setVlanPortUsage(VlanPortUsage.TRUNK);
                        break;
                    default:
                        log.warn("illegal value: portInfoVlanTag: " + eth.getFullyQualifiedName() + "=" + taggingValue);
                }
            }
        } catch (Exception e) {
            throw new IOException("failed to get tagging-state.", e);
        }
    }

    private void createLag() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> trunkPortStatuses = SnmpUtil.getIntSnmpEntries(getSnmpAccess(),
                    BladeNetworkMib.trunkGroupInfoPortStatus);
            List<IntSnmpEntry> trunkStatuses = SnmpUtil.getIntSnmpEntries(getSnmpAccess(),
                    BladeNetworkMib.trunkGroupInfoState);
            Map<Integer, IntSnmpEntry> trunkStatusMap = SnmpUtil.toIndexedMap(trunkStatuses);
            for (IntSnmpEntry trunkPortStatus : trunkPortStatuses) {
                int lagID = trunkPortStatus.getOIDSuffix(0);
                int portID = trunkPortStatus.getOIDSuffix(1);
                EthernetPortsAggregator lag = this.device.getEthernetPortsAggregatorByAggregationGroupId(lagID);
                if (lag == null) {
                    String ifName = "portchannel " + lagID;
                    lag = new EthernetPortsAggregatorImpl();
                    lag.initDevice(this.device);
                    lag.initAggregationGroupId(lagID);
                    lag.initIfName(ifName);
                    lag.setAggregationName(ifName);
                }
                EthernetPort eth = getEthernetPort(portID);
                if (eth == null) {
                    continue;
                }
                lag.addPhysicalPort(eth);
                IntSnmpEntry lagStatusEntry = trunkStatusMap.get(Integer.valueOf(lagID));
                switch (lagStatusEntry.intValue()) {
                    case 1:
                        lag.setAdminStatus("enabled");
                        break;
                    case 2:
                        lag.setAdminStatus("disabled");
                        break;
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (AbortedException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("failed to get mib", e);
        }
    }

    private EthernetPort getEthernetPort(int portIndex) {
        for (EthernetPort eth : this.device.selectPorts(EthernetPort.class)) {
            if (eth.getPortIndex() == portIndex) {
                return eth;
            }
        }
        return null;
    }

    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        createVlan();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void createVlan() throws IOException, AbortedException {
        createVlanIf();
        bindPorts();
    }

    private void createVlanIf() throws IOException, AbortedException {
        try {
            for (StringSnmpEntry vlanNameEntry : SnmpUtil.getStringSnmpEntries(getSnmpAccess(), BladeNetworkMib.vlanInfoName)) {
                int vlanID = vlanNameEntry.getLastOIDIndex().intValue();
                String vlanName = vlanNameEntry.getValue();
                VlanIf vif = new VlanIfImpl();
                vif.initDevice(this.device);
                vif.initVlanId(vlanID);
                vif.initIfName(getSwitchVlanIfName(vlanID));
                vif.setVlanName(vlanName);
            }
        } catch (IOException e) {
            throw e;
        } catch (AbortedException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("failed to get mib", e);
        }
    }

    private void bindPorts() throws IOException, AbortedException {
        try {
            for (ByteSnmpEntry vlanPortEntry : SnmpUtil.getByteSnmpEntries(getSnmpAccess(), BladeNetworkMib.vlanInfoPorts)) {
                int vlanID = vlanPortEntry.getLastOIDIndex().intValue();
                byte[] portList = vlanPortEntry.getValue();
                int[] portIDs = SnmpUtil.decodeBitList(portList);
                log.debug("* vlan=" + vlanID + "; " + Arrays.toString(portIDs));
                VlanIf vif = this.device.getVlanIfByVlanId(vlanID);
                if (vif == null) {
                    log.warn("vlan[" + vlanID + "] not found.");
                    continue;
                }
                for (int portID : portIDs) {
                    log.debug("port[" + portID + "]");
                    if (portID == 1) {
                        continue;
                    }
                    EthernetPort eth = getEthernetPort(portID - 1);
                    if (eth == null) {
                        continue;
                    }
                    log.debug("- port[" + portID + "] -> " + eth.getFullyQualifiedName());
                    LogicalEthernetPort le = this.device.getLogicalEthernetPort(eth);
                    VlanPortUsage usage = le.getVlanPortUsage();
                    if (usage == null) {
                        continue;
                    }
                    switch (usage) {
                        case ACCESS:
                            log.debug("- untagged DP LE: " + le);
                            vif.addUntaggedPort(le);
                            break;
                        case TRUNK:
                            log.debug("- tagged DP LE: " + le);
                            vif.addTaggedPort(le);
                            break;
                        default:
                            throw new IllegalStateException("unknown tagging state: " + usage);
                    }
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (AbortedException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("failed to get mib", e);
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
    protected ConsoleCommand getShowConfigCommand() {
        return this.show_running_config;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        if (this.textConfiguration == null) {
            getTextConfiguration();
        }

        SimpleCiscoIosConfigParser parser = new SimpleCiscoIosConfigParser(this.textConfiguration);
        parser.parse();

        this.config = parser.getConfigurationStructure();
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

            String res4 = console.getResponse(show_version);
            entry.addConsoleResult(show_version, res4);

            console.close();
        }

        entry.close();
    }
}