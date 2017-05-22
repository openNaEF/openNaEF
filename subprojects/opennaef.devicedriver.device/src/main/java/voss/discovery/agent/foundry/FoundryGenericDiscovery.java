package voss.discovery.agent.foundry;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import voss.discovery.agent.common.*;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.agent.util.DeviceRecorder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.model.*;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;

public class FoundryGenericDiscovery extends DeviceDiscoveryImpl implements DeviceDiscovery {
    private final static int VLAN_ID_UPPER_LIMIT = 4093;

    protected final FoundryCollectMethods method;
    protected final EthernetSwitch device;
    protected final Mib2Impl mib2;

    public FoundryGenericDiscovery(DeviceAccess access) {
        super(access);
        this.method = new FoundryCollectMethods(access.getSnmpAccess());
        this.device = new GenericEthernetSwitch();
        this.mib2 = new Mib2Impl(access.getSnmpAccess());
    }

    @Override
    public Device getDeviceInner() {
        return this.device;
    }

    @Override
    public void getConfiguration() throws IOException, AbortedException {
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getTargetAddress().getHostAddress());
        device.setDeviceName(mib2.getSysName());
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setVendorName(Constants.VENDOR_FOUNDRY);
        device.setModelTypeName(method.getModelName());
        device.setIpAddress(method.getIpAddressWithSubnetmask(device
                .getIpAddress()));
        device.setOsTypeName("IronWare");
        device.setOsVersion(method.getOSVersion());
        device.setGatewayAddress(method.getGatewayAddress());
        device.setTrapReceiverAddresses(mib2.getTrapReceiverAddresses());
        device.setSyslogServerAddresses(method.getSyslogServerAddresses()
                .toArray(new String[0]));
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));
        setDiscoveryStatusDone(DiscoveryStatus.DEVICE_INFORMATION);
    }

    @Override
    public void getPhysicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.DEVICE_INFORMATION)) {
            getDeviceInformation();
        }
        setup();
        collectSlots();
        collectPorts();
        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);
        setDiscoveryStatusDone(DiscoveryStatus.PHYSICAL_CONFIGURATION);
    }

    private void setup() throws IOException, AbortedException {
        try {
            method.setupTrunkList();
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private void collectSlots() throws IOException, AbortedException {
        try {
            int slotNumber = method.getSnChasNumSlots();
            if (slotNumber < 2) {
                return;
            }

            for (int slotid = 1; slotid <= slotNumber; slotid++) {
                Slot slot = new SlotImpl();
                slot.initSlotIndex(slotid);
                slot.initSlotId(String.valueOf(slotid));
                slot.initContainer(device);

                String moduleName = method.getModuleName(slotid);
                if (moduleName != null) {
                    Module module = new ModuleImpl();
                    module.initSlot(slot);
                    module.setModelTypeName(moduleName);
                }
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    private void collectPorts() throws IOException, AbortedException {
        InterfaceMibImpl ifmib = new InterfaceMibImpl(getDeviceAccess().getSnmpAccess());

        PhysicalPortType[] collectedPhysicalPorts = method
                .getPhysicalPortTypes();
        for (PhysicalPortType collectedPhysicalPort : collectedPhysicalPorts) {
            EthernetPort port = new EthernetPortImpl();
            int ifIndex = collectedPhysicalPort.getIfIndex();
            int portIfIndex = collectedPhysicalPort.getPortIfIndex();
            device.addPort(port);

            int slotIndex = collectedPhysicalPort.getSlotIndex();
            if (device.getSlots().length > 0
                    && slotIndex != PhysicalPortType.NOT_PRESENT) {
                Slot slot = device.getSlotBySlotIndex(slotIndex);
                Module module = slot.getModule();
                if (module == null) {
                    throw new IllegalStateException(
                            "Module not found for port: "
                                    + collectedPhysicalPort.getIfIndex() + " ("
                                    + collectedPhysicalPort.getSlotIndex()
                                    + "/"
                                    + collectedPhysicalPort.getPortNumber()
                                    + ")");
                }
                module.addPort(port);
            }

            port.initPortIndex(collectedPhysicalPort.getPortNumber());
            port.initIfIndex(ifIndex);

            ifmib.setIfName(port);
            ifmib.setIfAlias(port);
            ifmib.setIfAdminStatus(port);
            ifmib.setIfOperStatus(port);

            port.setPortTypeName(method.getPortType(portIfIndex));
            if (method.getOperationalSpeed(ifIndex) != null) {
                port.setPortOperationalSpeed(method.getOperationalSpeed(ifIndex));
            }

            PortSpeedValue.Admin admin = method
                    .getAdminSpeed(portIfIndex);
            if (admin != null) {
                port.setPortAdministrativeSpeed(admin);
            }

            EthernetPort.Duplex duplex = method.getDuplex(portIfIndex);
            if (duplex != null) {
                port.setDuplex(duplex);
            }

            Integer trunkId = method.getAggregationID(portIfIndex);
            if (trunkId != null) {
                EthernetPortsAggregator lag = DeviceInfoUtil
                        .getOrCreateLogicalEthernetPortByAggregationId(device,
                                trunkId.intValue());
                lag.addPhysicalPort(port);
            }
        }
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        if (!isDiscoveryDone(DiscoveryStatus.PHYSICAL_CONFIGURATION)) {
            getPhysicalConfiguration();
        }
        collectVlanIf();
        bindVlanIfVsEthernet();
        setDiscoveryStatusDone(DiscoveryStatus.LOGICAL_CONFIGURATION);
    }

    private void collectVlanIf() throws IOException, AbortedException {
        VlanType[] vlanType = method.getVlanTypes();
        for (int i = 0; i < vlanType.length; i++) {
            if (device.getVlanIfByVlanId(vlanType[i].getVlanID()) != null) {
                continue;
            }
            if (vlanType[i].getVlanID() > VLAN_ID_UPPER_LIMIT) {
                continue;
            }
            VlanIf vlan = new VlanIfImpl();
            device.addPort(vlan);
            vlan.initVlanId(vlanType[i].getVlanID());
            vlan.setVlanName(method.getVlanIfDescr(vlanType[i].getVlanID()));
        }
    }

    private void bindVlanIfVsEthernet() throws IOException, AbortedException {
        VlanType[] vlanType = method.getVlanTypes();

        for (int i = 0; i < vlanType.length; i++) {
            if (vlanType[i].getVlanID() > VLAN_ID_UPPER_LIMIT) {
                continue;
            }
            VlanIf vlanIf = device.getVlanIfByVlanId(vlanType[i].getVlanID());
            EthernetPort[] ports = getEthernetPortInfo(vlanType[i].getIfIndex());

            for (int j = 0; j < ports.length; j++) {
                bindVlanIfVsEthernet(vlanType[i], vlanIf, ports[j]);
            }
        }
    }

    private EthernetPort[] getEthernetPortInfo(int ifIndex) {
        return new EthernetPort[]{DeviceInfoUtil.getEthernetPortByIfIndex(
                device, ifIndex)};
    }

    private void bindVlanIfVsEthernet(VlanType vlanType, VlanIf vlanIf,
                                      EthernetPort port) throws IOException, AbortedException {
        if (vlanIf == null || port == null) {
            return;
        }

        String taggingState = method.getPortTaggingState(vlanIf.getVlanId(),
                port.getIfIndex());
        if (taggingState.equals("Tagged")) {
            DeviceInfoUtil.addTaggedPort(vlanIf, port);
        } else if (taggingState.equals("Untagged")) {
            DeviceInfoUtil.addUntaggedPort(vlanIf, port);
        } else {
        }
    }

    @Override
    public void getNeighbor() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }

    @Override
    public String getTextConfiguration() throws IOException, AbortedException {
        return null;
    }

    @Override
    public void record(DeviceRecorder recorder) throws IOException,
            ConsoleException {
    }
}