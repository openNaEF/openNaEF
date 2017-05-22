package voss.core.server.naming.naef;

import naef.dto.vlan.VlanIfDto;
import naef.mvo.ip.IpAddress;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.VlanUtil;
import voss.model.*;
import voss.model.LogicalEthernetPort.TagChanger;

public class AbsoluteNameFactory {

    public static String getAtmApsAbsoluteName(AtmAPSImpl aps) {
        return getAtmApsAbsoluteName(aps.getDevice().getDeviceName(), getAtmApsName(aps));
    }

    public static String getAtmApsAbsoluteName(String parent, String apsName) {
        return getAbsoluteName(parent, ATTR.TYPE_ATM_APS, apsName);
    }

    public static String getAtmApsName(AtmAPSImpl aps) {
        return aps.getIfName();
    }

    public static String getAtmPortAbsoluteName(String parent, AtmPortImpl atm) {
        return getAtmPortAbsoluteName(parent, getAtmPortName(atm));
    }

    public static String getAtmPortAbsoluteName(String parent, String atmName) {
        return getAbsoluteName(parent, ATTR.TYPE_JACK, atmName)
                + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_ATM_PORT + ATTR.NAME_DELIMITER_SECONDARY;
    }

    public static String getAtmPortName(AtmPortImpl atm) {
        return String.valueOf(atm.getPortIndex());
    }

    public static String getAtmVpAbsoluteName(String parent, AtmVp vp) {
        return getAtmVpAbsoluteName(parent, getAtmVpName(vp));
    }

    public static String getAtmVpAbsoluteName(String parent, String vpName) {
        return getAbsoluteName(parent, ATTR.TYPE_ATM_PVP_IF, vpName);
    }

    public static String getAtmVpName(AtmVp vp) {
        return String.valueOf(vp.getVpi());
    }

    public static String getAtmPvcAbsoluteName(String parent, AtmPvc pvc) {
        return getAtmPvcAbsoluteName(parent, getAtmPvcName(pvc));
    }

    public static String getAtmPvcAbsoluteName(String parent, String pvcName) {
        return getAbsoluteName(parent, ATTR.TYPE_ATM_PVC_IF, pvcName);
    }

    public static String getAtmPvcName(AtmPvc pvc) {
        return String.valueOf(pvc.getVci());
    }

    public static String getChassisAbsoluteName(String parent, Module chassis) {
        return getChassisAbsoluteName(parent, getChassisName(chassis));
    }

    public static String getChassisAbsoluteName(String parent, String chassisName) {
        if (chassisName == null) {
            chassisName = "";
        }
        return getAbsoluteName(parent, ATTR.TYPE_CHASSIS, chassisName);
    }

    public static String getChassisName(Module chassis) {
        if (chassis == null) {
            return "";
        }
        return chassis.getSystemDescription();
    }

    public static String getEthernetPortAbsoluteName(String parent, EthernetPort port) {
        return getEthernetPortAbsoluteName(parent, getEthernetPortName(port));
    }

    public static String getEthernetPortAbsoluteName(String parent, String portName) {
        return getAbsoluteName(parent, ATTR.TYPE_JACK, portName)
                + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_ETH_PORT + ATTR.NAME_DELIMITER_SECONDARY;
    }

    public static String getEthernetPortName(EthernetPort port) {
        if (port.getDevice().isVirtualDevice()) {
            return port.getIfName();
        }
        return String.valueOf(port.getPortIndex());
    }

    public static String getEthernetEPSAbsoluteName(EthernetProtectionPort eps) {
        return getEthernetEPSAbsoluteName(eps.getDevice().getDeviceName(), getEthernetEPSName(eps));
    }

    public static String getEthernetEPSAbsoluteName(String deviceName, String epsName) {
        return getAbsoluteName(deviceName, ATTR.TYPE_LAG_PORT, epsName);
    }

    public static String getEthernetEPSName(EthernetProtectionPort eps) {
        return eps.getIfName();
    }

    public static String getEthernetLAGAbsoluteName(EthernetPortsAggregator lag) {
        return getEthernetLAGAbsoluteName(lag.getDevice().getDeviceName(), getEthernetLAGName(lag));
    }

    public static String getEthernetLAGAbsoluteName(String deviceName, String lagName) {
        return getAbsoluteName(deviceName, ATTR.TYPE_LAG_PORT, lagName);
    }

    public static String getEthernetLAGName(EthernetPortsAggregator lag) {
        return lag.getIfName();
    }

