package voss.discovery.agent.apresia.apware;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import voss.discovery.iolib.simpletelnet.NullMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpHelper.IntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class ApresiaApwareDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(ApresiaApwareDiscovery.class);

    private final ApresiaApwareMib apwareMib;
    private final ApresiaSwitchImpl device;
    private final Mib2Impl mib2;
    private final InterfaceMibImpl ifmib;
    private final ConsoleCommand show_config = new ConsoleCommand(new NullMode(), "show configuration primary flash");


    public ApresiaApwareDiscovery(DeviceAccess access) {
        super(access);
        this.apwareMib = new ApresiaApwareMib(access.getSnmpAccess());
        this.device = new ApresiaSwitchImpl();
        this.mib2 = new Mib2Impl(access.getSnmpAccess());
        this.ifmib = new InterfaceMibImpl(access.getSnmpAccess());

        device.setVendorName(Constants.VENDOR_APRESIA);
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
        getDeviceInformation();
        getPhysicalConfiguration();
        getLogicalConfiguration();
    }

    @Override
    public Device getDeviceInner() {
        return device;
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setDeviceName(mib2.getSysName());
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setModelTypeName(apwareMib.getModelName());
        device.setIpAddress(getDeviceAccess().getNodeInfo().getFirstIpAddress().getHostAddress());
        LoopbackInterface systemPort = new LoopbackInterface();
        systemPort.initDevice(device);
        systemPort.initIfIndex(0);
        systemPort.initIfName("Management");
        device.addIpAddressToPort(apwareMib.getIpAddressWithSubnetmask(), systemPort);
        device.setOsTypeName(apwareMib.getOsTypeName());
        device.setOsVersion(apwareMib.getOSVersion());
        device.setGatewayAddress(apwareMib.getGatewayAddress());
        device.setTrapReceiverAddresses(mib2.getTrapReceiverAddresses());
        device.setSyslogServerAddresses(apwareMib.getSyslogServerAddresses());

        String sysDescr = Mib2Impl.getSysDescr(getDeviceAccess().getSnmpAccess());
        device.setDescription(sysDescr);
        device.setContactInfo(Mib2Impl.getSysContact(getDeviceAccess().getSnmpAccess()));
        device.setLocation(Mib2Impl.getSysLocation(getDeviceAccess().getSnmpAccess()));
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));

        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }

        collectSlotsAndModule();
        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);

        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }

        collectVdr();
        collectVlanIf();
        bindVlanIfVsEthernet();
        collectMmrp();

        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    public void getNeighbor() throws IOException, AbortedException {
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
            String res1 = console.getResponse(show_config);
            return res1;
        } catch (ConsoleException ce) {
            throw new IOException(ce);
        }
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        log.info("record(): begin - " + getDeviceAccess().getTargetAddress().getHostAddress());
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
        log.info("record(): end - " + getDeviceAccess().getTargetAddress().getHostAddress());
    }

    private void collectSlotsAndModule() throws IOException, AbortedException {
        List<SlotType> slotTypes = apwareMib.getSlotTypes();
        Map<Integer, Integer> slotStatusMap = apwareMib.getSlotStatus();

        if (slotTypes.size() == 0) {
            collectPorts();
        }

        for (SlotType slotType : slotTypes) {
            Slot slot = new SlotImpl();
            int slotIndex = slotType.getSlotIndex();
            Integer slotStatus = slotStatusMap.get(slotIndex);
            if (slotStatus == null) {
                throw new IllegalStateException("slot found, but slot status is null.");
            }

            if (slotStatus.intValue() == ApresiaApwareMib.SLOT_NOT_PRESENCE) {
                continue;
            }

            slot.initContainer(device);
            slot.initSlotIndex(slotIndex);
            slot.initSlotId(slotType.getSlotName());
            if (slotType.getModuleName() != null) {
                Module module = new ModuleImpl();
                module.initSlot(slot);
                module.setModelTypeName(slotType.getModuleName());
                collectPorts(slot, module);
            }
        }
    }

    private void collectPorts(Slot slot, Module module) throws IOException, AbortedException {
        List<PhysicalPortType> physicalCollects = apwareMib.getPhysicalPortTypes(slot.getSlotIndex());
        for (PhysicalPortType physicalCollect : physicalCollects) {
            int ifIndex = physicalCollect.getIfIndex();

            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            module.addPort(port);
            port.initPortIndex(physicalCollect.getPortIndex());
            port.initIfName(physicalCollect.getIfNameWithSlotIndex());
            port.initIfIndex(ifIndex);

            ifmib.setIfOperStatus(port);

            port.setPortTypeName(physicalCollect.getPortType());
            port.setPortName(apwareMib.getPortName(ifIndex));
            port.setAdminStatus(apwareMib.getAdminStatus(ifIndex));
            port.setPortOperationalSpeed(apwareMib.getOperationalSpeed(ifIndex));
            port.setPortAdministrativeSpeed(apwareMib.getAdminSpeed(ifIndex));
            port.setDuplex(apwareMib.getDuplex(ifIndex));

            int aggregationID = apwareMib.getAggregationID(ifIndex);
            if (aggregationID != -1) {
                createEthernetAggregator(port, aggregationID);
            }
        }
    }

    private void collectPorts() throws IOException, AbortedException {
        List<PhysicalPortType> mibPorts = apwareMib.getPhysicalPortTypes();
        for (PhysicalPortType mibPort : mibPorts) {
            int ifIndex = mibPort.getIfIndex();

            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initPortIndex(mibPort.getPortIndex());
            port.initIfName(mibPort.getIfNameWithoutSlotIndex());
            port.initIfIndex(ifIndex);

            port.setPortTypeName(mibPort.getPortType());
            port.setPortName(apwareMib.getPortName(ifIndex));
            port.setAdminStatus(apwareMib.getAdminStatus(ifIndex));
            ifmib.setIfOperStatus(port);
            port.setPortOperationalSpeed(apwareMib.getOperationalSpeed(ifIndex));
            port.setPortAdministrativeSpeed(apwareMib.getAdminSpeed(ifIndex));
            port.setDuplex(apwareMib.getDuplex(ifIndex));

            int aggregationID = apwareMib.getAggregationID(ifIndex);
            if (aggregationID != -1) {
                createEthernetAggregator(port, aggregationID);
            }
        }
    }

    private void createEthernetAggregator(EthernetPort port, int aggregationID) throws IOException, AbortedException {
        EthernetPortsAggregator lag = DeviceInfoUtil
                .getOrCreateEthernetPortsAggregatorInfoByIfIndex(device, 41000 + aggregationID);

        String aggregationName = apwareMib.getAggregationName(aggregationID);
        if (aggregationName.equals("")) {
            aggregationName = "_unnamed_lag(" + aggregationID + ")";
        }
        if (lag.getIfName() == null) {
            lag.initAggregationGroupId(aggregationID);
            lag.initIfName(aggregationName);
            lag.setAggregationName(aggregationName);
        }
        lag.addPhysicalPort(port);
    }

    private void collectVlanIf() throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> ifAdminStatuses =
                SnmpUtil.getWalkResult(getSnmpAccess(), InterfaceMib.ifAdminStatus,
                        SnmpHelper.intEntryBuilder, SnmpHelper.integerKeyCreator);

        List<VlanType> vlanTypes = apwareMib.getVlanTypes();
        for (VlanType vlanType : vlanTypes) {
            if (device.getVlanIfByVlanId(vlanType.getVlanID()) != null) {
                continue;
            }
            VlanIf vlan = new VlanIfImpl();
            device.addPort(vlan);
            vlan.initVlanId(vlanType.getVlanID());
            vlan.initVlanIfIndex(vlanType.getVlanIfIndex());

            BigInteger b = BigInteger.valueOf(vlanType.getVlanIfIndex());
            IntegerKey key = new IntegerKey(new BigInteger[]{b});
            IntSnmpEntry adminStatus = ifAdminStatuses.get(key);
            boolean enabled = true;
            if (adminStatus == null) {
                throw new IllegalStateException("no admin status: ifIndex=" + vlanType.getVlanIfIndex());
            } else {
                int value = adminStatus.getValueAsBigInteger().intValue();
                switch (value) {
                    case InterfaceMib.IF_ADMIN_STATUS_UP:
                        vlan.setAdminStatus(VlanIf.STATUS_ENABLED);
                        break;
                    case InterfaceMib.IF_ADMIN_STATUS_DOWN:
                        vlan.setAdminStatus(VlanIf.STATUS_DISABLED);
                        enabled = false;
                        break;
                    case InterfaceMib.IF_ADMIN_STATUS_TESTING:
                        vlan.setAdminStatus(Port.STATUS_TESTING);
                        enabled = false;
                        break;
                    default:
                        throw new IllegalStateException("unknown ifAdminStatus value: "
                                + vlanType.getVlanIfIndex() + "->" + value);
                }
            }

            String vlanIfDescr = null;
            if (enabled) {
                vlanIfDescr = apwareMib.getVlanIfDescr(vlanType.getVlanID());
            }
            vlan.setVlanName(vlanIfDescr == null ? "" : vlanIfDescr);
        }
    }

    private void bindVlanIfVsEthernet() throws IOException, AbortedException {
        List<VlanType> vlanTypes = apwareMib.getVlanTypes();
        for (VlanType vlanType : vlanTypes) {
            VlanIf vlanIf = device.getVlanIfByVlanId(vlanType.getVlanID());
            if (vlanType.isVdrRelation()) {
                Port uplink = device.getPortByIfIndex(vlanType.getPortIfIndex());
                if (uplink instanceof ApresiaVdrUplinkPort) {
                    ApresiaVdrUplinkPort vdrUplink = (ApresiaVdrUplinkPort) uplink;
                    ApresiaVdr vdr = vdrUplink.getVdr();
                    vdr.addMemberVlanIf(vlanIf);
                    bindVlanIfVsEthernet(vlanType, vlanIf, vdr.getUplink1().getUplinkLogicalEthernetPort());
                    bindVlanIfVsEthernet(vlanType, vlanIf, vdr.getUplink2().getUplinkLogicalEthernetPort());
                } else {
                    log.warn("unknown vdr: " + vlanType.getVdrId());
                    continue;
                }
            } else {
                LogicalEthernetPort port = getLogicalEthernetPortInfo(vlanType);
                bindVlanIfVsEthernet(vlanType, vlanIf, port);
            }
        }
    }

    private LogicalEthernetPort getLogicalEthernetPortInfo(VlanType vlanType) {
        if (vlanType.isLagRelation()) {
            return device.getEthernetPortsAggregatorByAggregationGroupId(vlanType.getLagId());
        } else {
            EthernetPort ethernetPort = DeviceInfoUtil.getEthernetPortByIfIndex(device, vlanType.getPortIfIndex());
            return device.getLogicalEthernetPort(ethernetPort);
        }
    }

    private void bindVlanIfVsEthernet(VlanType vlanType, VlanIf vlanIf, LogicalEthernetPort port) {
        if (vlanIf == null || port == null) {
            return;
        }
        if (vlanType.isTagged()) {
            vlanIf.addTaggedPort(port);
        }
        if (vlanType.isUntagged()) {
            vlanIf.addUntaggedPort(port);
        }
    }

    private void collectMmrp() throws IOException, AbortedException {
        this.apwareMib.buildMmrp(device);
    }

    private void collectVdr() throws IOException, AbortedException {
        this.apwareMib.buildVdr(device);
    }
}