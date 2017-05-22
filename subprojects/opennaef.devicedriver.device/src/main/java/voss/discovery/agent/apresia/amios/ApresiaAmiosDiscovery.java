package voss.discovery.agent.apresia.amios;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.apresia.apware.ApresiaSwitchImpl;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.agent.util.DiscoveryUtils;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.simpletelnet.NullMode;
import voss.discovery.iolib.simulation.SimulationEntry;
import voss.model.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class ApresiaAmiosDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static Logger log = LoggerFactory.getLogger(ApresiaAmiosDiscovery.class);

    private final ApresiaAmiosMib amiosMib;
    private final ApresiaSwitchImpl device;
    private final Mib2Impl mib2;
    private final InterfaceMibImpl ifmib;
    private final ConsoleCommand show_config = new ConsoleCommand(new NullMode(), "show configuration ram primary");
    private final ConsoleCommand show_system = new ConsoleCommand(new NullMode(), "show system");
    private final ConsoleCommand show_status_card = new ConsoleCommand(new NullMode(), "show status card");
    private final ConsoleCommand show_interface_detail = new ConsoleCommand(new NullMode(), "show interface detail");
    private final ConsoleCommand show_eoe = new ConsoleCommand(new NullMode(), "show eoe");
    private final ConsoleCommand show_lag = new ConsoleCommand(new NullMode(), "show lag");
    private final ConsoleCommand show_mmrp = new ConsoleCommand(new NullMode(), "show mmrp");
    private final ConsoleCommand show_vdr = new ConsoleCommand(new NullMode(), "show vdr 1-8");

    private final String recording_oid = ".1.3.6.1.2.1";

    public ApresiaAmiosDiscovery(DeviceAccess access) {
        super(access);
        this.amiosMib = new ApresiaAmiosMib(access.getSnmpAccess());
        this.device = new ApresiaSwitchImpl();
        this.mib2 = new Mib2Impl(access.getSnmpAccess());
        this.ifmib = new InterfaceMibImpl(access.getSnmpAccess());

        device.setVendorName(Constants.VENDOR_APRESIA);
        device.setEoeEnable(true);
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException, AbortedException {
        log.info("record(): begin - " + getDeviceAccess().getTargetAddress().getHostAddress());
        SimulationEntry entry = recorder.addEntry(getDeviceAccess().getTargetAddress().getHostAddress());

        entry.addMibDump(getDeviceAccess().getNodeInfo(), recording_oid);

        ConsoleAccess console = getDeviceAccess().getConsoleAccess();
        if (console != null) {
            console.connect();
            String res = console.getResponse(show_config);
            entry.addConsoleResult(show_config, res);

            res = console.getResponse(show_system);
            entry.addConsoleResult(show_system, res);

            res = console.getResponse(show_status_card);
            entry.addConsoleResult(show_status_card, res);

            res = console.getResponse(show_interface_detail);
            entry.addConsoleResult(show_interface_detail, res);

            res = console.getResponse(show_eoe);
            entry.addConsoleResult(show_eoe, res);

            res = console.getResponse(show_lag);
            entry.addConsoleResult(show_lag, res);

            res = console.getResponse(show_mmrp);
            entry.addConsoleResult(show_mmrp, res);

            res = console.getResponse(show_vdr);
            entry.addConsoleResult(show_vdr, res);

            console.close();
        }

        entry.close();
        log.info("record(): end - " + getDeviceAccess().getTargetAddress().getHostAddress());
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
        device.setModelTypeName(amiosMib.getModelName());
        device.setIpAddress(getDeviceAccess().getNodeInfo().getFirstIpAddress().getHostAddress());
        device.setOsTypeName(amiosMib.getOsTypeName());
        device.setOsVersion(amiosMib.getOSVersion());
        device.setGatewayAddress(amiosMib.getGatewayAddress());
        device.setSyslogServerAddresses(amiosMib.getSyslogServerAddresses());

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
        collectVlanIf();
        bindVlanIfVsLAG();
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

    private void collectSlotsAndModule() throws IOException, AbortedException {
        List<SlotType> slotTypes = amiosMib.getSlotTypes();
        Map<Integer, Integer> slotStatusMap = amiosMib.getSlotStatus();
        Map<Integer, String> serialNumberMap = amiosMib.getModuleSerialNumber();

        if (slotTypes.size() == 0) {
            collectPorts();
        }

        for (SlotType slotType : slotTypes) {
            Slot slot = new SlotImpl();
            int slotIndex = slotType.getMibIndex();
            Integer slotStatus = slotStatusMap.get(slotIndex);
            if (slotStatus == null) {
                throw new IllegalStateException("slot found, but slot status is null.");
            }

            if (slotStatus.intValue() == ApresiaAmiosMib.SLOT_NOT_PRESENCE) {
                continue;
            }
            slot.initContainer(device);
            slot.initSlotIndex(slotType.getMibIndex());
            slot.initSlotId(slotType.getSlotName());
            if (slotType.getModuleName() != null) {
                Module module = new ModuleImpl();
                module.initSlot(slot);
                module.setModelTypeName(slotType.getModuleName());
                collectPorts(slotType, slot, module);
                String serialNumber = serialNumberMap.get(slotIndex);
                if (serialNumber != null) {
                    module.setSerialNumber(serialNumber);
                }
            }
        }
    }

    private void collectPorts(SlotType slotType, Slot slot, Module module) throws IOException, AbortedException {
        List<PhysicalPortType> physicalCollects = amiosMib.getPhysicalPortTypes(slotType.getMibIndex());
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
            port.setPortName(amiosMib.getPortName(ifIndex));
            port.setAdminStatus(amiosMib.getAdminStatus(ifIndex));
            port.setPortOperationalSpeed(amiosMib.getOperationalSpeed(ifIndex));
            port.setPortAdministrativeSpeed(amiosMib.getAdminSpeed(ifIndex));
            port.setDuplex(amiosMib.getDuplex(ifIndex));

            int aggregationID = amiosMib.getAggregationID(ifIndex);
            if (aggregationID != -1) {
                createEthernetAggregator(port, aggregationID);
            }
        }
    }

    private void collectPorts() throws IOException, AbortedException {
        List<PhysicalPortType> mibPorts = amiosMib.getPhysicalPortTypes();
        for (PhysicalPortType mibPort : mibPorts) {
            int ifIndex = mibPort.getIfIndex();

            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initPortIndex(mibPort.getPortIndex());
            port.initIfName(mibPort.getIfNameWithoutSlotIndex());
            port.initIfIndex(ifIndex);

            port.setPortTypeName(mibPort.getPortType());
            port.setPortName(amiosMib.getPortName(ifIndex));
            port.setAdminStatus(amiosMib.getAdminStatus(ifIndex));
            ifmib.setIfOperStatus(port);
            port.setPortOperationalSpeed(amiosMib.getOperationalSpeed(ifIndex));
            port.setPortAdministrativeSpeed(amiosMib.getAdminSpeed(ifIndex));
            port.setDuplex(amiosMib.getDuplex(ifIndex));

            int aggregationID = amiosMib.getAggregationID(ifIndex);
            if (aggregationID != -1) {
                createEthernetAggregator(port, aggregationID);
            }
        }
    }

    private void createEthernetAggregator(EthernetPort port, int aggregationID) throws IOException, AbortedException {
        EthernetPortsAggregator lag = DeviceInfoUtil
                .getOrCreateEthernetPortsAggregatorInfoByIfIndex(device, 41000 + aggregationID);

        String aggregationName = amiosMib.getAggregationName(aggregationID);
        if (aggregationName.equals("")) {
            aggregationName = "unnamed_lag(" + aggregationID + ")";
        }
        if (lag.getIfName() == null) {
            lag.initAggregationGroupId(aggregationID);
            lag.initIfName(aggregationName);
            lag.setAggregationName(aggregationName);
        }
        lag.addPhysicalPort(port);
    }

    private void collectVlanIf() throws IOException, AbortedException {
        List<VlanNameEntry> vlanTypes = amiosMib.getVlanNames();
        for (VlanNameEntry vlanType : vlanTypes) {
            int eoeID = vlanType.getEoeID();
            int vlanID = vlanType.getVlanID();
            if (device.getVlanIfBy(eoeID, vlanID) != null) {
                continue;
            }
            String vlanName = vlanType.getValue();
            if (vlanName == null || vlanName.isEmpty()) {
                vlanName = "vlan" + eoeID + "." + vlanID;
            }
            log.debug("+vlan-if " + eoeID + "." + vlanID + " " + vlanName);
            VlanIf vlan = new VlanIfImpl();
            device.addPort(vlan);
            vlan.initIfIndex(vlanType.getVlanIfIndex());
            vlan.initVlanId(eoeID, vlanID);
            vlan.initIfName(vlanName);
            vlan.setVlanName(vlanName);
        }
    }

    private void collectMmrp() throws IOException, AbortedException {
        this.amiosMib.buildMmrp(device);
    }

    private void bindVlanIfVsLAG() throws IOException, AbortedException {
        List<IntVlanLagPortModeEntry> entries = amiosMib.getVlanLagPortModes();
        for (IntVlanLagPortModeEntry entry : entries) {
            log.debug("bindVlanIfVsLAG: found entry: " + entry.toString());
            VlanIf vlanIf = device.getVlanIfBy(entry.getEoeID(), entry.getVlanID());
            LogicalEthernetPort lag = device.getEthernetPortsAggregatorByAggregationGroupId(entry.getLagIndex());
            if (vlanIf == null || lag == null) {
                log.warn("bindVlanIfVsLAG: vlanIf or lag not found: vlanIf=" + vlanIf + ", lag=" + lag);
                continue;
            }
            if (entry.isBindAsTagged()) {
                vlanIf.addTaggedPort(lag);
                log.debug("- Bound as tagged: " + vlanIf.getIfName() + "->" + lag.getIfName());
            } else if (entry.isBindAsUntagged()) {
                vlanIf.addUntaggedPort(lag);
                log.debug("- Bound as untagged: " + vlanIf.getIfName() + "->" + lag.getIfName());
            } else {
                log.debug("- Not bound: port-mode is unknown; " + vlanIf.getIfName() + "->" + lag.getIfName());
            }
        }
    }

    private void bindVlanIfVsEthernet() throws IOException, AbortedException {
        List<IntVlanPortModeEntry> entries = amiosMib.getVlanPortModes();
        for (IntVlanPortModeEntry entry : entries) {
            log.debug("bindVlanIfVsEthernet: found entry: " + entry.toString());
            VlanIf vlanIf = device.getVlanIfBy(entry.getEoeID(), entry.getVlanID());
            Port port = device.getPortByIfIndex(entry.getPortIfIndex());
            LogicalEthernetPort le = DiscoveryUtils.getLogicalEthernetPort(port);
            if (vlanIf == null || le == null) {
                log.warn("bindVlanIfVsEthernet: vlanIf or lag not found: vlanIf=" + vlanIf + ", logicalEth=" + le);
                continue;
            }
            if (entry.isBindAsTagged()) {
                vlanIf.addTaggedPort(le);
                log.debug("- Bound as tagged: " + vlanIf.getIfName() + "->" + le.getIfName());
            } else if (entry.isBindAsUntagged()) {
                vlanIf.addUntaggedPort(le);
                log.debug("- Bound as untagged: " + vlanIf.getIfName() + "->" + le.getIfName());
            } else {
                log.debug("- Not bound: port-mode is unknown; " + vlanIf.getIfName() + "->" + le.getIfName());
            }
        }
    }
}