package voss.discovery.runner.simple;


import voss.discovery.agent.alcatel.AlcatelExtInfoNames;
import voss.discovery.agent.flashwave.FlashWaveExtInfoNames;
import voss.discovery.agent.juniper.JuniperJunosExtInfoNames;
import voss.discovery.agent.juniper.PromiscuousModeRenderer;
import voss.model.*;
import voss.model.LogicalEthernetPort.TagChanger;
import voss.model.value.PortSpeedValue;

import java.util.*;

public class SimpleSwitchView implements SwitchView {
    private final Device device;

    public SimpleSwitchView(Device device) {
        this.device = device;
    }

    public void view() {
        showDevice();
        showVirtualDevices();
        showInventory();
        showEthernetPortsAggregator();
        showEthernetProtectionPorts();
        showLogicalPorts();
        showPorts();
        showVlans();
        showVports();
        showVps();
        showPvcs();
        showMmrpVdr();
        showVMware();
        print("=======\r\n\r\n");
    }

    private void showDevice() {
        print("Device Name: " + device.getDeviceName());
        print("Domain Name: " + device.getDomainName());
        print("\tNode IP Address: " + device.getIpAddress());
        List<String> rawIpAddresses = Arrays.asList(device.getIpAddresses());
        List<PortAddrSortUnit> rootPortAddressUnits = getPortAddrSortUnit2(device.getIpAddressesWithMask());
        for (PortAddrSortUnit portAddressUnit : rootPortAddressUnits) {
            Port port = portAddressUnit.port;
            CidrAddress ipAddressMask = portAddressUnit.address;
            String prefix = (rawIpAddresses.contains(ipAddressMask.getAddress().getHostAddress()) ? "" : "* ");
            String suffix = port.isAssociatedPort() ? "(->" + port.getAssociatePort().getIfName() + ")" : "";
            print("\t@IP Address: " + prefix + ipAddressMask.toString() + " (" + port.getIfName() + suffix + "[" + port.getClass().getSimpleName() + "])");
        }
        if (MplsVlanDevice.class.isInstance(device)) {
            for (VrfInstance vrf : ((MplsVlanDevice) device).getVrfs()) {
                String prefix = vrf.getIfName();
                List<PortAddrSortUnit> portAddressUnits = getPortAddrSortUnit(vrf.getVpnPortAddressMap());
                for (PortAddrSortUnit portAddressUnit : portAddressUnits) {
                    Port port = portAddressUnit.port;
                    CidrAddress ipAddressMask = portAddressUnit.address;
                    String suffix = port.isAssociatedPort() ? "(->" + port.getAssociatePort().getIfName() + ")" : "";
                    print("\t@VPN IP Address: " + prefix + ":" + ipAddressMask.toString() + " (" + port.getIfName() + suffix + ")");
                }
            }
        }
        print("\tVendor: " + device.getVendorName());
        print("\tType: " + device.getModelTypeName());
        print("\tOS Type: " + device.getOsTypeName());
        print("\tOS Version: " + device.getOsVersion());
        print("\tSerial Number: " + device.getSerialNumber());
        print("\tDescription: " + device.getDescription());
        print("");
    }

    private void showVirtualDevices() {
        print("Virtual Device Information");
        for (Device vd : this.device.getVirtualDevices()) {
            showVirtualDevice(vd);
        }
        print("");
    }

    private void showVirtualDevice(Device device) {
        print("Virtual Device Name: " + device.getDeviceName());
        if (device.getDomainName() != null) {
            print("\tDomain Name: " + device.getDomainName());
        }
        print("\tVirtual Port Information (" + device.getPorts().length + ")");
        for (Port port : device.getPorts()) {
            if (port.isAliasPort()) {
                print("\t\tALIAS: " + port.getIfName() + " -> " + port.getAliasSource().getFullyQualifiedName());
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(port.getIfName());
                Set<CidrAddress> addrs = device.getIpAddresses(port);
                if (addrs.size() > 0) {
                    sb.append(" (IP: ");
                    for (CidrAddress addr : addrs) {
                        sb.append(addr.toString());
                        sb.append(", ");
                    }
                    sb.append(")");
                }
                print("\t\tPRIVATE: " + sb.toString());
            }
        }
    }

