package voss.core.server.util;

import naef.dto.*;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import naef.dto.serial.TdmSerialIfDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import naef.mvo.ip.IpAddress;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO;
import tef.MVO.MvoId;
import tef.skelton.dto.EntityDto;
import voss.core.server.database.ATTR;
import voss.core.server.database.CONSTANTS;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdType;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.*;

public class NodeUtil {
    private static final Logger log = LoggerFactory.getLogger(NodeUtil.class);
    public static final String KEY_NODE = "node";
    public static final String KEY_SLOT_FQN = "slot";
    public static final String KEY_PORT_FQN = "ifName";

    public static NodeDto getNode(String nodeName) throws ExternalServiceException {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NodeDto node = conn.getNodeDto(nodeName);
            return node;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static boolean isVirtualizationHostingEnable(NodeDto node) {
        return VirtualNodeCoreUtil.isVirtualizationHostingEnable(node);
    }

    public static boolean isEndOfUse(String operStatus) {
        if (operStatus == null) {
            return false;
        } else if (operStatus.equals("廃止済")) {
            return true;
        } else if (operStatus.equals("廃止予定")) {
            return true;
        }
        return false;
    }

    public static boolean isEndOfUse(NaefDto nodeElement) {
        String operStatus = DtoUtil.getStringOrNull(nodeElement, "運用状態");
        return isEndOfUse(operStatus);
    }

    public static boolean isRetired(String operStatus) {
        if (operStatus == null) {
            return false;
        } else if (operStatus.equals("廃止済")) {
            return true;
        }
        return false;
    }

    public static boolean isRetired(NaefDto ne) {
        String operStatus = DtoUtil.getStringOrNull(ne, "運用状態");
        return isEndOfUse(operStatus);
    }

    public static List<JackDto> getWholeInterfaces(NodeDto node) {
        if (node == null) {
            return new ArrayList<JackDto>();
        }
        Set<JackDto> jacks = node.getJacks();
        if (jacks == null) {
            return new ArrayList<JackDto>();
        }
        return new ArrayList<JackDto>(jacks);
    }

    public static List<JackDto> getWholeJacks(NodeDto node, PortFilter filter) {
        if (node == null) {
            return new ArrayList<JackDto>();
        }
        Set<JackDto> jacks = node.getJacks();
        if (jacks == null) {
            return new ArrayList<JackDto>();
        }
        List<JackDto> result = new ArrayList<JackDto>();
        for (JackDto jack : jacks) {
            PortDto port = jack.getPort();
            if (port == null) {
                continue;
            }
            if (!filter.match(port)) {
                continue;
            }
            result.add(jack);
        }
        return result;
    }

    public static List<PortDto> getWholePorts(NodeDto node, PortFilter filter) {
        List<PortDto> interfaces = new ArrayList<PortDto>();
        for (PortDto port : node.getPorts()) {
            if (filter != null && filter.match(port)) {
                interfaces.add(port);
            }
        }
        return interfaces;
    }

    public static List<PortDto> getWholePorts(NodeDto node, NodeElementFilter filter) {
        List<PortDto> interfaces = new ArrayList<PortDto>();
        for (PortDto port : node.getPorts()) {
            if (filter != null && !filter.match(port)) {
                interfaces.add(port);
            }
        }
        return interfaces;
    }

    public static List<PortDto> getPorts(NodeElementDto dto) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (dto instanceof NodeDto) {
            NodeDto node = (NodeDto) dto;
            Set<PortDto> ports = node.getPorts();
            if (ports == null) {
                return new ArrayList<PortDto>();
            }
            return new ArrayList<PortDto>(ports);
        } else if (dto instanceof HardwareDto) {
            return getPorts((HardwareDto) dto);
        } else if (dto instanceof VlanIfDto) {
            VlanIfDto vlanIf = (VlanIfDto) dto;
            result.add(vlanIf);
            result.addAll(vlanIf.getTaggedVlans());
            result.addAll(vlanIf.getUntaggedVlans());
        } else if (dto instanceof VrfIfDto) {
            VrfIfDto vrfIf = (VrfIfDto) dto;
            result.add(vrfIf);
            result.addAll(vrfIf.getAttachedPorts());
        } else if (dto instanceof VplsIfDto) {
            VplsIfDto vplsIf = (VplsIfDto) dto;
            result.add(vplsIf);
            result.addAll(vplsIf.getAttachedPorts());
        } else if (dto instanceof AtmPortDto) {
            AtmPortDto atm = (AtmPortDto) dto;
            Set<MvoId> vpMvoIds = new HashSet<MvoId>();
            List<AtmPvpIfDto> vps = new ArrayList<AtmPvpIfDto>();
            List<AtmPvcIfDto> vcs = new ArrayList<AtmPvcIfDto>();
            for (PortDto p : atm.getNode().getPorts()) {
                if (p instanceof AtmPvpIfDto) {
                    AtmPvpIfDto vp = (AtmPvpIfDto) p;
                    if (DtoUtil.mvoEquals(atm, vp.getPhysicalPort())) {
                        vps.add(vp);
                        vpMvoIds.add(DtoUtil.getMvoId(vp));
                    }
                }
            }
            for (PortDto p : atm.getNode().getPorts()) {
                if (p instanceof AtmPvcIfDto) {
                    AtmPvcIfDto vc = (AtmPvcIfDto) p;
                    if (vpMvoIds.contains(DtoUtil.getMvoId(vc.getOwner()))) {
                        vcs.add(vc);
                    }
                }
            }
            result.addAll(vps);
            result.addAll(vcs);
            log.debug("atm=" + atm.getAbsoluteName() + ", vps=" + vps.size() + ", pvcs=" + vcs.size());
        } else if (dto instanceof PosPortDto) {
        } else if (dto instanceof SerialPortDto) {
            result.addAll(getPvcs((SerialPortDto) dto));
        } else {
            throw new IllegalArgumentException("unknown dto: "
                    + dto.getClass().getName()
                    + " [" + dto.getName() + "]");
        }
        return result;
    }

