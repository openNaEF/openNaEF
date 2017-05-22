package voss.core.server.naming.inventory;

import naef.dto.*;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.of.OfPatchLinkDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import voss.core.server.database.ATTR;
import voss.core.server.naming.NamingUtil;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NameUtil;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.RsvpLspUtil;
import voss.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InventoryIdCalculator {
    public static final String DELIM0 = ":";
    public static final char DELIM1 = '/';
    public static final String DELIM2 = String.valueOf(DELIM1);

    public static String getId(NodeElementDto element) {
        if (element == null) {
            return null;
        } else if (!NodeDto.class.isInstance(element) && element.getOwner() == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[DELETED:");
            sb.append(DtoUtil.getMvoId(element).toString());
            sb.append("]");
            sb.append(getElementId(element));
            return sb.toString();
        }
        if (element instanceof NodeDto) {
            return getId((NodeDto) element);
        } else if (element instanceof ChassisDto) {
            return getId((ChassisDto) element);
        } else if (element instanceof SlotDto) {
            return getId((SlotDto) element);
        } else if (element instanceof ModuleDto) {
            return getId((ModuleDto) element);
        } else if (element instanceof JackDto) {
            return getId((JackDto) element);
        } else if (element instanceof PortDto) {
            return getId((PortDto) element);
        }
        throw new IllegalArgumentException("unexpected node-element-type:" + element.getAbsoluteName());
    }

    public static String getId(NodeDto node) {
        return InventoryIdBuilder.getNodeID(node.getName());
    }

    public static String getId(ChassisDto chassis) {
        StringBuilder sb = new StringBuilder();
        createId(sb, chassis);
        return InventoryIdBuilder.getNodeElementID(getElementId(chassis.getNode()), sb.toString());
    }

    public static String getId(SlotDto slot) {
        StringBuilder sb = new StringBuilder();
        createId(sb, slot);
        return InventoryIdBuilder.getNodeElementID(getElementId(slot.getNode()), sb.toString());
    }

    public static String getId(ModuleDto module) {
        StringBuilder sb = new StringBuilder();
        createId(sb, module);
        return InventoryIdBuilder.getNodeElementID(getElementId(module.getNode()), sb.toString());
    }

    public static String getId(JackDto jack) {
        StringBuilder sb = new StringBuilder();
        createId(sb, jack);
        return InventoryIdBuilder.getNodeElementID(getElementId(jack.getNode()), sb.toString());
    }

    public static String getId(PortDto port) {
        if (port.isAlias()) {
            return getAliasId(port);
        } else if (port instanceof EthLagIfDto) {
            return getId((EthLagIfDto) port);
        } else if (port instanceof InterconnectionIfDto) {
            return getId((InterconnectionIfDto) port);
        } else if (port instanceof VlanIfDto) {
            return getId((VlanIfDto) port);
        }
        String ifName = DtoUtil.getStringOrNull(port, NamingUtil.getIfNameAttributeName());
        if (ifName == null) {
            ifName = NameUtil.getDefaultIfName(port);
        }
        return InventoryIdBuilder.getPortID(port.getNode().getName(), ifName);
    }

    public static String getId(VlanIfDto vlanIf) {
        String ifName = getElementId(vlanIf);
        if (ifName == null) {
            ifName = NameUtil.getDefaultIfName(vlanIf);
        }
        return InventoryIdBuilder.getPortID(vlanIf.getNode().getName(), ifName);
    }

    public static String getId(AtmPvpIfDto vp) {
        String ifName = DtoUtil.getStringOrNull(vp, NamingUtil.getIfNameAttributeName());
        if (ifName == null) {
            ifName = NameUtil.getDefaultIfName(vp);
        }
        return InventoryIdBuilder.getPortID(vp.getNode().getName(), ifName);
    }

    public static String getId(EthLagIfDto lag) {
        String ifName = lag.getName();
        return InventoryIdBuilder.getPortID(lag.getNode().getName(), ifName);
    }

    public static String getId(InterconnectionIfDto pipe) {
        String ifName = DtoUtil.getStringOrNull(pipe, NamingUtil.getIfNameAttributeName());
        if (ifName == null) {
            ifName = NameUtil.getDefaultIfName(pipe);
        }
        return InventoryIdBuilder.getPipeID(pipe.getNode().getName(), ifName);
    }

    public static String getId(InterconnectionIfDto pipe, PortDto ac) {
        String pipeName = DtoUtil.getStringOrNull(pipe, NamingUtil.getIfNameAttributeName());
        if (pipeName == null) {
            pipeName = NameUtil.getDefaultIfName(pipe);
        }
        String ifName = DtoUtil.getStringOrNull(ac, NamingUtil.getIfNameAttributeName());
        if (ifName == null) {
            ifName = NameUtil.getDefaultIfName(ac);
        }
        return InventoryIdBuilder.getPipeID(pipe.getNode().getName(), pipeName, ifName);
    }

    public static String getId(RsvpLspDto lsp) {
        NodeDto ingress = RsvpLspUtil.getIngressNode(lsp);
        if (ingress == null) {
            throw new IllegalStateException("no ingress node.");
        }
        String lspName = DtoUtil.getStringOrNull(lsp, ATTR.LSP_NAME);
        if (lspName == null) {
            lspName = lsp.getName();
        }
        return InventoryIdBuilder.getRsvpLspID(ingress.getName(), lspName);
    }

    public static String getId(RsvpLspDto lsp, RsvpLspHopSeriesDto path) {
        NodeDto ingress = RsvpLspUtil.getIngressNode(lsp);
        if (ingress == null) {
            throw new IllegalStateException("no ingress node.");
        }
        String lspName = DtoUtil.getStringOrNull(lsp, ATTR.LSP_NAME);
        if (lspName == null) {
            lspName = lsp.getName();
        }
        String pathName = DtoUtil.getStringOrNull(path, ATTR.PATH_NAME);
        return InventoryIdBuilder.getPathID(ingress.getName(), lspName, pathName);
    }

    public static String getId(RsvpLspHopSeriesDto path) {
        NodeDto ingress = RsvpLspUtil.getIngressNode(path);
        if (ingress == null) {
            throw new IllegalStateException("no ingress node.");
        }
        String pathName = DtoUtil.getStringOrNull(path, ATTR.PATH_NAME);
        return InventoryIdBuilder.getPathID(ingress.getName(), null, pathName);
    }

    public static String getId(PseudowireDto pw) {
        if (pw.getLongId() != null) {
            return InventoryIdBuilder.getPseudoWireID(pw.getLongId().toString());
        } else if (pw.getStringId() != null) {
            return InventoryIdBuilder.getPseudoWireID(pw.getStringId().toString());
        }
        throw new IllegalStateException("unknown id-type.");
    }

    public static String getId(PseudowireDto pw, PortDto ac) {
        if (ac == null) {
            throw new IllegalArgumentException();
        }
        if (pw.getLongId() != null) {
            return InventoryIdBuilder.getPseudoWireID(ac.getNode().getName(), pw.getLongId().toString());
        } else if (pw.getStringId() != null) {
            return InventoryIdBuilder.getPseudoWireID(ac.getNode().getName(), pw.getStringId().toString());
        }
        throw new IllegalStateException("unknown id-type.");
    }

    public static String getId(VplsIfDto vpls) {
        return InventoryIdBuilder.getVplsID(getElementId(vpls.getNode()), vpls.getName());
    }

    public static String getId(VrfIfDto vrf) {
        return InventoryIdBuilder.getVrfID(getElementId(vrf.getNode()), vrf.getName());
    }

    public static String getId(LinkDto link) {
        List<String> memberNames = new ArrayList<String>();
        for (PortDto port : link.getMemberPorts()) {
            memberNames.add(escape(getId(port)));
        }
        InventoryIdType type = NodeUtil.selectType(link);
        return getId(memberNames, type);
    }

    public static String getId(IpSubnetDto subnet) {
        List<String> memberNames = new ArrayList<String>();
        for (PortDto port : subnet.getMemberIpifs()) {
            String portID = null;
            if (!IpIfDto.class.isInstance(port)) {
                portID = getId(port);
                memberNames.add(escape(portID));
                continue;
            }
            IpIfDto ipIf = (IpIfDto) port;
            for (PortDto assoc : ipIf.getAssociatedPorts()) {
                if (assoc == null) {
                    portID = getId(ipIf);
                } else {
                    portID = getId(assoc);
                }
                if (portID == null) {
                    throw new IllegalStateException();
                }
                memberNames.add(escape(portID));
            }
        }
        return getId(memberNames, InventoryIdType.L3LINK);
    }

    public static String getId(OfPatchLinkDto link) {
        List<String> attachedPorts = new ArrayList<>();
        attachedPorts.add(escape(getId(link.getPatchPort1())));
        attachedPorts.add(escape(getId(link.getPatchPort2())));
        attachedPorts.sort(Comparator.naturalOrder());
        return getId(attachedPorts, InventoryIdType.OFPLINK);
    }

    private static String getId(List<String> portNames, InventoryIdType type) {
        return getId(portNames, type, null);
    }

    private static String getId(List<String> portNames, InventoryIdType type, String customTypeName) {
        if (type == null) {
            throw new IllegalArgumentException("type is null.");
        }
        Collections.sort(portNames);
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());
        if (null != customTypeName) {
            sb.append(DELIM0).append(customTypeName);
        }
        for (String memberName : portNames) {
            sb.append(DELIM0);
            sb.append(memberName);
        }
        return sb.toString();
    }

    private static String getAliasId(PortDto alias) {
        PortDto source = alias.getAliasRootSource();
        String id = getId(source);
        return InventoryIdBuilder.getAliasID(alias.getNode().getName(), id);
    }

    private static void createId(StringBuilder sb, NodeElementDto element) {
        if (element instanceof NodeDto) {
            return;
        }
        if (element.getOwner() != null) {
            createId(sb, element.getOwner());
        }
        String elementId = getElementId(element);
        if (element instanceof ChassisDto) {
            sb.append(elementId).append(DELIM2);
        } else if (element instanceof ModuleDto) {
            sb.append(DELIM2);
        } else {
            sb.append(elementId);
        }
    }

    private static String getElementId(NodeElementDto element) {
        String s = null;
        if (element == null) {
            s = null;
        } else if (element instanceof NodeDto) {
            s = element.getName();
        } else if (element.getOwner() == null) {
            return element.getName();
        } else if (element instanceof ChassisDto) {
            s = element.getName();
        } else if (element instanceof SlotDto) {
            s = element.getName();
        } else if (element instanceof ModuleDto) {
            s = null;
        } else if (element instanceof JackDto) {
            s = element.getName();
        } else if (element instanceof VlanIfDto) {
            s = AbsoluteNameFactory.getVlanIfName((VlanIfDto) element);
        } else if (element instanceof PortDto) {
            PortDto port = (PortDto) element;
            if (port.getOwner() instanceof JackDto) {
                s = port.getOwner().getName();
            } else {
                s = element.getName();
            }
        } else {
            s = null;
        }
        return s;
    }

    public static String getElementId(VlanModel model) {
        String s = null;
        if (model instanceof Device) {
            Device device = (Device) model;
            if (device.isVirtualDevice()) {
                s = AbsoluteNameFactory.getVirtualNodeName(device);
            } else {
                s = device.getDeviceName();
            }
        } else if (model instanceof Slot) {
            s = ((Slot) model).getSlotId();
        } else if (model instanceof Module) {
            s = DELIM2;
        } else if (model instanceof EthernetPortsAggregator) {
            s = ((EthernetPortsAggregator) model).getAggregationName();
        } else if (model instanceof MplsTunnel) {
            s = ((MplsTunnel) model).getIfName();
        } else if (model instanceof LabelSwitchedPathEndPoint) {
            s = ((LabelSwitchedPathEndPoint) model).getLspName();
        } else if (model instanceof PseudoWirePort) {
            PseudoWirePort pw = (PseudoWirePort) model;
            long pwID = pw.getPseudoWireID();
            if (pwID < 1) {
                s = pw.getPwName();
            } else {
                s = String.valueOf(pwID);
            }
        } else if (model instanceof NodePipe<?>) {
            s = ((NodePipe<?>) model).getPipeName();
        } else if (model instanceof VplsInstance) {
            s = ((VplsInstance) model).getVplsID();
        } else if (model instanceof VrfInstance) {
            s = ((VrfInstance) model).getVrfID();
        } else if (model instanceof VlanIf) {
            s = AbsoluteNameFactory.getVlanIfName((VlanIf) model);
        } else if (model instanceof Port) {
            Port port = (Port) model;
            s = port.getIfName();
        } else {
            throw new IllegalArgumentException("unsupported type: " + model.getClass().getName());
        }
        return s;
    }

    public static String getId(Device device) {
        return InventoryIdBuilder.getNodeID(getElementId(device));
    }

    public static String getId(Slot slot) {
        StringBuilder sb = new StringBuilder();
        createId(sb, slot);
        return InventoryIdBuilder.getNodeElementID(getElementId(slot.getDevice()), sb.toString());
    }

    private static void createId(StringBuilder sb, Slot slot) {
        Container container = slot.getContainer();
        createId(sb, container);
        sb.append(getElementId(slot));
    }

    private static void createId(StringBuilder sb, Container container) {
        if (container instanceof Device) {
            sb.append(DELIM1);
        } else if (container instanceof Module) {
            Module module = (Module) container;
            createId(sb, module.getSlot());
            sb.append(getElementId(module));
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static String getId(Module module) {
        StringBuilder sb = new StringBuilder();
        createId(sb, module);
        return InventoryIdBuilder.getNodeElementID(getElementId(module.getDevice()), sb.toString());
    }

    public static String getId(Port port) {
        if (port.isAliasPort()) {
            return getAliasId(port);
        } else if (port instanceof MplsTunnel) {
            return getId((MplsTunnel) port);
        } else if (port instanceof EthernetPortsAggregator) {
            return getId((EthernetPortsAggregator) port);
        } else if (port instanceof LabelSwitchedPathEndPoint) {
            return getId((LabelSwitchedPathEndPoint) port);
        } else if (port instanceof PseudoWirePort) {
            return getId((PseudoWirePort) port);
        } else if (port instanceof VplsInstance) {
            return getId((VplsInstance) port);
        } else if (port instanceof VrfInstance) {
            return getId((VrfInstance) port);
        } else if (port instanceof NodePipe<?>) {
            return getId((NodePipe<?>) port);
        }
        String s = InventoryIdBuilder.getPortID(getElementId(port.getDevice()), getElementId(port));
        return s;
    }

    public static String getId(EthernetPortsAggregator lag) {
        return InventoryIdBuilder.getPortID(getElementId(lag.getDevice()), getElementId(lag));
    }

    public static String getId(MplsTunnel lsp, LabelSwitchedPathEndPoint path) {
        return InventoryIdBuilder.getPathID(getElementId(path.getDevice()), getElementId(lsp), getElementId(path));
    }

    public static String getId(LabelSwitchedPathEndPoint path) {
        return InventoryIdBuilder.getPathID(getElementId(path.getDevice()), null, getElementId(path));
    }

    public static String getId(MplsTunnel tunnel) {
        Device ingress = tunnel.getDevice();
        return InventoryIdBuilder.getRsvpLspID(getElementId(ingress), getElementId(tunnel));
    }

    public static String getId(PseudoWirePort pw) {
        return InventoryIdBuilder.getPseudoWireID(getElementId(pw));
    }

    public static String getId(NodePipe<?> pipe) {
        return InventoryIdBuilder.getPipeID(getElementId(pipe.getDevice()), getElementId(pipe));
    }

    public static String getId(NodePipe<?> pipe, Port ac) {
        return InventoryIdBuilder.getPipeID(getElementId(pipe.getDevice()), getElementId(pipe), getElementId(ac));
    }

    public static String getId(PseudoWirePort pw, Device device) {
        return InventoryIdBuilder.getPseudoWireID(getElementId(device), getElementId(pw));
    }

    public static String getId(VplsInstance vpls) {
        return InventoryIdBuilder.getVplsID(getElementId(vpls.getDevice()), getElementId(vpls));
    }

    public static String getId(VrfInstance vrf) {
        return InventoryIdBuilder.getVrfID(getElementId(vrf.getDevice()), getElementId(vrf));
    }

    public static String getId(Link link) {
        if (link == null) {
            throw new IllegalArgumentException();
        }
        String port1 = getId(link.getPort1());
        String port2 = getId(link.getPort2());
        List<String> ports = new ArrayList<String>();
        ports.add(port1);
        ports.add(port2);
        Collections.sort(ports);
        return InventoryIdBuilder.getLayer2LinkID(ports.get(0), ports.get(1));
    }

    public static String getAliasId(Port port) {
        Port source = port.getAliasSource();
        String nodeName = AbsoluteNameFactory.getVirtualNodeName(port.getDevice());
        String sourceID = getId(source);
        return InventoryIdBuilder.getAliasID(nodeName, sourceID);
    }

    public static String escape(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            int ch = s.codePointAt(i);
            if (ch == ':' || ch == '\\') {
                sb.append('\\');
            }
            sb.append((char) ch);
        }
        return sb.toString();
    }

    public static String unEscape(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int ch;
        boolean escaped = false;
        for (int i = 0; i < str.length(); i++) {
            ch = str.codePointAt(i);
            if (escaped) {
                sb.append((char) (0xffff & ch));
                escaped = false;
            } else {
                if (ch == '\\') {
                    escaped = true;
                } else {
                    sb.append((char) (0xffff & ch));
                }
            }
        }
        return sb.toString();
    }

    public static boolean isNull(String s) {
        if (s == null) {
            return true;
        } else if (s.length() == 0) {
            return true;
        }
        return false;
    }
}
