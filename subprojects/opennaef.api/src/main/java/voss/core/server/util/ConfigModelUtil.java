package voss.core.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.*;

import java.util.*;

public class ConfigModelUtil {
    private static final Logger log = LoggerFactory.getLogger(ConfigModelUtil.class);

    public static List<MplsTunnel> getWholeRsvpLsps(Collection<Device> devices) {
        List<MplsTunnel> tunnels = new ArrayList<MplsTunnel>();
        for (Device device : devices) {
            if (device instanceof MplsVlanDevice) {
                MplsVlanDevice mplsDevice = (MplsVlanDevice) device;
                for (MplsTunnel tunnel : mplsDevice.getTeTunnels()) {
                    tunnels.add(tunnel);
                }
            } else {
                log.debug("not a mpls-vlan-device: " + device.getDeviceName() + "(" + device.getClass().getName() + ")");
            }
        }
        return tunnels;
    }

    public static List<PseudoWirePort> getWholePseudoWires(Collection<Device> devices) {
        List<PseudoWirePort> tunnels = new ArrayList<PseudoWirePort>();
        for (Device device : devices) {
            if (device instanceof MplsVlanDevice) {
                MplsVlanDevice mplsDevice = (MplsVlanDevice) device;
                for (PseudoWirePort pw : mplsDevice.getPseudoWirePorts()) {
                    tunnels.add(pw);
                }
            } else {
                log.debug("not a mpls-vlan-device: " + device.getDeviceName());
            }
        }
        return tunnels;
    }

    public static List<NodePipe<?>> getWholeNodePipes(Collection<Device> devices) {
        List<NodePipe<?>> tunnels = new ArrayList<NodePipe<?>>();
        for (Device device : devices) {
            if (device instanceof MplsVlanDevice) {
                for (NodePipe<?> pipe : device.selectPorts(NodePipe.class)) {
                    tunnels.add((NodePipe<?>) pipe);
                }
            } else {
                log.debug("not a mpls-vlan-device: " + device.getDeviceName());
            }
        }
        return tunnels;
    }

    public static String getDeviceName(VlanModel model) {
        if (model instanceof Device) {
            return ((Device) model).getDeviceName();
        } else if (model instanceof Slot) {
            Slot slot = (Slot) model;
            return slot.getDevice().getDeviceName();
        } else if (model instanceof Module) {
            Module module = (Module) model;
            return module.getDevice().getDeviceName();
        } else if (model instanceof Port) {
            Port port = (Port) model;
            return port.getDevice().getDeviceName();
        } else if (model instanceof Slot) {
            Slot slot = (Slot) model;
            return slot.getDevice().getDeviceName();
        }
        return null;
    }

    public static String getLocalName(VlanModel model) {
        if (model instanceof Device) {
            return null;
        } else if (model instanceof Slot) {
            Slot slot = (Slot) model;
            return slot.getSlotId();
        } else if (model instanceof Module) {
            Module module = (Module) model;
            return module.getModelTypeName();
        } else if (model instanceof Port) {
            Port port = (Port) model;
            return port.getIfName();
        }
        return null;
    }

    public static String getTypeName(VlanModel model) {
        if (model instanceof Device) {
            return "Node";
        } else if (model instanceof Slot) {
            return "Slot";
        } else if (model instanceof Module) {
            return "Module";
        } else if (model instanceof Port) {
            return "Port";
        } else if (model instanceof Link) {
            return "Link";
        } else if (model instanceof VlanStpElement) {
            return "VLAN-STP";
        } else {
            throw new IllegalArgumentException("Unknown type: " + model.getClass().getName());
        }
    }

    public static boolean isJuniper(Port port) {
        if (port == null) {
            return false;
        }
        Device d = port.getDevice();
        String vendor = d.getVendorName();
        if (vendor == null) {
            return false;
        }
        return vendor.equals(voss.discovery.agent.common.Constants.VENDOR_JUNIPER);
    }

    public static String toName(VlanModel model) {
        String typeName = getTypeName(model);
        StringBuilder sb = new StringBuilder();
        sb.append(typeName).append(":");
        if (Port.class.isInstance(model)) {
            sb.append(((Port) model).getFullyQualifiedName());
        } else if (Device.class.isInstance(model)) {
            sb.append(((Device) model).getDeviceName());
        } else if (Slot.class.isInstance(model)) {
            Slot slot = Slot.class.cast(model);
            sb.append(slot.getDevice().getDeviceName());
            sb.append(":");
            sb.append(getSlotName(slot));
        } else if (Module.class.isInstance(model)) {
            Module module = Module.class.cast(model);
            sb.append(module.getDevice().getDeviceName());
            sb.append(":");
            sb.append(getSlotName(module.getSlot()));
        } else if (Link.class.isInstance(model)) {
            Link link = Link.class.cast(model);
            sb.append(link.getPort1().getFullyQualifiedName());
            sb.append(":");
            sb.append(link.getPort2().getFullyQualifiedName());
        } else if (VlanStpElement.class.isInstance(model)) {
            VlanStpElement stp = VlanStpElement.class.cast(model);
            sb.append(stp.getDevice().getDeviceName());
            sb.append(":");
            sb.append(stp.getVlanStpElementId());
        } else {
            throw new IllegalArgumentException("Unknown type: " + model.getClass().getName());
        }
        return sb.toString();
    }