    private List<PortAddrSortUnit> getPortAddrSortUnit(Map<Port, CidrAddress> map) {
        List<PortAddrSortUnit> result = new ArrayList<PortAddrSortUnit>();
        for (Map.Entry<Port, CidrAddress> entry : map.entrySet()) {
            Port p = entry.getKey();
            CidrAddress addr = entry.getValue();
            if (p == null || addr == null) {
                continue;
            }
            PortAddrSortUnit unit = new PortAddrSortUnit();
            unit.port = p;
            unit.address = addr;
            result.add(unit);
        }
        Collections.sort(result);
        return result;
    }

    private List<PortAddrSortUnit> getPortAddrSortUnit2(Map<CidrAddress, Port> map) {
        List<PortAddrSortUnit> result = new ArrayList<PortAddrSortUnit>();
        for (Map.Entry<CidrAddress, Port> entry : map.entrySet()) {
            CidrAddress addr = entry.getKey();
            Port p = entry.getValue();
            if (p == null || addr == null) {
                continue;
            }
            PortAddrSortUnit unit = new PortAddrSortUnit();
            unit.port = p;
            unit.address = addr;
            result.add(unit);
        }
        Collections.sort(result);
        return result;
    }

    private static class PortAddrSortUnit implements Comparable<PortAddrSortUnit> {
        public Port port;
        public CidrAddress address;
        private CidrAddressComparator comparator = new CidrAddressComparator();

        @Override
        public int compareTo(PortAddrSortUnit o) {
            return this.comparator.compare(address, o.address);
        }
    }

    public static class CidrAddressComparator implements Comparator<CidrAddress> {
        @Override
        public int compare(CidrAddress c1, CidrAddress c2) {
            byte[] addr1 = c1.getAddress().getAddress();
            byte[] addr2 = c2.getAddress().getAddress();
            if (addr1.length < addr2.length) {
                return -1;
            } else if (addr1.length > addr2.length) {
                return 1;
            }
            for (int i = 0; i < addr1.length; i++) {
                byte b1 = addr1[i];
                byte b2 = addr2[i];
                if (b1 != b2) {
                    return b1 - b2;
                }
            }
            return 0;
        }
    }

    private void showInventory() {
        print("Slot/Module information");
        Slot[] slots = device.getSlots();
        showSlotModule(0, slots);
        print("");
    }