    public static String getIfNameAbsoluteName(String nodeName, String ifName) {
        String nodePart = ATTR.TYPE_NODE + ATTR.NAME_DELIMITER_SECONDARY + nodeName;
        return getAbsoluteName(nodePart, ATTR.TYPE_IFNAME, ifName);
    }

    public static String getIfIndexAbsoluteName(String nodeName, int ifIndex) {
        String nodePart = ATTR.TYPE_NODE + ATTR.NAME_DELIMITER_SECONDARY + nodeName;
        return getAbsoluteName(nodePart, ATTR.TYPE_IFINDEX, String.valueOf(ifIndex));
    }

    public static String getLoopbackAbsoluteName(LoopbackInterface loopback) {
        return getLoopbackAbsoluteName(loopback.getDevice().getDeviceName(), getLoopbackName(loopback));
    }

    public static String getLoopbackAbsoluteName(String deviceName, String loopbackName) {
        return getAbsoluteName(deviceName, ATTR.TYPE_IP_PORT, loopbackName);
    }

    public static String getLoopbackName(LoopbackInterface loopback) {
        return loopback.getIfName();
    }

    public static String getModelAbsoluteName(String parent, Module module) {
        return getModelAbsoluteName(parent, getModuleName(module));
    }

    public static String getModelAbsoluteName(String parent, String moduleName) {
        return getAbsoluteName(parent, ATTR.TYPE_MODULE, moduleName);
    }

    public static String getModuleName(Module module) {
        return "";
    }

    public static String getNodeName(Device device) {
        if (device.isVirtualDevice()) {
            return getVirtualNodeName(device);
        }
        return device.getDeviceName();
    }

    public static String getNodeAbsoluteName(Device device) {
        return getNodeAbsoluteName(getNodeName(device));
    }

    public static String getNodeAbsoluteName(String deviceName) {
        return deviceName;
    }

    public static String getNodeAbsoluteName2(Device device) {
        return getNodeAbsoluteName2(getNodeName(device));
    }

    public static String getNodeAbsoluteName2(String deviceName) {
        return ATTR.TYPE_NODE + ATTR.NAME_DELIMITER_SECONDARY + deviceName;
    }

    public static String getPosApsAbsoluteName(POSAPSImpl aps) {
        return getPosApsAbsoluteName(aps.getDevice().getDeviceName(), getPosApsName(aps));
    }

    public static String getPosApsAbsoluteName(String deviceName, String apsName) {
        return getAbsoluteName(deviceName, ATTR.TYPE_POS_APS_PORT, apsName);
    }

    public static String getPosApsName(POSAPSImpl aps) {
        return aps.getIfName();
    }

    public static String getPosPortAbsoluteName(String parent, POSImpl pos) {
        return getPosPortAbsoluteName(parent, getPosPortName(pos));
    }

    public static String getPosPortAbsoluteName(String parent, String posName) {
        return getAbsoluteName(parent, ATTR.TYPE_JACK, posName)
                + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_POS_PORT + ATTR.NAME_DELIMITER_SECONDARY;
    }

    public static String getPosPortName(POSImpl pos) {
        return String.valueOf(pos.getPortIndex());
    }

    public static String getSerialPortAbsoluteName(String parent, SerialPortImpl serial) {
        return getSerialPortAbsoluteName(parent, getSerialPortName(serial));
    }

    public static String getSerialPortAbsoluteName(String parent, String serialName) {
        return getAbsoluteName(parent, ATTR.TYPE_JACK, serialName)
                + ATTR.NAME_DELIMITER_PRIMARY + ATTR.TYPE_SERIAL_PORT + ATTR.NAME_DELIMITER_SECONDARY;
    }

    public static String getSerialPortName(SerialPortImpl serial) {
        return String.valueOf(serial.getPortIndex());
    }

    public static String getSlotAbsoluteName(String parent, Slot slot) {
        return getSlotAbsoluteName(parent, getSlotName(slot));
    }

    public static String getSlotAbsoluteName(String parent, String slotName) {
        return getAbsoluteName(parent, ATTR.TYPE_SLOT, slotName);
    }

    public static String getSlotName(Slot slot) {
        return slot.getSlotId();
    }

    public static String getTagChangerAbsoluteName(String parent, TagChanger tagChanger) {
        return getTagChangerAbsoluteName(parent, getTagChangerName(tagChanger));
    }

    public static String getTagChangerAbsoluteName(String parent, String tagChangerName) {
        return getAbsoluteName(parent, ATTR.TYPE_VLAN_SEGMENT_GATEWAY_IF, tagChangerName);
    }

    public static String getTagChangerName(TagChanger tagChanger) {
        return tagChanger.getIfName();
    }