    public static String getSlotName(Slot slot) {
        StringBuilder sb = new StringBuilder();
        getSlotName(sb, slot);
        return sb.toString();
    }

    private static void getSlotName(StringBuilder sb, Slot slot) {
        if (slot == null) {
            return;
        } else {
            Slot parent = getParentSlot(slot);
            if (parent != null) {
                getSlotName(sb, parent);
            }
            sb.append("/");
            sb.append(slot.getSlotId());
        }
    }

    public static Slot getParentSlot(Slot slot) {
        if (slot == null) {
            return null;
        }
        Container parent = slot.getContainer();
        if (Module.class.isInstance(parent)) {
            Module m = Module.class.cast(parent);
            return m.getSlot();
        } else {
            return null;
        }
    }

    public static String getIfName(VlanModel model) {
        if (model == null || !(model instanceof Port)) {
            return null;
        }
        Port port = (Port) model;
        if (ConfigModelUtil.isJuniper(port)) {
            String portIfName = port.getIfName();
            if (portIfName == null) {
                throw new IllegalStateException("no ifName: device=" + port.getDevice().getDeviceName());
            }
            if (portIfName.endsWith(".0")) {
                portIfName = portIfName.substring(0, portIfName.length() - 2);
            }
            return portIfName;
        } else {
            return port.getIfName();
        }
    }

    public static String getIpAddress(Port port, boolean tryAssociatedPort) {
        if (port == null) {
            return null;
        }
        Set<CidrAddress> addresses = port.getDevice().getIpAddresses(port);
        if (addresses.size() > 0) {
            CidrAddress address = addresses.iterator().next();
            String ipAddress = address.getAddress().getHostAddress();
            return ipAddress;
        }
        if (!tryAssociatedPort || port.getAssociatedPort() == null) {
            log.debug("- getIpAddress: no ip address");
            return null;
        }
        Port associated = port.getAssociatedPort();
        return getIpAddress(associated, false);
    }

    public static String getVpnIpAddress(Port port) {
        Device device = port.getDevice();
        if (MplsVlanDevice.class.isInstance(device)) {
            VrfInstance vrf = ((MplsVlanDevice) device).getPortRelatedVrf(port);
            if (vrf != null) {
                CidrAddress vpnAddr = vrf.getVpnIpAddress(port);
                if (vpnAddr != null) {
                    return vpnAddr.getAddress().getHostAddress();
                }
            }
        }
        return null;
    }

    public static String getSubnetMask(Port port, boolean tryAssociatedPort) {
        if (port == null) {
            return null;
        }
        Set<CidrAddress> addresses = port.getDevice().getIpAddresses(port);
        if (addresses.size() > 0) {
            CidrAddress address = addresses.iterator().next();
            String maskLength = String.valueOf(address.getSubnetMaskLength());
            return maskLength;
        }
        if (!tryAssociatedPort || port.getAssociatedPort() == null) {
            log.debug("- getIpAddress: no ip address");
            return null;
        }
        Port associated = port.getAssociatedPort();
        return getSubnetMask(associated, false);
    }

    public static String getIpIfName(VlanModel model) {
        if (model == null || !(model instanceof Port)) {
            return null;
        }
        Port port = (Port) model;
        if (ConfigModelUtil.isJuniper(port)) {
            if (hasIpAddress(port)) {
                return null;
            }
            Port associated = port.getAssociatedPort();
            if (associated != null && hasIpAddress(associated)) {
                return associated.getIfName();
            }
        }
        return null;
    }

    public static boolean hasIpAddress(Port port) {
        if (port == null) {
            return false;
        }
        Device device = port.getDevice();
        Set<CidrAddress> addresses = device.getIpAddresses(port);
        return addresses.size() > 0;
    }

    public static Port getPhysicalPort(Port port) {
        if (port == null) {
            return null;
        } else if (port instanceof DefaultLogicalEthernetPort) {
            port = ((DefaultLogicalEthernetPort) port).getPhysicalPort();
        }
        return port;
    }

    public static Port getPort(Device device, String ipAddress) {
        for (Map.Entry<CidrAddress, Port> entry : device.getIpAddressesWithMask().entrySet()) {
            CidrAddress cidr = entry.getKey();
            if (cidr.getAddress() != null && cidr.getAddress().getHostAddress().equals(ipAddress)) {
                return entry.getValue();
            }
        }
        return null;
    }
}