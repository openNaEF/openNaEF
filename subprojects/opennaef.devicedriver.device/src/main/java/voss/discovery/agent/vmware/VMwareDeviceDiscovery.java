package voss.discovery.agent.vmware;

import com.vmware.vim25.*;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.EntityMib;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.agent.util.DiscoveryUtils;
import voss.discovery.agent.vmware.collector.CollectorService;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleException;
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
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.*;

public class VMwareDeviceDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(VMwareDeviceDiscovery.class);
    private final InterfaceMibImpl ifMib;
    protected final AgentConfiguration agentConfig;
    private final CollectorService service;

    public VMwareDeviceDiscovery(DeviceAccess access)
            throws IOException, AbortedException {
        super(access);
        this.agentConfig = AgentConfiguration.getInstance();
        this.ifMib = new InterfaceMibImpl(access.getSnmpAccess());
        this.service = new CollectorService(
                access.getSnmpAccess().getSnmpAgentAddress().getAddress().getHostAddress(),
                access.getNodeInfo().getAdminAccount(),
                access.getNodeInfo().getAdminPassword());
    }

    private VMwareServerImpl device;

    @Override
    public Device getDeviceInner() {
        return this.device;
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        device = new VMwareServerImpl();
        String fqn = Mib2Impl.getSysName(getDeviceAccess().getSnmpAccess());
        String hostname = fqn;
        String domainName = null;
        int idx = fqn.indexOf('.');
        if (idx != -1) {
            hostname = fqn.substring(0, idx);
            domainName = fqn.substring(idx + 1);
        }
        String sysObjectId = Mib2Impl.getSysObjectId(getDeviceAccess().getSnmpAccess());
        device.setModelTypeName(sysObjectId);
        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setVendorName(Constants.VENDOR_VMWARE);
        device.setOsTypeName(OSType.VMware_ESX.caption);
        String esxVersion = getEsxVersion();
        device.setOsVersion(esxVersion);
        device.setDeviceName(hostname);
        device.setDomainName(domainName);
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setDescription(sysDescr);
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getSnmpAccess().getSnmpAgentAddress().getAddress()
                .getHostAddress());
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

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

    private String getEsxVersion() {
        return null;
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        ifMib.createPhysicalPorts(device);
        ifMib.setAllIfNames(device);
        setPortAttributes();
        DiscoveryUtil.supplementLogicalEthernetPort(this.device);
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void setPortAttributes() throws AbortedException, IOException {
        Map<String, Long> map = new HashMap<String, Long>();

        MibTable ifXTable = new MibTable(getSnmpAccess(), "", InterfaceMib.ifXTable);
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

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        collectVSwitches();
        collectVirtualHosts();
        collectVirtualLinks();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void collectVSwitches() {
        for (EthernetSwitch vSwitch : discoverVSwitches()) {
            device.addVSwitch(vSwitch);
        }
    }

    private List<EthernetSwitch> discoverVSwitches() {
        List<EthernetSwitch> result = new ArrayList<EthernetSwitch>();
        for (HostVirtualSwitch vSw : getVSwitches()) {
            EthernetSwitch vSwitch = discoverVSwitch(vSw.getName());
            result.add(vSwitch);
        }
        return result;
    }

    private ArrayList<HostVirtualSwitch> getVSwitches() {
        return service.getAllVSwitch();
    }

    private EthernetSwitch discoverVSwitch(String vSwitchKey) {
        EthernetSwitch vSwitch = new GenericEthernetSwitch();
        vSwitch.setPhysicalDevice(this.device);
        vSwitch.setDeviceName(vSwitchKey);
        vSwitch.setModelTypeName(Constants.DEVICE_TYPE_VMWARE_VSWITCH);
        vSwitch.setOsTypeName(OSType.vSwitchOS.caption);
        discoverVSwitchPorts(vSwitchKey, vSwitch);
        discoverVSwitchVlans(vSwitchKey, vSwitch);
        bindPortAndVlan(vSwitchKey, vSwitch);
        return vSwitch;
    }

    private void discoverVSwitchPorts(String vSwitchKey, EthernetSwitch vSwitch) {
        log.debug("discoverVSwitchPorts: device=" + vSwitch.getDeviceName());
        HostVirtualSwitch hVS = service.getVSwitch(vSwitchKey);
        if (hVS.getPnic() != null) {
            for (String key : hVS.getPnic()) {
                String ifName = VMwareDiscoveryUtil.getVmnicName(key);
                log.debug("- found uplink port: " + ifName);
                EthernetPort etherPort = new EthernetPortImpl();
                etherPort.initDevice(vSwitch);
                etherPort.initIfName(ifName);
                DefaultLogicalEthernetPort le = new DefaultLogicalEthernetPortImpl();
                le.initDevice(vSwitch);
                le.initPhysicalPort(etherPort);
                le.initIfName("[logical]" + ifName);
                le.setVlanPortUsage(VlanPortUsage.TRUNK);
            }
        }

        for (VirtualMachineConfigInfo vm : service.getVirtualMachineFromVSwitchName(vSwitchKey)) {
            String ifName = vm.getName();
            log.debug("- found downlink port: " + ifName);
            EthernetPort etherPort = new EthernetPortImpl();
            etherPort.initDevice(vSwitch);
            etherPort.initIfName(ifName);
            etherPort.setMacAddress(service.getVMPorts(vm.getName()).get(0).getMacAddress());
            DefaultLogicalEthernetPort le = new DefaultLogicalEthernetPortImpl();
            le.initDevice(vSwitch);
            le.initPhysicalPort(etherPort);
            le.initIfName("[logical]" + ifName);
            le.setVlanPortUsage(VlanPortUsage.ACCESS);
        }
    }


    private void discoverVSwitchVlans(String vSwitchKey, EthernetSwitch vSwitch) {
        log.debug("discoverVSwitchVlans: device=" + vSwitch.getDeviceName());
        if (getVlans(vSwitchKey) != null) {
            for (HostPortGroupSpec vlan : getVlans(vSwitchKey)) {
                log.debug("- id=" + vlan.getVlanId() + ", name=" + vlan.getName());
                if (vlan.getVlanId() >= 0) {
                    VlanIf vif = new VlanIfImpl();
                    vif.initDevice(vSwitch);
                    vif.initVlanId(vlan.getVlanId());
                    vif.initIfName(vlan.getName());
                    vif.setConfigName(vlan.getName());
                    vif.setVlanName(vlan.getName());
                }
            }
        }
    }

    private List<HostPortGroupSpec> getVlans(String vSwitchKey) {
        List<HostPortGroupSpec> vlans = new ArrayList<HostPortGroupSpec>();

        List<String> console = new ArrayList<String>();
        try {
            if (service.getHostConfigInfo().getNetwork().getConsoleVnic() != null) {
                for (HostVirtualNic vNic : service.getHostConfigInfo().getNetwork().getConsoleVnic()) {
                    console.add(vNic.getPortgroup());
                }
            }
        } catch (IOException e) {
            log.debug("failed to get ConsoleVnic");
        }

        for (String portGroupKey : service.getVSwitch(vSwitchKey).getPortgroup()) {
            HostPortGroup portGroup = service.getPortGroup(portGroupKey);
            if (portGroup != null) {
                HostPortGroupSpec spec = portGroup.getSpec();
                if (!console.contains(spec.getName())) {
                    vlans.add(spec);
                }
            }
        }
        return vlans;
    }

    private void bindPortAndVlan(String vSwitchKey, EthernetSwitch vSwitch) {
        log.debug("bindPortAndVlan: " + vSwitch.getDeviceName() + ", " + vSwitchKey);
        for (PhysicalNic tagged : getTaggedBinds(vSwitchKey)) {
            for (VlanIf vif : vSwitch.getVlanIfs()) {
                if (vif.getVlanId() < 0) {
                    continue;
                }
                String ifName = VMwareDiscoveryUtil.getVmnicName(tagged.getKey());
                Port port = vSwitch.getPortByIfName(ifName);
                LogicalEthernetPort le = DiscoveryUtils.getLogicalEthernetPort(port);
                if (le == null) {
                    throw new IllegalStateException("no uplink logical-ether: "
                            + vSwitch.getDeviceName() + ":" + ifName);
                }
                log.debug("- found uplink: " + le.getFullyQualifiedName());
                vif.addTaggedPort(le);
            }
        }
        for (HostPortGroup untagged : getUntaggedBinds(vSwitchKey)) {
            int vlanId = untagged.getSpec().getVlanId();
            String networkName = untagged.getSpec().getName();
            if (vlanId < 0 || 4095 < vlanId) {
                continue;
            }
            VlanIf vif = vSwitch.getVlanIfByVlanId(vlanId);
            for (VirtualMachineConfigInfo vm : service.getVirtualMachineFromNetworkName(networkName)) {
                Port port = vSwitch.getPortByIfName(vm.getName());
                LogicalEthernetPort le = DiscoveryUtils.getLogicalEthernetPort(port);
                vif.addUntaggedPort(le);
                log.debug("- found downlink: " + port.getFullyQualifiedName());
            }
        }
    }

    private List<PhysicalNic> getTaggedBinds(String vSwitchKey) {
        List<PhysicalNic> taggedBinds = new ArrayList<PhysicalNic>();
        String[] pnic = service.getVSwitch(vSwitchKey).getPnic();
        if (pnic != null) {
            for (String portKey : pnic) {
                for (PhysicalNic port : service.getPnic()) {
                    if (port.getKey().equals(portKey)) {
                        taggedBinds.add(port);
                    }
                }
            }
        }

        return taggedBinds;
    }

    private List<HostPortGroup> getUntaggedBinds(String vSwitchKey) {
        return service.getPortGroups(vSwitchKey);
    }

    private void collectVirtualHosts() {
        service.getVirtualMachines();
        for (VirtualMachineConfigInfo vm : getVirtualMachines()) {
            VirtualServerDevice vHost = new VirtualServerDevice();
            vHost.setPhysicalDevice(this.device);
            vHost.setDeviceName(vm.getName());
            vHost.setModelTypeName(Constants.DEVICE_TYPE_VMWARE_VM);
            vHost.setOsTypeName(vm.getGuestFullName());
            discoverPorts(vm, vHost);
            device.addVirtualHost(vHost);
            log.debug("found vm: " + vHost.getDeviceName());
        }
    }

    private ArrayList<VirtualMachineConfigInfo> getVirtualMachines() {
        return service.getVirtualMachines();
    }

    private void discoverPorts(VirtualMachineConfigInfo vm, VirtualServerDevice vHost) {
        log.debug("discoverPorts: " + vHost.getDeviceName());
        for (VirtualEthernetCard vNic : getVirtualHostPorts(vm)) {
            EthernetPort eth = new EthernetPortImpl();
            eth.initDevice(vHost);
            eth.initIfName(vNic.getDeviceInfo().getLabel());
            eth.setMacAddress(vNic.getMacAddress());
            CidrAddress cidrAddress = discoverIpAddress(eth.getMacAddress());
            if (cidrAddress != null) {
                vHost.addIpAddressToPort(cidrAddress, eth);
            }
            log.debug("- found port: " + eth.getIfName());
        }
    }

    private CidrAddress discoverIpAddress(String macAddress) {
        return service.getIpAddress(macAddress);
    }

    private List<VirtualEthernetCard> getVirtualHostPorts(VirtualMachineConfigInfo vm) {
        return service.getVMPorts(vm.getName());
    }

    private void collectVirtualLinks() {
        Map<String, EthernetPort> vSwitchPorts = new HashMap<String, EthernetPort>();
        for (EthernetSwitch vSwitch : device.getVSwitches()) {
            for (EthernetPort port : vSwitch.getEthernetPorts()) {
                vSwitchPorts.put(port.getMacAddress(), port);
            }
        }

        Map<String, EthernetPort> vHostPorts = new HashMap<String, EthernetPort>();
        for (VirtualServerDevice vHost : device.getVirtualHosts()) {
            for (PhysicalPort port : vHost.getPhysicalPorts()) {
                if (port instanceof EthernetPort) {
                    EthernetPort ether = (EthernetPort) port;
                    vHostPorts.put(ether.getMacAddress(), ether);
                }
            }
        }

        for (EthernetSwitch vSwitch : device.getVSwitches()) {
            for (Port port : vSwitch.getPorts()) {
                if (port instanceof EthernetPort) {
                    PhysicalPort p1 = (PhysicalPort) port;
                    String mac = ((EthernetPort) port).getMacAddress();
                    if (!vHostPorts.containsKey(mac)) continue;
                    PhysicalPort p2 = vHostPorts.get(mac);
                    Link link = new LinkImpl();
                    link.initPorts(p1, p2);
                    log.debug("create link: {" + p1.getFullyQualifiedName() + "|" + p2.getFullyQualifiedName() + "}");
                }
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
        ifMib.setAllIfOperStatus(device);
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        return null;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        setDiscoveryStatusDone(DiscoveryStatus.CONFIGURATION);
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException, ConsoleException, AbortedException {
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());
        entry.addMibDump(getDeviceAccess().getNodeInfo(), ".1");
        try {
            if (!isDiscoveryDone(DiscoveryStatus.LOGICAL_CONFIGURATION)) {
                this.getLogicalConfiguration();
            }
            Device d = getDevice();
            entry.addDevice(d);
        } catch (Exception e) {
            log.warn("failed to save discovery-result", e);
        } finally {
            entry.close();
        }
    }
}