    public static String getTagChangerName(String prefix, String parent, Integer vportID) {
        return (prefix == null ? "" : prefix + ":") + parent + ":" + vportID.toString();
    }

    public static String getTdmSerialIfAbsoluteName(String parent, Channel channel) {
        return getTdmSerialIfAbsoluteName(parent, getTdmSerialIfName(channel));
    }

    public static String getTdmSerialIfAbsoluteName(String parent, String channelName) {
        return getAbsoluteName(parent, ATTR.TYPE_TDM_SERIAL_PORT, channelName);
    }

    public static String getTdmSerialIfName(Channel channel) {
        return channel.getIfName();
    }

    public static String getVirtualNodeName(Device device) {
        if (!device.isVirtualDevice()) {
            throw new IllegalArgumentException("not virtual-device: " + device.getDeviceName());
        }
        Device hostDevice = device.getPhysicalDevice();
        return getVirtualNodeName(hostDevice.getDeviceName(), device.getDeviceName());
    }

    public static String getVirtualNodeName(String host, String guest) {
        return guest + "@" + host;
    }

    public static String getVlanIfAbsoluteName(String parent, VlanIf vlanIf) {
        return getVlanIfAbsoluteName(parent, getVlanIfName(vlanIf));
    }

    public static String getVlanIfName(VlanIf vlanIf) {
        if (vlanIf instanceof RouterVlanIf) {
            return vlanIf.getIfName();
        } else {
            int id = vlanIf.getVlanId();
            return getSwitchVlanIfName(id);
        }
    }

    public static String getVlanIfAbsoluteName(String parent, VlanIfDto vlanIf) {
        return getVlanIfAbsoluteName(parent, getVlanIfName(vlanIf));
    }

    public static String getVlanIfName(VlanIfDto vlanIf) {
        if (VlanUtil.isRouterVlanIf(vlanIf)) {
            return DtoUtil.getIfName(vlanIf);
        } else {
            int id = vlanIf.getVlanId();
            return getSwitchVlanIfName(id);
        }
    }

    public static String getVlanIfAbsoluteName(String parent, String vlanIfName) {
        return getAbsoluteName(parent, ATTR.TYPE_VLAN_IF, vlanIfName);
    }

    public static String getSwitchVlanIfName(int vlanID) {
        return String.format("vlan%04d", vlanID);
    }

    public static String getVrfIfAbsoluteName(String parent, VrfInstance vrf) {
        return getVrfIfAbsoluteName(parent, getVrfIfName(vrf));
    }

    public static String getVrfIfAbsoluteName(String nodeName, String vrfIfName) {
        return getAbsoluteName(nodeName, ATTR.TYPE_VRF_IF, vrfIfName);
    }

    public static String getVrfIfName(VrfInstance vrf) {
        return vrf.getVrfID();
    }

    public static String getVplsIfAbsoluteName(String parent, VplsInstance vpls) {
        return getVplsIfAbsoluteName(parent, getVplsIfName(vpls));
    }

    public static String getVplsIfAbsoluteName(String nodeName, String vplsIfName) {
        return getAbsoluteName(nodeName, ATTR.TYPE_VPLS_IF, vplsIfName);
    }

    public static String getVplsIfName(VplsInstance vpls) {
        return vpls.getVplsID();
    }

    protected static String getAbsoluteName(String parent, String type, String value) {
        if (value == null) {
            value = "";
        }
        return InventoryBuilder.appendContext(parent, type, value);
    }

    public static String toIpSubnetName(String vpnPrefix, String startAddress, Integer maskLength) {
        if (startAddress == null || maskLength == null) {
            throw new IllegalArgumentException();
        }
        IpAddress addr = IpAddress.gain(startAddress);
        StringBuilder sb = new StringBuilder();
        if (vpnPrefix != null) {
            sb.append(vpnPrefix);
            sb.append(ATTR.VPN_DELIMITER);
        }
        sb.append(toHexFormat(addr));
        sb.append(ATTR.VPN_DELIMITER);
        sb.append(maskLength);
        return sb.toString();
    }

    public static String toIpSubnetAddressName(String vpnPrefix, String ipAddress, Integer maskLength) {
        if (ipAddress == null || maskLength == null) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("IP:");
        sb.append(toIpSubnetName(vpnPrefix, ipAddress, maskLength));
        return sb.toString();
    }

    public static String toHexFormat(IpAddress addr) {
        return String.format("%0" + Integer.toString(addr.ipVersionBitLength() / 4) + "x", addr.toBigInteger());
    }
}