    public static List<PortDto> getPorts(HardwareDto hardware) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (hardware == null) {
            return result;
        }
        getPorts(result, hardware);
        return result;
    }

    private static void getPorts(List<PortDto> result, NodeElementDto hardware) {
        if (hardware instanceof ChassisDto) {
            ChassisDto chassis = (ChassisDto) hardware;
            for (NodeElementDto hardware_ : chassis.getSubElements()) {
                getPorts(result, hardware_);
            }
        } else if (hardware instanceof SlotDto) {
            SlotDto slot = (SlotDto) hardware;
            ModuleDto module = slot.getModule();
            if (module != null) {
                getPorts(result, module);
            }
        } else if (hardware instanceof ModuleDto) {
            ModuleDto module = (ModuleDto) hardware;
            for (NodeElementDto hardware_ : module.getSubElements()) {
                getPorts(result, hardware_);
            }
        } else if (hardware instanceof JackDto) {
            JackDto jack = (JackDto) hardware;
            if (!result.contains(jack.getPort())) {
                getPorts(result, jack.getPort());
            }
        } else {
            throw new IllegalArgumentException("unknown hardware: "
                    + hardware.getClass().getName() + ":"
                    + hardware.toString());
        }
    }

    private static void getPorts(List<PortDto> result, PortDto port) {
        if (port == null) {
            return;
        }
        try {
            NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
            for (NodeElementDto p : facade.getDescendants(port, false)) {
                if (!(p instanceof PortDto)) {
                    continue;
                } else {
                    result.add((PortDto) p);
                }
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private static List<PortDto> getPvcs(SerialPortDto port) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (port == null) {
            return result;
        }
        for (NodeElementDto sub : port.getSubElements()) {
            if (FrPvcIfDto.class.isInstance(sub)) {
                result.add(FrPvcIfDto.class.cast(sub));
            }
            if (AtmPvpIfDto.class.isInstance(sub)) {
                AtmPvpIfDto vp = AtmPvpIfDto.class.cast(sub);
                for (NodeElementDto pvc : vp.getSubElements()) {
                    if (AtmPvcIfDto.class.isInstance(pvc)) {
                        result.add(AtmPvcIfDto.class.cast(pvc));
                    }
                }
            }
        }
        return result;
    }

    public static void getSubNodeElements(List<NodeElementDto> result, NodeElementDto base) {
        if (base == null) {
            return;
        }
        result.add(base);
        for (NodeElementDto sub : base.getSubElements()) {
            getSubNodeElements(result, sub);
        }
    }

    public static PortDto getPortByAbsoluteName(NodeDto node, String absoluteName) {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            EntityDto dto = conn.getMvoDtoByAbsoluteName(absoluteName);
            if (PortDto.class.isInstance(dto)) {
                return (PortDto) dto;
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static PortDto getPortByIfName(NodeDto node, String ifName) {
        if (ifName == null) {
            return null;
        }
        NaefDtoFacade facade = DtoUtil.getNaefDtoFacade(node);
        try {
            Set<PortDto> ports = facade.selectNodeElements(node, PortDto.class, SearchMethod.EXACT_MATCH, DtoUtil.getIfNameAttributeName(), ifName);
            if (ports.size() == 0) {
                return null;
            }
            return ports.iterator().next();
        } catch (RemoteException e) {
            log.warn("dtofacade returns error.", e);
        }
        return null;
    }

    public static PortDto getPortByConfigName(NodeDto node, String configName) {
        if (configName == null) {
            return null;
        }
        NaefDtoFacade facade = DtoUtil.getNaefDtoFacade(node);
        try {
            Set<PortDto> ports = facade.selectNodeElements(node, PortDto.class, SearchMethod.EXACT_MATCH, ATTR.CONFIG_NAME, configName);
            if (ports.size() == 0) {
                return null;
            }
            return ports.iterator().next();
        } catch (RemoteException e) {
            log.warn("dtofacade returns error.", e);
        }
        return null;
    }

    public static PortDto getPortByConfigName(PortDto ownerPort, String configName) {
        if (configName == null) {
            return null;
        }
        for (PortDto sub : ownerPort.getParts()) {
            if (sub != null && DtoUtil.hasStringValue(sub, ATTR.CONFIG_NAME, configName)) {
                return sub;
            }
            PortDto moreSubIf = getPortByConfigName(sub, configName);
            if (moreSubIf != null) {
                return moreSubIf;
            }
        }
        for (NodeElementDto sub : ownerPort.getSubElements()) {
            if (!PortDto.class.isInstance(sub)) {
                continue;
            }
            PortDto subIf = (PortDto) sub;
            if (subIf != null && DtoUtil.hasStringValue(subIf, ATTR.CONFIG_NAME, configName)) {
                return subIf;
            }
            PortDto moreSubIf = getPortByConfigName(subIf, configName);
            if (moreSubIf != null) {
                return moreSubIf;
            }
        }
        return null;
    }

    public static PortDto getPortByName(NodeDto node, String name) {
        if (name == null) {
            return null;
        }
        for (PortDto port : node.getPorts()) {
            if (port.getName().equals(name)) {
                return port;
            }
        }
        for (JackDto jack : node.getJacks()) {
            if (jack.getName().equals(name)) {
                return jack.getPort();
            }
        }
        return null;
    }

    public static String getNodeName(NodeElementDto dto) {
        if (dto == null) {
            return null;
        }
        return dto.getNode().getName();
    }

    public static String getNodeName(NaefDto dto) {
        if (dto == null) {
            return null;
        }
        if (dto instanceof NodeElementDto) {
            return getNodeName((NodeElementDto) dto);
        }
        return dto.getClass().getSimpleName().replace("Dto", "");
    }

    public static PortDto getAnotherEndPort(NetworkDto link, PortDto here) {
        if (link == null) {
            return null;
        } else if (here == null) {
            return null;
        }
        Set<PortDto> members = link.getMemberPorts();
        if (members.size() != 2) {
            return null;
        }
        for (PortDto member : members) {
            if (DtoUtil.mvoEquals(member, here)) {
                continue;
            } else {
                return member;
            }
        }
        return null;
    }

    public static PortDto getLayer1Neighbor(PortDto port) {
        if (port == null) {
            return null;
        } else if (HardPortDto.class.isInstance(port)) {
            HardPortDto hardPort = HardPortDto.class.cast(port);
            return hardPort.getL1Neighbor();
        }
        return null;
    }

    public static PortDto getLayer2Neighbor(PortDto port) {
        if (port == null) {
            return null;
        } else if (HardPortDto.class.isInstance(port)) {
            HardPortDto hardPort = HardPortDto.class.cast(port);
            Collection<PortDto> ports = hardPort.getL2Neighbors();
            if (ports.size() == 0) {
                return null;
            } else if (ports.size() == 1) {
                return ports.iterator().next();
            } else if (ports.size() > 1) {
                log.warn("2 or more l2-neighbor found: " + ports.size() + " on [" + port.getAbsoluteName() + "]");
            }
        } else if (EthLagIfDto.class.isInstance(port)) {
            EthLagIfDto lag = EthLagIfDto.class.cast(port);
            LinkDto link = lag.getLink();
            if (link == null) {
                return null;
            }
            return getAnotherEndPort(link, lag);
        }
        return null;
    }

    public static PortDto getL2NeighborOf(PortDto port, String linkType) {
        if (linkType == null) {
            return null;
        } else if (port == null) {
            return null;
        }
        LinkDto link = getL2LinkOf(port, linkType);
        if (link == null) {
            return null;
        }
        return getAnotherEndPort(link, port);
    }

    public static LinkDto getLayer1Link(PortDto port) {
        if (port == null) {
            return null;
        } else if (HardPortDto.class.isInstance(port)) {
            return ((HardPortDto) port).getL1Link();
        }
        return null;
    }

    public static LinkDto getLayer2Link(PortDto port) {
        if (port == null) {
            return null;
        } else if (HardPortDto.class.isInstance(port)) {
            Collection<LinkDto> links = ((HardPortDto) port).getL2Links();
            if (links.size() == 1) {
                return links.iterator().next();
            } else if (links.size() > 1) {
                log.warn("2 or more l2-neighbor found: " + links.size() + " on [" + port.getAbsoluteName() + "]");
            }
        } else if (EthLagIfDto.class.isInstance(port)) {
            return ((EthLagIfDto) port).getLink();
        }
        return null;
    }

    public static List<LinkDto> getLayer2Links(PortDto port) {
        List<LinkDto> result = new ArrayList<LinkDto>();
        if (port == null) {
        } else if (HardPortDto.class.isInstance(port)) {
            Collection<LinkDto> links = ((HardPortDto) port).getL2Links();
            result.addAll(links);
        } else if (EthLagIfDto.class.isInstance(port)) {
            result.add(((EthLagIfDto) port).getLink());
        }
        return result;
    }

    public static LinkDto getL2LinkOf(PortDto port, String linkType) {
        if (port == null) {
            return null;
        } else if (HardPortDto.class.isInstance(port)) {
            HardPortDto hp = HardPortDto.class.cast(port);
            Collection<LinkDto> links = hp.getL2Links();
            for (LinkDto link : links) {
                String _linkType = LinkCoreUtil.getLinkType(link);
                if (linkType == null && _linkType == null) {
                    return link;
                } else if (linkType != null && linkType.equals(_linkType)) {
                    return link;
                }
            }
        } else if (EthLagIfDto.class.isInstance(port)) {
            EthLagIfDto lag = EthLagIfDto.class.cast(port);
            LinkDto link = lag.getLink();
            if (linkType == null) {
                return link;
            } else {
                return null;
            }
        }
        return null;
    }

    public static IpSubnetDto getLayer3Link(PortDto port) {
        if (port == null) {
            return null;
        } else if (port instanceof IpIfDto) {
            for (NetworkDto network : port.getNetworks()) {
                if (network instanceof IpSubnetDto) {
                    return (IpSubnetDto) network;
                }
            }
            return null;
        } else {
            IpIfDto ip = getIpOn(port);
            if (ip == null) {
                return null;
            } else {
                return getLayer3Link(ip);
            }
        }
    }

    public static boolean isSamePort(PortDto port1, PortDto port2) {
        if (port1 == null || port2 == null) {
            return false;
        }
        return DtoUtil.mvoEquals(port1, port2);
    }

    public static boolean isSameNode(PortDto port1, PortDto port2) {
        if (port1 == null || port2 == null) {
            return false;
        }
        return DtoUtil.mvoEquals(port1.getNode(), port2.getNode());
    }

    public static boolean isSameNode(NodeElementDto element, NodeDto node) {
        if (element == null || node == null) {
            return false;
        }
        if (element instanceof NodeDto) {
            return DtoUtil.mvoEquals(element, node);
        }
        return DtoUtil.mvoEquals(element.getNode(), node);
    }

    public static boolean isSubInterface(PortDto port) {
        if (port instanceof AtmPvcIfDto) {
            return true;
        } else if (port instanceof AtmPvpIfDto) {
            return true;
        } else if (port instanceof FrPvcIfDto) {
            return true;
        } else if (port instanceof TdmSerialIfDto) {
            return true;
        } else if (port instanceof EthPortDto) {
            Class<?> ownerClass = DtoUtil.getOwnerClass(port);
            if (ownerClass == null) {
                return false;
            }
            return PortDto.class.isAssignableFrom(ownerClass);
        } else if (port instanceof VlanIfDto) {
            Class<?> ownerClass = DtoUtil.getOwnerClass(port);
            if (ownerClass == null) {
                return false;
            } else if (PortDto.class.isAssignableFrom(ownerClass)) {
                return true;
            }
        } else if (port instanceof IpIfDto) {
            IpIfDto ip = (IpIfDto) port;
            Collection<PortDto> associated = ip.getAssociatedPorts();
            return associated != null && associated.size() > 0 && isIndependentIp((IpIfDto) port);
        }
        return false;
    }

    public static boolean isIndependentIp(IpIfDto ip) {
        if (ip == null) {
            return false;
        }
        return DtoUtil.hasStringValue(ip, ATTR.PORT_TYPE, CONSTANTS.INTERFACE_TYPE_INDEPENDENT_IP);
    }

    @SuppressWarnings({"unchecked"})
    public static <T extends PortDto> List<T> getSpecificPortOn(NodeDto node, Class<T> cls) {
        List<T> result = new ArrayList<T>();
        for (NodeElementDto subElement : node.getSubElements()) {
            if (cls.isAssignableFrom(subElement.getClass())) {
                result.add((T) subElement);
            }
        }
        return result;
    }

    public static List<PortDto> getSubInterfaces(PortDto parent) {
        Set<MvoId> known = new HashSet<MvoId>();
        List<PortDto> results = new ArrayList<PortDto>();
        getSubInterfaces(known, results, parent);
        return results;
    }

    private static void getSubInterfaces(Set<MvoId> known, List<PortDto> results, PortDto parent) {
        if (parent == null) {
            return;
        }
        for (NodeElementDto subElement : parent.getSubElements()) {
            if (!(subElement instanceof PortDto)) {
                continue;
            }
            if (known.contains(DtoUtil.getMvoId(subElement))) {
                continue;
            }
            PortDto subInterface = (PortDto) subElement;
            results.add(subInterface);
            known.add(DtoUtil.getMvoId(subInterface));
            getSubInterfaces(known, results, subInterface);
        }
    }

    public static boolean isSameOrSubElement(NodeElementDto element1, NaefDto dto) {
        if (element1 == null) {
            return false;
        } else if (!(dto instanceof NodeElementDto)) {
            return false;
        }
        NodeElementDto element2 = (NodeElementDto) dto;
        while (element2 != null) {
            if (DtoUtil.mvoEquals(element1, element2)) {
                return true;
            }
            element2 = element2.getOwner();
        }
        return false;
    }

    public static void checkDeletable(NodeElementDto element) throws InventoryException {
        List<PortDto> ports = getPorts(element);
        for (PortDto port : ports) {
            if (!(port instanceof HardPortDto)) {
                throw new InventoryException("Some hard-ports are still alive. " + NameUtil.getCaption(port));
            }
        }
    }

    public static void populateHardware(NodeElementDto dto, Set<MvoId> set) {
        if (dto instanceof NodeDto) {
            NodeDto node = (NodeDto) dto;
            for (ChassisDto chassis : node.getChassises()) {
                populateHardware(chassis, set);
            }
            for (JackDto jack : node.getJacks()) {
                populateHardware(jack, set);
            }
        } else if (!(dto instanceof HardwareDto)) {
            set.add(DtoUtil.getMvoId(dto));
        } else if (dto instanceof ChassisDto) {
            ChassisDto chassis = (ChassisDto) dto;
            for (SlotDto slot : chassis.getSlots()) {
                populateHardware(slot, set);
            }
        } else if (dto instanceof SlotDto) {
            SlotDto slot = (SlotDto) dto;
            ModuleDto module = slot.getModule();
            if (module != null) {
                populateHardware(module, set);
            }
        } else if (dto instanceof ModuleDto) {
            ModuleDto module = (ModuleDto) dto;
            for (NodeElementDto hw : module.getSubElements()) {
                if (hw instanceof HardwareDto) {
                    populateHardware(hw, set);
                }
            }
            for (JackDto jack : module.getJacks()) {
                populateHardware(jack, set);
            }
        } else if (dto instanceof JackDto) {
            set.add(DtoUtil.getMvoId(dto));
        } else {
            throw new IllegalStateException("unknown hardware: " + dto.getAbsoluteName());
        }
    }

    public static EthPortDto getBridgePort(PortDto bridgedPort) {
        if (bridgedPort == null) {
            return null;
        }
        for (NodeElementDto sub : bridgedPort.getSubElements()) {
            if (EthPortDto.class.isInstance(sub)) {
                return EthPortDto.class.cast(sub);
            }
        }
        return null;
    }

    public static PortDto getPortByOwnerAndSuffix(NodeElementDto base, String suffix)
            throws ExternalServiceException {
        if (base == null) {
            return null;
        }
        try {
            NaefDtoFacade facade = CoreConnector.getInstance().getDtoFacade();
            for (NodeElementDto ne : facade.getDescendants(base, false)) {
                if (!(ne instanceof PortDto)) {
                    continue;
                }
                String s_ = DtoUtil.getStringOrNull(ne, "suffix");
                if (s_ != null && s_.equals(suffix)) {
                    return (PortDto) ne;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static void checkDeletable(PortDto port) throws InventoryException {
        if (port == null) {
            return;
        }
        checkNetwork(port);
        if (VlanUtil.isBridgedPort(port)) {
            PortDto bridge = VlanUtil.getBridgePort(port);
            if (bridge == null) {
                return;
            }
            checkNetwork(bridge);
        }
    }

    private static void checkNetwork(PortDto port) {
        Set<NetworkDto> networks = port.getNetworks();
        if (networks != null && networks.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (NetworkDto network : networks) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(NameUtil.getCaption(network));
            }
            throw new IllegalStateException("There are resources using this port." + sb.toString());
        }
    }

    public static boolean isIfNameEditable(PortDto port) {
        if (!DtoUtil.isSupportedAttribute(port, DtoUtil.getIfNameAttributeName())) {
            return false;
        }
        boolean isSubInterface = NodeUtil.isSubInterface(port);
        boolean isSVI = port instanceof VlanIfDto && VlanUtil.isSwitchVlanIf((VlanIfDto) port);
        boolean ifNameEditable = !isSubInterface && !isSVI;
        return ifNameEditable;
    }

    public static IpIfDto getIpIfOn(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof IpIfDto) {
            return (IpIfDto) port;
        }
        if (port.getPrimaryIpIf() != null) {
            return port.getPrimaryIpIf();
        } else if (port.getSecondaryIpIf() != null) {
            return port.getSecondaryIpIf();
        }
        return null;
    }

    public static IpIfDto getIpOn(PortDto port) {
        if (port == null) {
            return null;
        }
        IpIfDto ip = port.getPrimaryIpIf();
        if (port instanceof IpIfDto) {
            if (ip == null) {
                return (IpIfDto) port;
            } else if (isIndependentIp(ip)) {
                return (IpIfDto) port;
            } else {
                return ip;
            }
        } else {
            if (ip == null) {
                return null;
            } else if (isIndependentIp(ip)) {
                return null;
            } else {
                return ip;
            }
        }
    }

    public static PortDto getAssociatedPort(PortDto port) {
        if (port instanceof IpIfDto) {
            return getAssociatedPort((IpIfDto) port);
        }
        return port;
    }

    public static PortDto getAssociatedPort(IpIfDto ip) {
        if (ip == null) {
            return null;
        }
        Collection<PortDto> ports = ip.getAssociatedPorts();
        if (ports.size() == 0) {
            return null;
        } else if (ports.size() == 1) {
            return ports.iterator().next();
        } else {
            throw new IllegalStateException("2 or more associated ports found: " + ip.getAbsoluteName());
        }
    }

    public static List<PortDto> getAssociatedPorts(IpIfDto ip) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (ip != null) {
            result.addAll(ip.getAssociatedPorts());
        }
        return result;
    }

    public static IpIfDto toIpIfDto(PortDto port) {
        if (port == null) {
            return null;
        } else if (IpIfDto.class.isInstance(port)) {
            return (IpIfDto) port;
        }
        return null;
    }

    public static Set<IpIfDto> getVpnIpIf(PortDto vpn) {
        MvoDtoSet<IpIfDto> result = new MvoDtoSet<IpIfDto>();
        for (NodeElementDto sub : vpn.getSubElements()) {
            if (!IpIfDto.class.isInstance(sub)) {
                continue;
            }
            result.add((IpIfDto) sub);
        }
        return result;
    }

    public static IpIfDto getVpnIpIfByIfName(PortDto vpn, String ifName) {
        if (vpn == null || ifName == null) {
            return null;
        }
        for (NodeElementDto sub : vpn.getSubElements()) {
            if (!IpIfDto.class.isInstance(sub)) {
                continue;
            }
            IpIfDto ipIf = (IpIfDto) sub;
            String ipIfName = DtoUtil.getIfName(ipIf);
            if (ipIfName == null) {
                continue;
            } else if (ipIfName.equals(ifName)) {
                return ipIf;
            }
        }
        return null;
    }

    public static IpIfDto getVpnIpIfByIpAddress(PortDto vpn, String ipAddress) {
        if (vpn == null || ipAddress == null) {
            return null;
        }
        for (NodeElementDto sub : vpn.getSubElements()) {
            if (!(sub instanceof IpIfDto)) {
                continue;
            }
            IpIfDto ipIf = (IpIfDto) sub;
            String address = DtoUtil.getStringOrNull(ipIf, ATTR.IP_ADDRESS);
            if (address == null) {
                continue;
            } else if (address.equals(ipAddress)) {
                return ipIf;
            }
        }
        return null;
    }

    public static NodeElementDto getNodeElementByAbsoluteName(String absoluteName) throws ExternalServiceException {
        String[] names = absoluteName.split(",");
        NodeElementDto current = null;
        for (String name : names) {
            if (current == null) {
                current = getNode(name);
                if (current == null) {
                    return null;
                } else {
                    continue;
                }
            } else {
                for (NodeElementDto ne : current.getSubElements()) {
                    if (Util.s2n(ne.getName()) == null && Util.s2n(name) == null) {
                        current = ne;
                        continue;
                    } else if (ne.getName().equals(name)) {
                        current = ne;
                        continue;
                    }
                }
                return null;
            }
        }
        return null;
    }

    public static int getDepth(NodeElementDto onDatabase) {
        if (onDatabase == null) {
            throw new IllegalStateException();
        }
        int count = 0;
        NodeElementDto current = onDatabase;
        while (!(current instanceof NodeDto)) {
            count++;
            current = current.getOwner();
        }
        return count;
    }

    public static EthLagIfDto getEthLag(EthPortDto eth) {
        if (eth == null) {
            return null;
        }
        PortDto container = eth.getContainer();
        if (EthLagIfDto.class.isInstance(container)) {
            return EthLagIfDto.class.cast(container);
        }
        return null;
    }

    public static AtmApsIfDto getAtmApsIf(AtmPortDto atm) {
        if (atm == null) {
            return null;
        }
        PortDto container = atm.getContainer();
        if (AtmApsIfDto.class.isInstance(container)) {
            return AtmApsIfDto.class.cast(container);
        }
        return null;
    }

    public static PosApsIfDto getPosApsIf(PosPortDto pos) {
        if (pos == null) {
            return null;
        }
        PortDto container = pos.getContainer();
        if (PosApsIfDto.class.isInstance(container)) {
            return PosApsIfDto.class.cast(container);
        }
        return null;
    }

    public static List<PortDto> getMemberPorts(PortDto aggregatorPort) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (aggregatorPort == null) {
            return result;
        } else if (isAggregatorPort(aggregatorPort)) {
            return result;
        }
        if (aggregatorPort instanceof AtmApsIfDto) {
            result.addAll(((AtmApsIfDto) aggregatorPort).getAtmPorts());
        } else if (aggregatorPort instanceof PosApsIfDto) {
            result.addAll(((PosApsIfDto) aggregatorPort).getPosPorts());
        } else if (aggregatorPort instanceof EthLagIfDto) {
            result.addAll(((EthLagIfDto) aggregatorPort).getBundlePorts());
        }
        return result;
    }

    public static boolean isAggregatorPort(PortDto port) {
        if (port == null) {
            return false;
        } else if (port instanceof AtmApsIfDto) {
            return true;
        } else if (port instanceof PosApsIfDto) {
            return true;
        } else if (port instanceof EthLagIfDto) {
            return true;
        }
        return false;
    }

    public static boolean isImplicitPort(PortDto port) {
        return DtoUtil.getBoolean(port, ATTR.IMPLICIT);
    }

    public static InventoryIdType selectType(LinkDto link) {
        if (link == null) {
            return null;
        } else if (link.getMemberPorts().size() == 0) {
            throw new IllegalStateException("undeterminable link type. (link has no member port)");
        }
        PortDto port = link.getMemberPorts().iterator().next();
        List<LinkDto> l2Links = getLayer2Links(port);
        for (LinkDto l2Link : l2Links) {
            if (DtoUtil.mvoEquals(link, l2Link)) {
                String linkType = getCustomLinkType(l2Link);
                if (linkType == null) {
                    return InventoryIdType.L2LINK;
                } else {
                    return InventoryIdType.L2LINK_CUSTOM;
                }
            }
        }
        LinkDto l1Link = getLayer1Link(port);
        if (l1Link != null && DtoUtil.mvoEquals(link, l1Link)) {
            return InventoryIdType.L1LINK;
        }
        throw new IllegalStateException("unknown type: " + DtoUtil.toDebugString(link));
    }

    public static String getCustomLinkType(LinkDto link) {
        if (link == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(link, ATTR.LINK_TYPE);
    }

    public static String getIpAddress(IpIfDto ipIf) {
        if (ipIf == null) {
            return null;
        }
        IpAddress ip = ipIf.getIpAddress();
        if (ip == null) {
            return null;
        }
        InetAddress inet = IpAddress.toJavaInetAddress(ip);
        return inet.getHostAddress();
    }

    public static boolean isImplicit(PortDto port) {
        return DtoUtil.getBoolean(port, ATTR.IMPLICIT);
    }

    public static NodeElementDto getSubElementWithName(NodeElementDto parent, String name) {
        if (parent == null) {
            return null;
        } else if (name == null) {
            name = "";
        }
        for (NodeElementDto sub : parent.getSubElements()) {
            if (name.equals(sub.getName())) {
                return sub;
            }
        }
        return null;
    }

    public static boolean isLoopback(PortDto port) {
        if (!(port instanceof IpIfDto)) {
            return false;
        }
        String loopbackName = PortType.LOOPBACK.getCaption();
        return loopbackName.equals(DtoUtil.getString(port, MPLSNMS_ATTR.PORT_TYPE));
    }

    public static boolean isEPS(EthLagIfDto dto) {
        String epsFlag = DtoUtil.getStringOrNull(dto, MPLSNMS_ATTR.EPS_FLAG);
        return Boolean.parseBoolean(epsFlag);
    }

    public static String getIpAddress(PortDto port) {
        IpIfDto ip = getIpOn(port);
        if (ip == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.IP_ADDRESS);
    }

    public static String getSubnetMask(PortDto port) {
        IpIfDto ip = getIpOn(port);
        if (ip == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.MASK_LENGTH);
    }

    public static IpIfDto getIpIfByIp(NodeDto node, String ip) {
        return getIpIfByIp(node, null, ip);
    }

    public static IpIfDto getIpIfByIp(NodeDto node, String vpnPrefix, String ip) {
        if (node == null || ip == null) {
            return null;
        }
        try {
            Set<IpIfDto> ports = DtoUtil.getNaefDtoFacade(node).selectNodeElements(node, IpIfDto.class,
                    SearchMethod.EXACT_MATCH, MPLSNMS_ATTR.IP_ADDRESS, ip);
            Set<IpIfDto> result = new HashSet<IpIfDto>();
            for (IpIfDto port : ports) {
                if (DtoUtil.hasStringValue(port, ATTR.VPN_PREFIX, vpnPrefix)) {
                    result.add(port);
                }
            }
            switch (result.size()) {
                case 0:
                    return null;
                case 1:
                    return result.iterator().next();
                default:
                    throw new IllegalStateException("dupliated ip-address found: [" +
                            vpnPrefix + ATTR.VPN_DELIMITER + ip + "] on node:" + node.getName());
            }
        } catch (RemoteException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static boolean isIpUsed(String ipAddress, PortDto exclude) {
        if (ipAddress == null) {
            return false;
        }
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            for (NodeDto node : conn.getActiveNodes()) {
                Set<IpIfDto> results = facade.selectNodeElements(node, IpIfDto.class,
                        SearchMethod.EXACT_MATCH, MPLSNMS_ATTR.IP_ADDRESS, ipAddress);
                if (!results.isEmpty()) {
                    for (IpIfDto result : results) {
                        if (exclude != null && DtoUtil.isSameMvoEntity(result, exclude)) {
                            continue;
                        } else {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static boolean isVirtualNode(NodeDto node) {
        if (node == null) {
            return false;
        }
        return DtoUtil.getBoolean(node, MPLSNMS_ATTR.VIRTUAL_NODE);
    }

    public static List<NodeDto> getVirtualNodes() {
        List<NodeDto> result = new ArrayList<NodeDto>();
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            for (NodeDto node : conn.getActiveNodes()) {
                if (isVirtualNode(node)) {
                    result.add(node);
                }
            }
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}