    private void showSlotModule(int level, Slot[] slots) {
        level++;
        Arrays.sort(slots, new Comparator<Slot>() {
            public int compare(Slot o1, Slot o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o2 == null) {
                    return 1;
                } else if (o1 == null) {
                    return -1;
                }
                return o1.getSlotIndex() - o2.getSlotIndex();
            }
        });
        for (Slot slot : slots) {
            String module = null;
            if (slot.getModule() == null) {
                module = "not present";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append(slot.getSlotId());
                sb.append(":");
                sb.append(slot.getModule().getModelTypeName());
                sb.append(" (Rev:");
                sb.append(slot.getModule().getHardwareRevision());
                sb.append(")] [S/N:");
                sb.append(slot.getModule().getSerialNumber());
                sb.append("] \t#");
                sb.append(slot.getModule().getSystemDescription());
                sb.append("\r\n");
                int count = 0;
                StringBuilder sb2 = new StringBuilder();
                for (PhysicalPort ph : slot.getModule().getPhysicalPorts()) {
                    if (count % 5 == 0) {
                        if (count == 0) {
                            sb.append(sb2.toString());
                        } else {
                            sb2.append("\r\n");
                            sb.append(indent(level + 1, sb2.toString()));
                        }
                        sb2 = new StringBuilder();
                    } else {
                        sb2.append("\t");
                    }
                    count++;
                    sb2.append(ph.getIfName());
                }
                if (sb2.length() > 0) {
                    sb.append(indent(level + 1, sb2.toString()));
                }
                module = sb.toString();
            }
            print(indent(level, slot.getSlotIndex() + ": " + module + ""));

            if (slot.getModule() != null && slot.getModule().getSlots() != null) {
                showSlotModule(level, slot.getModule().getSlots());
            }
        }
    }

    private void showVlans() {
        if (!VlanDevice.class.isInstance(device)) {
            return;
        }
        print("VLAN information");
        VlanIf[] vlans = ((VlanDevice) device).getVlanIfs();
        Arrays.sort(vlans, new Comparator<VlanIf>() {
            public int compare(VlanIf o1, VlanIf o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return -1;
                } else if (o2 == null) {
                    return 1;
                }
                if (o1.getEoeId() == null && o2.getEoeId() == null) {
                    return o1.getVlanId() - o2.getVlanId();
                } else if (o1.getEoeId() == null) {
                    return -1;
                } else if (o2.getEoeId() == null) {
                    return 1;
                }
                if (o1.getEoeId().intValue() == o2.getEoeId().intValue()) {
                    return o1.getVlanId() - o2.getVlanId();
                } else {
                    return o1.getEoeId().intValue() - o2.getEoeId().intValue();
                }
            }
        });
        for (VlanIf vlan : vlans) {
            showVlan(vlan);
        }
        print("");
    }

    private void showVlan(VlanIf vlan) {
        String vlanName = (vlan.getVlanName() == null ? "-" : vlan.getVlanName());
        String prefix = "";
        if (vlan.getDevice() instanceof VlanDevice && ((VlanDevice) vlan.getDevice()).isEoeEnable()) {
            prefix = String.valueOf(vlan.getEoeId()) + ".";
        }
        print("VLAN: " + prefix + vlan.getVlanId() + " (" + vlanName + ")");
        if (RouterVlanIf.class.isInstance(vlan)) {
            RouterVlanIf vif = (RouterVlanIf) vlan;
            print("--type: router vlan-if. parent=" + vif.getRouterPort().getIfName());
        } else {
            print("--type: switch vlan-if.");
        }
        String ifIndex = "";
        try {
            int _ifIndex = vlan.getIfIndex();
            ifIndex = " (ifIndex: " + String.valueOf(_ifIndex) + ")";
        } catch (Exception e) {
        }
        print("--ifName: " + vlan.getIfName() + ifIndex);
        print("--bandwidth: " + vlan.getBandwidth());
        LogicalEthernetPort[] taggedPorts = vlan.getTaggedPorts();
        print("--tagged: ");
        showBoundPortList(taggedPorts);
        LogicalEthernetPort[] untaggedPorts = vlan.getUntaggedPorts();
        print("--untagged: ");
        showBoundPortList(untaggedPorts);
        print("");
    }

    private void showBoundPortList(LogicalEthernetPort[] ports) {
        for (LogicalEthernetPort port : ports) {
            String logicalName =
                    getAggregatorName(port) != null ? getAggregatorName(port) : port.getIfName();
            StringBuilder memberNames = new StringBuilder();
            EthernetPort[] members = port.getPhysicalPorts();
            for (EthernetPort member : members) {
                memberNames.append(member.getIfName()).append(",");
            }
            print("\t" + logicalName + " (" + memberNames.toString() + ")");
        }
    }

    private String getAggregatorName(LogicalEthernetPort logical) {
        if (logical instanceof EthernetPortsAggregator) {
            return "LAG:" + ((EthernetPortsAggregator) logical).getAggregationName();
        }
        return null;
    }

    private void showLogicalPorts() {
        LogicalPort[] ports = device.getLogicalPorts();
        print("LogicalPort -- ");
        for (LogicalPort port : ports) {
            if (port instanceof VlanIf) {
                continue;
            } else if (port instanceof DefaultLogicalEthernetPort) {
                continue;
            } else if (port instanceof EthernetPortsAggregator) {
                continue;
            } else if (port instanceof ApresiaMmrpRing) {
                continue;
            }
            showLogicalPort(port);
        }
        print("");
    }

    private void showLogicalPort(LogicalPort port) {
        String associate = port.isAssociatedPort() ? " associate=" + port.getAssociatePort().getIfName() : "";
        print(port.getIfName() + " -- TYPE: "
                + port.getClass().getName() + " " + associate);
        if (port instanceof PseudoWirePortImpl) {
            showPseudoWire((PseudoWirePort) port);
        } else if (port instanceof BgpVpnPseudoWirePortImpl) {
            showBgpPseudoWire((BgpVpnPseudoWirePortImpl) port);
        } else if (port instanceof NodePipe<?>) {
            showPipe((NodePipe<?>) port);
        } else if (port instanceof VplsInstance) {
            showVpls((VplsInstance) port);
        } else if (port instanceof VrfInstance) {
            showVrf((VrfInstance) port);
        } else if (port instanceof AtmVp) {
            showAtmVp((AtmVp) port);
        } else if (port instanceof AtmPvc) {
            showAtmPvc((AtmPvc) port);
        } else if (port instanceof MplsTunnel) {
            showLsp((MplsTunnel) port);
        } else if (port instanceof LabelSwitchedPathEndPoint) {
            showLspPath((LabelSwitchedPathEndPoint) port);
        } else if (port instanceof Channel) {
            showChannel((Channel) port);
        }
    }

    private void showPseudoWire(PseudoWirePort pw) {
        print("\t" + pw.getPseudoWireID() + "->" + pw.getPeerIpAddress().getHostAddress() + ":" + pw.getPeerPwId());
        print("\tID=" + pw.getPseudoWireID() + ", Name=" + pw.getPwName() + ", Type=" + pw.getType());
        print("\toperStatus=" + pw.getPseudoWireOperStatus() + ", Bandwidth: " + pw.getBandwidth() + ", control-word: " + pw.hasControlWord());
        StringBuilder sb = new StringBuilder();
        sb.append("\tSDP ID: ").append(pw.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_SDP_ID));
        sb.append(", Service ID: ").append(pw.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_SERVICE_ID));
        sb.append(", Tunnel ID: ").append(pw.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_TUNNEL_ID));
        print(sb.toString());
        MplsTunnel lsp = pw.getTransmitLsp();
        print("\tLSP: " + (lsp == null ? "-" : lsp.getIfName()));
        Port ac = pw.getAttachedCircuitPort();
        String operStatus = (ac != null ? ac.getOperationalStatus() : "");
        print("\tAC: " + (ac != null ? ac.getFullyQualifiedName() : "-") + " (" + operStatus + ")");
        print("");
    }

    private void showBgpPseudoWire(BgpVpnPseudoWirePortImpl pw) {
        String peerHost = (pw.getPeerIpAddress() == null ? "Unknown" : pw.getPeerIpAddress().getHostAddress());
        print("\tName=" + pw.getPwName() + ", Type=" + pw.getType());
        print("\toperStatus=" + pw.getPseudoWireOperStatus() + ", peer=" + peerHost + ":" + pw.getPeerPwId());
        print("\trd=" + pw.getRouteDistinguisher() + ", rt=" + pw.getRouteTarget());
        print("\tBandwidth: " + pw.getBandwidth());
        StringBuilder sb = new StringBuilder();
        sb.append("\tSDP ID: ").append(pw.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_SDP_ID));
        sb.append(", Service ID: ").append(pw.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_SERVICE_ID));
        sb.append(", Tunnel ID: ").append(pw.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_TUNNEL_ID));
        print(sb.toString());
        MplsTunnel lsp = pw.getTransmitLsp();
        print("\tLSP: " + (lsp == null ? "-" : lsp.getIfName()));
        Port ac = pw.getAttachedCircuitPort();
        String operStatus = (ac != null ? ac.getOperationalStatus() : "");
        print("\tAC: " + (ac != null ? ac.getFullyQualifiedName() : "-") + " (" + operStatus + ")");
        print("");
    }

    private void showPipe(NodePipe<?> pipe) {
        print("\t" + pipe.getIfName() + ", Bandwidth: " + pipe.getBandwidth());
        print("\tAC1: " + getIfName(pipe.getAttachmentCircuit1()));
        print("\tAC2: " + getIfName(pipe.getAttachmentCircuit2()));
        print("");
    }

    private void showLsp(MplsTunnel lsp) {
        print("\tifName:" + lsp.getIfName() + " [ifDescr:" + lsp.getIfDescr() + "]");
        LabelSwitchedPathEndPoint active = lsp.getActiveHops();
        for (LabelSwitchedPathEndPoint path : lsp.getMemberLsps().values()) {
            String prefix = "";
            if (active != null && active == path) {
                prefix = "*";
            }
            print("\t-" + prefix + " " + path.getLspName() + " [" + lsp.getIfDescr() + "]");
        }
        print("\tBandwidth: " + lsp.getBandwidth());
        StringBuilder sb = new StringBuilder();
        sb.append("\tSDP ID: ").append(lsp.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_SDP_ID));
        sb.append(", Service ID: ").append(lsp.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_SERVICE_ID));
        sb.append(", Tunnel ID: ").append(lsp.gainConfigurationExtInfo().get(AlcatelExtInfoNames.CONFIG_LSP_TUNNEL_ID));
        sb.append(", term: ").append(lsp.gainConfigurationExtInfo().get(JuniperJunosExtInfoNames.LSP_TERM_NUMBER));
        print(sb.toString());
        print("");
    }

    private void showLspPath(LabelSwitchedPathEndPoint path) {
        print("\t" + path.getLspName() + " [" + path.getIfDescr() + "]");
        int i = 1;
        for (String hop : path.getLspHops()) {
            print("\t- " + i + ": " + hop);
            i++;
        }
        print("\tBandwidth: " + path.getBandwidth());
        print("");
    }

    private void showVpls(VplsInstance vpls) {
        print("\t" + vpls.getIfName() + " descr:[" + vpls.getIfDescr() + "]");
        print("\tvpls-id: " + vpls.getVplsID());
        print("\toperStatus: " + vpls.getOperationalStatus());
        for (Port port : vpls.getAttachmentPorts()) {
            print("\t-" + port.getIfName() + " [" + port.getIfDescr() + "]");
        }
        print("");
    }

    private void showVrf(VrfInstance vrf) {
        print("\t" + vrf.getIfName() + " descr:[" + vrf.getIfDescr() + "]");
        print("\tvrf-id: " + vrf.getVrfID());
        print("\toperStatus: " + vrf.getOperationalStatus());
        for (Port port : vrf.getAttachmentPorts()) {
            print("\t-" + port.getIfName() + " [" + port.getIfDescr() + "]");
        }
        print("");
    }

    private void showAtmVp(AtmVp vp) {
        print("\tifName=" + vp.getIfName());
        print("\tparent: " + vp.getPhysicalPort().getIfName());
        print("\tvpi=" + vp.getVpi());
        print("\toperStatus=" + vp.getOperationalStatus() + ", Bandwidth: " + vp.getBandwidth());
        PromiscuousModeRenderer renderer = new PromiscuousModeRenderer(vp);
        print("\tpromiscuous=" + renderer.get());
        print("");
    }

    private void showAtmPvc(AtmPvc pvc) {
        print("\tifName=" + pvc.getIfName());
        print("\tparent: " + pvc.getVp().getPhysicalPort().getIfName());
        print("\tvpi=" + pvc.getVp().getVpi() + ", vci=" + pvc.getVci());
        print("\toperStatus=" + pvc.getOperationalStatus() + ", Bandwidth: " + pvc.getBandwidth());
        print("");
    }

    private void showChannel(Channel ch) {
        print("\tifName=" + ch.getIfName());
        print("\ttimeslot=" + ch.getTimeslotRange() + ", channel-group=" + ch.getChannelGroupId());
        print("\toperStatus=" + ch.getOperationalStatus() + ", Bandwidth: " + ch.getBandwidth());
        print("");
    }

    private void showEthernetPortsAggregator() {
        if (!VlanDevice.class.isInstance(this.device)) {
            return;
        }
        print("Link Aggregation Group -- ");
        EthernetPortsAggregator[] lags = ((VlanDevice) device).getEthernetPortsAggregators();
        for (int i = 0; i < lags.length; i++) {
            Integer ifIndex = null;
            try {
                ifIndex = lags[i].getIfIndex();
            } catch (Exception e) {
            }
            Integer altIfIndex = lags[i].getAlternativeIfIndex();
            print("\tLAG: Name=" + lags[i].getAggregationName()
                    + "(ifName=" + lags[i].getIfName()
                    + "), (ifIndex=" + ifIndex
                    + (altIfIndex == null ? "" : ";" + lags[i].getAlternativeIfIndex())
                    + "), ID=" + lags[i].getAggregationGroupId());
            print("\toperStatus=" + lags[i].getOperationalStatus());
            String usage = (lags[i].getVlanPortUsage() == null ? "" : lags[i].getVlanPortUsage().name());
            print("\tVlanPortUsage: " + usage);
            PhysicalPort[] members = lags[i].getPhysicalPorts();
            print("\tMemberPort -- total " + members.length);
            for (int j = 0; j < members.length; j++) {
                print("\t\t[" + j + "] " + members[j].getIfName());
            }
            print("");
        }
    }

    private void showEthernetProtectionPorts() {
        print("Ethernet Protection Ports -- ");
        EthernetProtectionPort[] epses = device.selectPorts(EthernetProtectionPort.class);
        for (int i = 0; i < epses.length; i++) {
            print("\tEPS: ifName=" + epses[i].getIfName());
            print("\toperStatus=" + epses[i].getOperationalStatus());
            EthernetPort working = epses[i].getWorkingPort();
            PhysicalPort[] members = epses[i].getPhysicalPorts();
            print("\tMemberPort -- total " + members.length);
            for (int j = 0; j < members.length; j++) {
                String prefix = "";
                if (working == members[j]) {
                    prefix = "*";
                }
                print("\t\t[" + j + "] " + prefix + members[j].getIfName());
            }
            print("");
        }
    }

    private void showPorts() {
        print("");
        print("PhysicalPort -- ");

        PhysicalPort[] ports = device.getPhysicalPorts();
        Arrays.sort(ports, new PortIfnameComparator());
        for (PhysicalPort port : ports) {
            showPort(port);
        }
        print("");
    }

    private void showPort(PhysicalPort port) {
        StringBuffer result = new StringBuffer();
        result.append(port.getIfName());
        if (port.getRawIfIndex() == null) {
            result.append(" (no ifIndex) ");
        } else {
            result.append(" (ifIndex:").append(port.getIfIndex()).append(") ");
        }
        if (port.getRawPortIndex() == null) {
            result.append(" index:- ");
        } else {
            append(result, "index", port.getPortIndex());
        }
        append(result, "ifDescr", port.getIfDescr());
        append(result, "name", port.getPortName());
        append(result, "type", port.getPortTypeName());
        result.append("status: ").append(port.getAdminStatus()).append("/").append(port.getOperationalStatus()).append(" ");
        result.append("\r\n\t");
        if (port instanceof EthernetPort) {
            EthernetPort ether = (EthernetPort) port;
            PortSpeedValue.Admin admin = ether.getPortAdministrativeSpeed();
            String adminSpeed = null;
            if (admin == null) {
                adminSpeed = "";
            } else if (admin.isAuto()) {
                adminSpeed = "auto";
            } else {
                adminSpeed = admin.getValueAsMega().toString();
            }
            result.append("speed: ")
                    .append(adminSpeed)
                    .append("/")
                    .append(ether.getPortOperationalSpeed() == null ? 0L
                            : ether.getPortOperationalSpeed().getValueAsMega()).append(" ");
            EthernetPort.Duplex duplex = ether.getDuplex();
            String duplexValue = null;
            if (duplex == null) {
                duplexValue = "";
            } else if (duplex.isAuto()) {
                duplexValue = "auto";
            } else {
                duplexValue = duplex.getValue().getId();
            }
            append(result, "Duplex", duplexValue);
            append(result, "AutoNego", (ether.getAutoNego() == null ? ""
                    : ether.getAutoNego().getValue().getId()));
            result.append("\r\n\t");
            if (VlanDevice.class.isInstance(port.getDevice())) {
                LogicalEthernetPort le = ((VlanDevice) port.getDevice()).getLogicalEthernetPort(ether);
                if (le == null) {
                    result.append("no LogicalEthernet ");
                } else {
                    String swPortMode = (String) le.gainConfigurationExtInfo().get(FlashWaveExtInfoNames.CONFIG_ETH_PORT_MODE);
                    append(result, "switchPortMode", swPortMode);
                    String usage = (le.getVlanPortUsage() == null ? "" : le.getVlanPortUsage().name());
                    append(result, "VlanPortUsage", usage);
                    append(result, "LogicalEthernet", le.getClass().getSimpleName());
                }
            }
            result.append("\r\n\t");
        } else {
            Long bandwidth = port.getBandwidth();
            String adminSpeed = null;
            if (bandwidth == null) {
                adminSpeed = "";
            } else {
                adminSpeed = bandwidth.toString() + " bps";
            }
            append(result, "Bandwidth", adminSpeed);
        }
        append(result, "Module", (port.getModule() != null ? port.getModule()
                .getSlot().getSlotIndex()
                + ":" + port.getModule().getModelTypeName() : ""));
        result.append("\r\n\t");
        append(result, "Neighbor", (port.getNeighbor() != null ? port.getNeighbor().getFullyQualifiedName() : "-"));
        ConfigurationExtInfo ex1 = port.getConfigurationExtInfo();
        if (ex1 != null) {
            result.append("\r\n\tConfiguration-ext-info: ");
            for (Object key : ex1.getKeys()) {
                Object value = ex1.get(key);
                result.append(key).append("=").append(value).append(";");
            }
        }
        NonConfigurationExtInfo ex2 = port.getNonConfigurationExtInfo();
        if (ex2 != null) {
            result.append("\r\n\tNon-Configuration-ext-info: ");
            for (Object key : ex2.getKeys()) {
                Object value = ex2.get(key);
                result.append(key).append("=").append(value).append(";");
            }
        }
        print(result.toString());
        print("");
    }

    private void showVports() {
        if (device.getModelTypeName() == null
                || !device.getModelTypeName().startsWith("FlashWave")) {
            return;
        }
        print("");
        print("Vport -- ");
        LogicalEthernetPort[] ports = ((EthernetSwitch) device)
                .getLogicalEthernetPorts();
        for (LogicalEthernetPort port : ports) {
            showVport(port);
        }
    }

    private void showVport(LogicalEthernetPort logical) {
        String ifname;
        String ifindex;
        if (logical instanceof EthernetProtectionPort) {
            ifname = logical.getIfName();
            ifindex = "EPS:" + logical.getIfIndex();
        } else if (logical instanceof EthernetPortsAggregator) {
            ifname = ((EthernetPortsAggregator) logical).getAggregationName();
            ifindex = "LAG:"
                    + ((EthernetPortsAggregator) logical).getAggregationGroupId();
        } else if (logical instanceof TagChanger) {
            return;
        } else if (logical instanceof PseudoWirePortImpl) {
            return;
        } else {
            ifname = logical.getPhysicalPorts()[0].getIfName();
            ifindex = "ifindex:" + logical.getPhysicalPorts()[0].getIfIndex();
        }
        TagChanger[] tagChangers = logical.getTagChangers();
        for (TagChanger tagChanger : tagChangers) {
            print("\t[" + ifname + " (" + ifindex + ")] - "
                    + tagChangerToString(tagChanger));
            print("\toperStatus=" + tagChanger.getOperationalStatus());
            for (Map.Entry<Integer, Integer> entry : tagChanger.getSecondaryMap().entrySet()) {
                Integer _inner = entry.getKey();
                Integer _secondary = entry.getValue();
                print("\t\tsecondary-tag-changer device[" + _inner + "]<=>["
                        + tagChanger.getOuterVlanId() + "." + _secondary + "]");
            }
        }
        print("");
    }

    private String tagChangerToString(TagChanger changer) {
        return "TagChanger id=" + changer.getTagChangerId() + "; inner="
                + changer.getInnerVlanId() + "; outer="
                + changer.getOuterVlanId();
    }

    private void showVps() {
        if (device instanceof EAConverter) {
            print("");
            print("ATM VP -- ");
            System.err.println(((EAConverter) device).getVps().length);
            for (AtmVp vp : ((EAConverter) device).getVps()) {
                String s = "VPI=" + vp.getVpi() + ", ifName=[" + vp.getIfName() + "], pcr=[" + vp.getPcr() + "]";
                print(s);
            }
        }
    }

    private void showPvcs() {
        if (device instanceof EAConverter) {
            print("");
            print("ATM PVC -- ");
            for (AtmPvc pvc : ((EAConverter) device).getPvcs()) {
                String s = "ifName=[" + pvc.getIfName() + "], operStatus=[" + pvc.getOperationalStatus() + "]\r\n";
                s = s + "\tpcr=[" + pvc.getPcr() + "], mcr=[" + pvc.getMcr() + "]\r\n";
                AtmVlanBridge bridge = ((EAConverter) device).getAtmVlanBridge(pvc);
                if (bridge != null) {
                    s = s + "\tBridgePortNumber=[" + bridge.getBridgePortNumber() + "]";
                    if (bridge.getUntaggedVlanIf() != null) {
                        s = s + " Untagged VLAN ID=[" + bridge.getUntaggedVlanIf().getVlanId() + "]";
                    }
                } else {
                    s = s + "\tNo bridge.";
                }
                print(s);
            }
        }
    }

    private void showMmrpVdr() {
        if (!(device instanceof GenericEthernetSwitch)) {
            return;
        }
        GenericEthernetSwitch sw = (GenericEthernetSwitch) this.device;
        for (LogicalPort port : sw.getLogicalPorts()) {
            if (port instanceof ApresiaMmrpRing) {
                ApresiaMmrpRing mmrp = (ApresiaMmrpRing) port;
                print("mmrp: ring id=" + mmrp.getMmrpRingId() + " (" + mmrp.getDevice().getDeviceName() + ")");
                print("\tname: " + mmrp.getRingName());
                if (mmrp.getMasterPort() != null) {
                    print("\tmaster: " + mmrp.getMasterPort().getFullyQualifiedName());
                }
                if (mmrp.getSlavePort() != null) {
                    print("\tslave: " + mmrp.getSlavePort().getFullyQualifiedName());
                }
                for (Port aware : mmrp.getAwarePorts()) {
                    print("\taware: " + aware.getFullyQualifiedName());
                }
                print("\tstatus: " + mmrp.getAdminStatus());
                StringBuilder sb = new StringBuilder();
                for (VlanIf member : mmrp.getMemberVlanIfs()) {
                    sb.append(member.getVlanName()).append(", ");
                }
                print("\tmember vlan:" + sb.toString());
                print("");
            }
        }
        for (LogicalPort port : sw.getLogicalPorts()) {
            if (port instanceof ApresiaVdr) {
                ApresiaVdr vdr = (ApresiaVdr) port;
                print("vdr: name=" + vdr.getIfName());
                if (vdr.getUplink1() != null && vdr.getUplink1().getUplinkLogicalEthernetPort() != null) {
                    print("\tUplink1: " + vdr.getUplink1().getUplinkLogicalEthernetPort().getIfName());
                }
                if (vdr.getUplink2() != null && vdr.getUplink2().getUplinkLogicalEthernetPort() != null) {
                    print("\tUplink2: " + vdr.getUplink2().getUplinkLogicalEthernetPort().getIfName());
                }
                print("\tstatus: " + vdr.getAdminStatus());
                print("");
            }
        }
    }

    private void showVMware() {
        if (!(this.device instanceof VMwareServer)) {
            return;
        }
        VMwareServer server = (VMwareServer) this.device;
        print("VMware: " + server.getDeviceName());
        int i = 0;
        for (Device virtualServer : server.getVirtualHosts()) {
            i++;
            print("\tGuest[" + i + "]: " + virtualServer.getDeviceName());
        }
        for (EthernetSwitch vSwitch : server.getVSwitches()) {
            i++;
            print("\tvSwitch[" + i + "]: " + vSwitch.getDeviceName());
        }
        print("");
        print("VM *******************************");
        print("");
        for (Device virtualServer : server.getVirtualHosts()) {
            SimpleSwitchView v = new SimpleSwitchView(virtualServer);
            v.view();
            print("********");
            print("");
        }
        print("");
        print("VSwitch *******************************");
        print("");
        for (EthernetSwitch vSwitch : server.getVSwitches()) {
            SimpleSwitchView v = new SimpleSwitchView(vSwitch);
            v.view();
            print("********");
            print("");
        }
    }

    private void print(String msg) {
        System.out.println(msg);
    }

    private void append(StringBuffer buffer, String prefix, int value) {
        append(buffer, prefix, Integer.toString(value));
    }

    @SuppressWarnings("unused")
    private void append(StringBuffer buffer, String prefix, Long value) {
        append(buffer, prefix, value.toString());
    }

    private void append(StringBuffer buffer, String prefix, String value) {
        buffer.append(prefix);
        buffer.append(": ");
        buffer.append(value);
        buffer.append(" ");
    }

    private String indent(int level, String msg) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("\t");
        }
        sb.append(msg);
        return sb.toString();
    }

    private String getIfName(Port port) {
        if (port == null) {
            return null;
        }
        return port.getIfName();
    }
}