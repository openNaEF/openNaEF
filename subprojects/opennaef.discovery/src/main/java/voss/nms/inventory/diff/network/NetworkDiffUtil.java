package voss.nms.inventory.diff.network;

import naef.dto.*;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.serial.TdmSerialIfDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentGatewayIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.ChangeUnit;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.IpSubnetAddressUtil;
import voss.core.server.util.NodeUtil;
import voss.discovery.agent.juniper.JuniperJunosExtInfoNames;
import voss.model.*;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.constants.PortMode;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.diff.*;
import voss.nms.inventory.diff.network.analyzer.*;

import java.io.IOException;
import java.util.*;

public class NetworkDiffUtil {
    private static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public static DiffPolicy getPolicy() {
        try {
            DiffConfiguration config = DiffConfiguration.getInstance();
            return config.getDiffPolicy();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static boolean isDiffTarget(NodeDto node) {
        if (node == null) {
            return false;
        }
        String value = DtoUtil.getStringOrNull(node, ATTR.DIFF_TARGET);
        if (value == null) {
            return true;
        }
        return Boolean.parseBoolean(value);
    }

    public static DiffUnitEntityType getEntityType(String id, NaefDto dto, Renderer renderer) {
        if (dto != null) {
            if (dto instanceof PortDto) {
                if (((PortDto) dto).isAlias()) {
                    return DiffUnitEntityType.ALIAS_PORT;
                }
            }
            if (dto instanceof NodeDto) {
                assertClass(DeviceRenderer.class, renderer);
                NodeDto node = (NodeDto) dto;
                if (NodeUtil.isVirtualNode(node)) {
                    return DiffUnitEntityType.VIRTUAL_NODE;
                } else {
                    return DiffUnitEntityType.NODE;
                }
            } else if (dto instanceof ChassisDto) {
                assertClass(ChassisRenderer.class, renderer);
                return DiffUnitEntityType.CHASSIS;
            } else if (dto instanceof SlotDto) {
                assertClass(SlotRenderer.class, renderer);
                return DiffUnitEntityType.SLOT;
            } else if (dto instanceof ModuleDto) {
                assertClass(ModuleRenderer.class, renderer);
                return DiffUnitEntityType.MODULE;
            } else if (dto instanceof EthLagIfDto) {
                if (NodeUtil.isEPS((EthLagIfDto) dto)) {
                    return DiffUnitEntityType.EPS;
                }
                return DiffUnitEntityType.LAG;
            } else if (dto instanceof AtmApsIfDto) {
                return DiffUnitEntityType.ATM_APS;
            } else if (dto instanceof PosApsIfDto) {
                return DiffUnitEntityType.POS_APS;
            } else if (dto instanceof EthPortDto) {
                EthPortDto eth = (EthPortDto) dto;
                if (NodeUtil.isVirtualNode(eth.getNode())) {
                    return DiffUnitEntityType.VM_NIC;
                } else {
                    assertClass(AbstractHardPortRenderer.class, renderer);
                    return DiffUnitEntityType.PHYSICAL_PORT;
                }
            } else if (dto instanceof HardPortDto) {
                assertClass(AbstractHardPortRenderer.class, renderer);
                return DiffUnitEntityType.PHYSICAL_PORT;
            } else if (dto instanceof IpIfDto && NodeUtil.isLoopback((IpIfDto) dto)) {
                return DiffUnitEntityType.LOOPBACK;
            } else if (dto instanceof VlanIfDto) {
                if (id.endsWith(VlanIfBindingRenderer.SUFFIX)) {
                    return DiffUnitEntityType.VLAN_IF_BINDING;
                } else {
                    return DiffUnitEntityType.VLAN_IF;
                }
            } else if (dto instanceof VlanSegmentGatewayIfDto) {
                return DiffUnitEntityType.TAG_CHANGER;
            } else if (dto instanceof AtmPvpIfDto) {
                return DiffUnitEntityType.ATM_VP;
            } else if (dto instanceof AtmPvcIfDto) {
                return DiffUnitEntityType.ATM_PVC;
            } else if (dto instanceof TdmSerialIfDto) {
                return DiffUnitEntityType.CHANNEL;
            }
        } else if (renderer != null) {
            if (renderer instanceof DeviceRenderer) {
                DeviceRenderer _renderer = (DeviceRenderer) renderer;
                if (_renderer.isVirtual()) {
                    return DiffUnitEntityType.VIRTUAL_NODE;
                } else {
                    return DiffUnitEntityType.NODE;
                }
            } else if (renderer instanceof ChassisRenderer) {
                return DiffUnitEntityType.CHASSIS;
            } else if (renderer instanceof SlotRenderer) {
                return DiffUnitEntityType.SLOT;
            } else if (renderer instanceof ModuleRenderer) {
                return DiffUnitEntityType.MODULE;
            } else if (renderer instanceof LoopbackPortRenderer) {
                return DiffUnitEntityType.LOOPBACK;
            } else if (renderer instanceof AliasPortRenderer) {
                return DiffUnitEntityType.ALIAS_PORT;
            } else if (renderer instanceof VMEthernetPortRenderer) {
                return DiffUnitEntityType.VM_NIC;
            } else if (renderer instanceof EthernetLAGRenderer) {
                return DiffUnitEntityType.LAG;
            } else if (renderer instanceof EthernetEPSRenderer) {
                return DiffUnitEntityType.EPS;
            } else if (renderer instanceof ATMAPSRenderer) {
                return DiffUnitEntityType.ATM_APS;
            } else if (renderer instanceof POSAPSRenderer) {
                return DiffUnitEntityType.POS_APS;
            } else if (renderer instanceof AbstractHardPortRenderer<?>) {
                return DiffUnitEntityType.PHYSICAL_PORT;
            } else if (renderer instanceof VlanIfRenderer) {
                return DiffUnitEntityType.VLAN_IF;
            } else if (renderer instanceof VlanIfBindingRenderer) {
                return DiffUnitEntityType.VLAN_IF_BINDING;
            } else if (renderer instanceof AtmVpRenderer) {
                return DiffUnitEntityType.ATM_VP;
            } else if (renderer instanceof AtmPvcIfRenderer) {
                return DiffUnitEntityType.ATM_PVC;
            } else if (renderer instanceof TdmSerialIfRenderer) {
                return DiffUnitEntityType.CHANNEL;
            } else if (renderer instanceof TagChangerRenderer) {
                return DiffUnitEntityType.TAG_CHANGER;
            }
        }
        throw new IllegalArgumentException("unsupported combination: "
                + (dto == null ? "no-dto" : dto.getAbsoluteName())
                + ", "
                + (renderer == null ? "no-renderer" : renderer.getClass().getName()));
    }

    private static void assertClass(Class<?> expected, Renderer actual) {
        if (actual == null) {
            return;
        }
        if (!expected.isAssignableFrom(actual.getClass())) {
            throw new IllegalArgumentException("illegal class combination: " +
                    "expected=" + expected.getClass().getName() +
                    ", actual=" + actual.getClass().getName());
        }
    }

    public static String getAbsoluteName(NodeElementDto dto, Renderer renderer) {
        if (dto != null) {
            return "dto:" + getSimpleAbsoluteName(dto);
        } else if (renderer != null) {
            return "renderer:" + renderer.getAbsoluteName();
        }
        return null;
    }

    public static String getSimpleAbsoluteName(NodeElementDto dto) {
        List<String> arr = new ArrayList<String>();
        NodeElementDto current = dto;
        while (current != null) {
            arr.add(current.getName());
            current = current.getOwner();
        }
        Collections.reverse(arr);
        StringBuilder sb = new StringBuilder();
        for (String element : arr) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(element);
        }
        return sb.toString();
    }

    public static String getLspName(MplsTunnel lsp) {
        if (lsp == null) {
            throw new IllegalArgumentException();
        }
        return lsp.getDevice().getDeviceName() + ":" + lsp.getIfName();
    }

    public static String getPathAbsoluteName(LabelSwitchedPathEndPoint path) {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        return path.getDevice().getDeviceName() + ":" + path.getLspName();
    }


    public static DiffOperationType getOperationType(Object discovered, Object onDatabase) {
        if (discovered == null && onDatabase == null) {
            return null;
        } else if (discovered == null) {
            return DiffOperationType.REMOVE;
        } else if (onDatabase == null) {
            return DiffOperationType.ADD;
        } else {
            return DiffOperationType.UPDATE;
        }
    }

    public static List<String> getWholeIDs(Map<String, ?> map1, Map<String, ?> map2) {
        Set<String> set = new HashSet<String>();
        set.addAll(map1.keySet());
        set.addAll(map2.keySet());
        List<String> list = new ArrayList<String>();
        list.addAll(set);
        return list;
    }

    public static DiffUnit createDummyDiffUnit(NodeDto node, Device device) {
        DiffUnit unit = new DiffUnit(node.getName(), DiffOperationType.INFORMATION);
        unit.setDescription("Got no information from device: " + device.getDeviceName());
        unit.setSourceSystem(DiffCategory.DISCOVERY.name());
        unit.setStatus(DiffStatus.INITIAL);
        return unit;
    }

    public static Map<String, NodePipe<?>> getNodePipes(Collection<NodePipe<?>> pipes) {
        Map<String, NodePipe<?>> results = new HashMap<String, NodePipe<?>>();
        for (NodePipe<?> pipe : pipes) {
            String id = InventoryIdCalculator.getId(pipe);
            results.put(id, pipe);
        }
        return results;
    }

    public static PolicyViolation buildPolicyViolation(String eventMatter, Port port, String msg) {
        PolicyViolation violation1 = new PolicyViolation(
                InventoryIdCalculator.getId(port),
                port.getDevice().getDeviceName(),
                port.getDevice().getIpAddress(),
                eventMatter, msg);
        return violation1;
    }

    public static void fillDiffContents(DiffUnit unit, CommandBuilder builder) {
        if (builder.getChangeUnits().size() == 0) {
            log.debug("no diff.");
            return;
        }
        for (ChangeUnit change : builder.getChangeUnits()) {
            if (change.isChanged() && change.isPublic()) {
                Diff diff = new Diff(change.getKey(), change.getPreChangedValue(), change.getChangedValue());
                try {
                    unit.addDiff(diff);
                } catch (DuplicationException e) {
                    log.error("duplicated! " + change.toString());
                }
            }
        }
    }

    public static void setVersionCheckValues(CommandBuilder builder) {
        ShellCommands cmd = builder.getCommand();
        cmd.setVersionCheck(false);
        cmd.setValueCheck(true);
        cmd.setValueCheckContents(builder.getChangeUnits());
    }

    public static List<String> getSubnetSeriesFromPathHops(IpAddressDB ipDB, LabelSwitchedPathEndPoint path) {
        List<String> hops = new ArrayList<String>();
        int count = 0;
        Set<String> knownSubnets = new HashSet<String>();
        String upstreamDeviceName = path.getDevice().getDeviceName();
        for (String hopIP : path.getLspHops()) {
            count++;
            String hopName = getAbsoluteNameByHopIp(ipDB, hopIP);
            log.debug("*[" + count + "] hopIP=" + hopIP + "(" + hopName
                    + "), current upstream device=[" + upstreamDeviceName + "]");
            IpSubnet subnet = ipDB.getLinkOn(hopIP);
            if (subnet == null) {
                log.debug("*[" + count + "] - link not found (ignore).");
                continue;
            } else if (!subnet.isP2pLink()) {
                log.debug("*[" + count + "] - not p2p link (ignore).");
                continue;
            } else if (knownSubnets.contains(subnet.getNetworkAddress())) {
                log.debug("*[" + count + "] - known hop (ignore).");
                continue;
            }
            knownSubnets.add(subnet.getNetworkAddress());
            String upstreamIP = subnet.getUpstreamIP(upstreamDeviceName, hopIP);
            if (upstreamIP == null) {
                throw new IllegalStateException("upstream-ip not found. subnet=" + subnet.toString());
            }
            String absoluteName = getAbsoluteNameByHopIp(ipDB, upstreamIP);
            log.debug("*[" + count + "] - normalized hop is [" + upstreamIP + "(" + absoluteName + ")]");
            if (absoluteName == null) {
                continue;
            } else {
                hops.add(absoluteName);
            }
            upstreamDeviceName = subnet.getDownstreamDeviceName(upstreamDeviceName);
        }
        return hops;
    }

    public static String getAbsoluteNameByHopIp(IpAddressDB ipDB, String hopIP) {
        IpAddressHolder ip = ipDB.getIpAddress(null, hopIP);
        if (ip == null) {
            return null;
        }
        return ip.getAbsoluteName();
    }

    public static CidrAddress getVpnCidrAddressOf(Port port) {
        if (port == null) {
            return null;
        } else if (!(port.getDevice() instanceof MplsVlanDevice)) {
            return null;
        }
        MplsVlanDevice device = (MplsVlanDevice) port.getDevice();
        VrfInstance vrf = device.getPortRelatedVrf(port);
        if (vrf == null) {
            return null;
        }
        CidrAddress addr = vrf.getVpnIpAddress(port);
        return addr;

    }

    public static String getVpnIpAddressOf(Port port) {
        CidrAddress addr = getVpnCidrAddressOf(port);
        if (addr != null) {
            return addr.getAddress().getHostAddress();
        }
        return null;
    }

    public static String getVpnIpMaskLengthOf(Port port) {
        CidrAddress addr = getVpnCidrAddressOf(port);
        if (addr != null) {
            return String.valueOf(addr.getSubnetMaskLength());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getLspTerm(MplsTunnel lsp) {
        if (lsp == null) {
            return new ArrayList<String>();
        }
        Object obj = lsp.gainConfigurationExtInfo().get(JuniperJunosExtInfoNames.LSP_TERM_NUMBER);
        if (obj == null) {
            return new ArrayList<String>();
        } else if (!List.class.isInstance(obj)) {
            throw new IllegalStateException("[lsp.extinfo.term] is not list!");
        }
        return (List<String>) obj;
    }

    public static PortMode getPortMode(LogicalEthernetPort le) {
        if (le == null) {
            return null;
        }
        VlanPortUsage usage = le.getVlanPortUsage();
        if (usage == null) {
            return guessPortMode(le);
        }
        switch (usage) {
            case ROUTER:
                return PortMode.IP;
            case TRUNK:
            case ACCESS:
            case TUNNEL:
            case EOE:
                return PortMode.VLAN;
        }
        return null;
    }

    public static PortMode guessPortMode(LogicalEthernetPort le) {
        if (le == null) {
            return null;
        }
        log.debug("guessing port-mode: " + le.getFullyQualifiedName());
        if (VirtualServerDevice.class.isInstance(le.getDevice())) {
            log.debug("- device type is vmware vm. port mode will be 'IP'.");
            return PortMode.IP;
        }
        if (MplsVlanDevice.class.isInstance(le.getDevice())) {
            MplsVlanDevice vd = (MplsVlanDevice) le.getDevice();
            List<VlanIf> routerVlanIfs = vd.getRouterVlanIfsOn(le);
            if (routerVlanIfs != null && routerVlanIfs.size() > 0) {
                log.debug("- router vlan-if found. port mode is 'IP'.");
                return PortMode.IP;
            }
            log.debug("- device type is mpls-vlan-device. port mode will be 'IP'.");
            return PortMode.IP;
        }
        if (le.getTaggedVlanIfs() != null && le.getTaggedVlanIfs().length > 0) {
            log.debug("- tagged vlan found. port mode is 'VLAN'.");
            return PortMode.VLAN;
        } else if (le.getUntaggedVlanIfs() != null && le.getUntaggedVlanIfs().length > 0) {
            log.debug("- untagged vlan found. port mode is 'VLAN'.");
            return PortMode.VLAN;
        }
        if (VlanDevice.class.isInstance(le.getDevice())) {
            log.debug("- device type is vlan-device. port mode will be 'VLAN'.");
            return PortMode.VLAN;
        }
        log.debug("- cannot determine port mode.");
        return null;
    }

    public static SwitchPortMode getSwitchPortMode(LogicalEthernetPort le) {
        if (le == null) {
            return null;
        }
        VlanPortUsage usage = le.getVlanPortUsage();
        if (usage == null) {
            return guessSwitchPortMode(le);
        }
        switch (usage) {
            case ACCESS:
                return SwitchPortMode.ACCESS;
            case TUNNEL:
                return SwitchPortMode.DOT1Q_TUNNEL;
            case TRUNK:
            case ROUTER:
            case EOE:
                return SwitchPortMode.TRUNK;
        }
        return null;
    }

    public static SwitchPortMode guessSwitchPortMode(LogicalEthernetPort le) {
        if (le == null) {
            return null;
        }
        log.debug("guessing switch-port-mode: " + le.getFullyQualifiedName());
        if (VirtualServerDevice.class.isInstance(le.getDevice())) {
            log.debug("- device type is vmware vm. switch port mode will be 'ACCESS'.");
            return SwitchPortMode.ACCESS;
        }
        if (MplsVlanDevice.class.isInstance(le.getDevice())) {
            MplsVlanDevice d = (MplsVlanDevice) le.getDevice();
            List<VlanIf> vifs = d.getRouterVlanIfsOn(le);
            if (vifs != null && vifs.size() > 0) {
                log.debug("- router vlan-if found. switch port mode is 'TRUNK'.");
                return SwitchPortMode.TRUNK;
            }
        }
        if (le.getTaggedVlanIfs() != null && le.getTaggedVlanIfs().length > 0) {
            log.debug("- tagged vlan found. switch port mode is 'TRUNK'.");
            return SwitchPortMode.TRUNK;
        }
        log.debug("- switch port mode is 'ACCESS'.");
        return SwitchPortMode.ACCESS;
    }

    public static IpSubnetAddressDto getDefaultRootIpSubnetAddress() throws InventoryException, ExternalServiceException {
        try {
            String addressName = DiffConfiguration.getInstance().getDiffPolicy().getDefaultRootIpSubnetAddressName();
            if (addressName == null) {
                return null;
            }
            return InventoryConnector.getInstance().getRootIpSubnetAddress(addressName);
        } catch (IOException e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static IpSubnetAddressDto getGlobalIpSubnetAddress(String ipAddress, int maskLength) throws InventoryException, ExternalServiceException {
        String name = AbsoluteNameFactory.toIpSubnetAddressName(null, ipAddress, maskLength);
        IpSubnetAddressDto root = getDefaultRootIpSubnetAddress();
        if (root == null) {
            return null;
        }
        return IpSubnetAddressUtil.findIpSubnetAddress(root, name);
    }
}