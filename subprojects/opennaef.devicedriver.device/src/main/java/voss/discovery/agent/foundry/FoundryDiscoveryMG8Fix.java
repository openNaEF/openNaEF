package voss.discovery.agent.foundry;


import voss.discovery.agent.common.Constants;
import voss.discovery.agent.common.DeviceInfoUtil;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.model.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;

public class FoundryDiscoveryMG8Fix extends FoundryGenericDiscovery {

    protected final FoundryMG8Mib method;

    public FoundryDiscoveryMG8Fix(DeviceAccess access) {
        super(access);
        this.method = new FoundryMG8Mib(access.getSnmpAccess());
    }

    @Override
    public void getDeviceInformation() throws IOException, AbortedException {
        device.setCommunityRO(getDeviceAccess().getSnmpAccess().getCommunityString());
        device.setIpAddress(getDeviceAccess().getTargetAddress().getHostAddress());
        device.setDeviceName(mib2.getSysName());
        device.setSite(getDeviceAccess().getNodeInfo().getSiteName());
        device.setVendorName(Constants.VENDOR_FOUNDRY);
        device.setModelTypeName(method.getModelName());
        String ipAddress = getDeviceAccess().getTargetAddress().getHostAddress();
        String subnetmask = mib2.getSubnetMaskByIpAddress(ipAddress);
        device.setIpAddress(ipAddress + "/" + subnetmask);
        device.setOsTypeName("IronWare");
        device.setOsVersion(method.getOSVersion());
        device.setGatewayAddress(mib2.getGatewayAddress());
        device.setTrapReceiverAddresses(mib2.getTrapReceiverAddresses());
        device.setSyslogServerAddresses(method.getSyslogServerAddresses()
                .toArray(new String[0]));
        device.setSysUpTime(new Date(Mib2Impl.getSysUpTimeInMilliseconds(getDeviceAccess().getSnmpAccess()).longValue()));
    }

    public void getPhysicalConfiguration() throws IOException, AbortedException {
        InterfaceMibImpl ifmib = new InterfaceMibImpl(this.getDeviceAccess().getSnmpAccess());

        PhysicalPortType[] physicalCollect = method.getPhysicalPortTypes();
        for (int i = 0; i < physicalCollect.length; i++) {
            int ifIndex = physicalCollect[i].getPortIfIndex();

            EthernetPort port = new EthernetPortImpl();
            device.addPort(port);

            port.initIfIndex(ifIndex);
            port.initPortIndex(physicalCollect[i].getPortNumber());
            ifmib.setIfName(port);
            ifmib.setIfAdminStatus(port);
            ifmib.setIfOperStatus(port);
            ifmib.setIfAlias(port);
            ifmib.setIfSpeed(port);
            port.setPortAdministrativeSpeed(null);

            port.setPortTypeName(method.getPortType(ifIndex));
            port.setDuplex(method.getDuplex(ifIndex));

            Integer trunkId = method.getAggregationID(ifIndex);
            if (trunkId != null) {
                EthernetPortsAggregator lag = DeviceInfoUtil
                        .getOrCreateLogicalEthernetPortByAggregationId(device,
                                trunkId.intValue());
                lag.initIfIndex(trunkId.intValue() * -1);
                lag.addPhysicalPort(port);
            }
        }
        DeviceInfoUtil.supplementDefaultLogicalEthernetPortInfo(device);
    }

    @Override
    public void getLogicalConfiguration() throws IOException, AbortedException {
        collectVlanIf();
        bindVlanIfVsEthernet();
    }

    private void collectVlanIf() throws IOException, AbortedException {
        VlanType[] vlanType = method.getVlanTypes();
        for (int i = 0; i < vlanType.length; i++) {
            if (device.getVlanIfByVlanId(vlanType[i].getVlanID()) != null) {
                continue;
            }
            VlanIf vlan = new VlanIfImpl();
            device.addPort(vlan);
            vlan.initVlanId(vlanType[i].getVlanID());
            vlan.setVlanName(method.getVlanIfDescr(vlanType[i].getVlanID()));
        }
    }

    private void bindVlanIfVsEthernet() throws IOException, AbortedException {
        VlanPortBindings[] bindings = method.getVlanPortBindings();

        for (int i = 0; i < bindings.length; i++) {
            int vlanId = bindings[i].getVlanId();
            int ifIndex = bindings[i].getIfIndex();
            boolean isTagged = bindings[i].isTagged();

            VlanIf vlanif = device.getVlanIfByVlanId(vlanId);
            EthernetPort port = DeviceInfoUtil.getEthernetPortByIfIndex(device,
                    ifIndex);
            if (isTagged) {
                DeviceInfoUtil.addTaggedPort(vlanif, port);
            } else {
                DeviceInfoUtil.addUntaggedPort(vlanif, port);
            }
        }
    }
}