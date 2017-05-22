package voss.core.server.util;

import naef.dto.*;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanSegmentDto;
import naef.mvo.vlan.VlanType;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO;
import tef.MVO.MvoId;
import tef.skelton.dto.EntityDto.Desc;
import voss.core.server.config.AttributePolicy;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.database.ATTR;
import voss.core.server.database.CoreConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.rmi.RemoteException;
import java.util.*;

public class VlanUtil {
    public static final String KEY_POOL_ID = "pool_id";
    public static final String KEY_VLAN_ID = "vlan_id";
    private static final Logger log = LoggerFactory.getLogger(VlanUtil.class);

    private static AttributePolicy getPolicy() {
        return CoreConfiguration.getInstance().getAttributePolicy();
    }

    public static VlanIdPoolDto getPool(String poolId) throws ExternalServiceException {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<VlanIdPoolDto> roots = facade.getRootIdPools(VlanIdPoolDto.class);
            VlanIdPoolDto result = getPoolByName(roots, poolId);
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static Set<VlanIdPoolDto> getPools() throws InventoryException {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<VlanIdPoolDto> vlanPools = new HashSet<VlanIdPoolDto>();
            Set<VlanIdPoolDto> roots = facade.getRootIdPools(VlanIdPoolDto.class);
            for (VlanIdPoolDto root : roots) {
                populatePools(vlanPools, root);
            }
            return roots;
        } catch (Exception e) {
            throw new InventoryException("TODO", e);
        }
    }

    private static void populatePools(Set<VlanIdPoolDto> pools, VlanIdPoolDto parent) {
        if (parent == null) {
            return;
        }
        pools.add(parent);
        for (VlanIdPoolDto child : parent.getChildren()) {
            populatePools(pools, child);
        }
    }

    private static VlanIdPoolDto getPoolByName(Collection<VlanIdPoolDto> pools, String poolId) {
        for (VlanIdPoolDto pool : pools) {
            if (poolId.equals(pool.getName())) {
                return pool;
            }
            VlanIdPoolDto result = getPoolByName(pool.getChildren(), poolId);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public static List<VlanIdPoolDto> getAllVlanPools() throws ExternalServiceException {
        List<VlanIdPoolDto> pools = new ArrayList<VlanIdPoolDto>();
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            populatePools(pools, facade.getRootIdPools(VlanIdPoolDto.class));
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
        return pools;
    }

    private static void populatePools(List<VlanIdPoolDto> result, Collection<VlanIdPoolDto> pools) {
        result.addAll(pools);
        for (VlanIdPoolDto pool : pools) {
            Set<VlanIdPoolDto> subPools = pool.getChildren();
            if (subPools == null) {
                continue;
            }
            populatePools(result, subPools);
        }
    }

    public static VlanDto getVlan(VlanIdPoolDto pool, Integer vlanId) {
        for (VlanDto vlan : pool.getUsers()) {
            if (vlan.getVlanId().equals(vlanId)) {
                return vlan;
            }
        }
        return null;
    }

    public static VlanDto getVlan(VlanIfDto vlanIf) {
        if (vlanIf == null) {
            return null;
        }
        return vlanIf.getTrafficDomain();
    }

    public static Set<VlanDto> getVlans(PortDto port) {
        Set<VlanDto> result = new HashSet<VlanDto>();
        for (NetworkDto network : port.getNetworks()) {
            if (network instanceof VlanDto) {
                result.add((VlanDto) network);
            }
        }
        return result;
    }

    public static String getVlanName(VlanDto vlan) {
        Integer vlanId = vlan.getVlanId();
        return getVlanName(vlanId);
    }

    public static String getVlanName(Integer vlanId) {
        String vlanName = "vlan" + vlanId.toString();
        return vlanName;
    }

    public static List<VlanIfDto> getMemberVlanIfs(VlanDto vlan) {
        List<VlanIfDto> result = new ArrayList<VlanIfDto>();
        if (vlan == null) {
            return result;
        }
        Set<VlanIfDto> members = vlan.getMemberVlanifs();
        if (members == null) {
            return result;
        }
        result.addAll(members);
        return result;
    }

    public static List<PortDto> getTaggedPorts(VlanDto vlan) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (vlan == null) {
            return result;
        }
        for (VlanIfDto vlanIf : getMemberVlanIfs(vlan)) {
            result.addAll(vlanIf.getTaggedVlans());
        }
        return result;
    }

    public static List<PortDto> getTaggedPorts(VlanIfDto vlanIf) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (vlanIf == null) {
            return result;
        }
        result.addAll(vlanIf.getTaggedVlans());
        return result;
    }

    public static boolean isTaggedBoundPort(VlanIfDto vlanIf, PortDto port) {
        if (vlanIf == null) {
            return false;
        } else if (port == null) {
            return false;
        }
        for (PortDto tagged : vlanIf.getTaggedVlans()) {
            if (DtoUtil.mvoEquals(tagged, port)) {
                return true;
            }
        }
        return false;
    }

    public static List<PortDto> getUntaggedPorts(VlanIfDto vlanIf) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (vlanIf == null) {
            return result;
        }
        result.addAll(vlanIf.getUntaggedVlans());
        return result;
    }

    public static List<PortDto> getUntaggedPorts(VlanDto vlan) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (vlan == null) {
            return result;
        }
        for (VlanIfDto vlanIf : getMemberVlanIfs(vlan)) {
            result.addAll(vlanIf.getUntaggedVlans());
        }
        return result;
    }

    public static List<PortDto> getMemberPorts(VlanIfDto vif) {
        MvoDtoSet<PortDto> set = new MvoDtoSet<PortDto>();
        set.addAll(getTaggedPorts(vif));
        set.addAll(getUntaggedPorts(vif));
        List<PortDto> result = new ArrayList<PortDto>(set);
        return result;
    }

    public static List<PortDto> getMemberPorts(VlanDto vlan) {
        List<PortDto> result = new ArrayList<PortDto>();
        result.addAll(getTaggedPorts(vlan));
        result.addAll(getUntaggedPorts(vlan));
        return result;
    }

    public static List<PortDto> getUntaggedPorts(VlanDto vlan, NodeDto node) {
        List<PortDto> result = new ArrayList<PortDto>();
        for (VlanIfDto vlanIf : getMemberVlanIfs(vlan)) {
            Desc<NodeDto> nodeRef = vlanIf.get(NodeElementDto.ExtAttr.NODE);
            if (!DtoUtil.getMvoId(nodeRef).equals(DtoUtil.getMvoId(node))) {
                continue;
            }
            result.addAll(getUntaggedPorts(vlanIf));
        }
        return result;

    }

    public static List<PortDto> getTaggedPorts(VlanDto vlan, NodeDto node) {
        List<PortDto> result = new ArrayList<PortDto>();
        for (VlanIfDto vlanIf : getMemberVlanIfs(vlan)) {
            Desc<NodeDto> nodeRef = vlanIf.get(NodeElementDto.ExtAttr.NODE);
            if (!DtoUtil.getMvoId(nodeRef).equals(DtoUtil.getMvoId(node))) {
                continue;
            }
            result.addAll(getTaggedPorts(vlanIf));
        }
        return result;
    }

    public static List<PortDto> getMemberPorts(VlanDto vlan, NodeDto node) {
        List<PortDto> result = new ArrayList<PortDto>();
        result.addAll(getTaggedPorts(vlan, node));
        result.addAll(getUntaggedPorts(vlan, node));
        return result;
    }

    public static String getVlanIdString(VlanIfDto vlanIf) {
        if (vlanIf == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (isSecondaryVlanIf(vlanIf)) {
            VlanIfDto primary = (VlanIfDto) vlanIf.getOwner();
            sb.append(primary.getVlanId().intValue()).append(".");
        }
        sb.append(vlanIf.getVlanId().intValue());
        return sb.toString();
    }

    public static Integer getVlanId(VlanIfDto vlanIf) {
        if (vlanIf == null) {
            return null;
        }
        return vlanIf.getVlanId();
    }

    public static Map<String, VlanIfDto> getVlanIfsAsMap(NodeDto node) {
        final Map<String, VlanIfDto> vlanIfs = new HashMap<String, VlanIfDto>();
        for (PortDto port : node.getPorts()) {
            if (port instanceof VlanIfDto) {
                VlanIfDto vlan = (VlanIfDto) port;
                vlanIfs.put(vlan.getName(), vlan);
            }
        }
        return vlanIfs;
    }

    public static List<VlanIfDto> getAllVlanIfs(NodeDto node) {
        final List<VlanIfDto> vlanIfs = new ArrayList<VlanIfDto>();
        if (node == null) {
            return vlanIfs;
        }
        for (PortDto port : node.getPorts()) {
            if (!(port instanceof VlanIfDto)) {
                continue;
            }
            vlanIfs.add((VlanIfDto) port);
        }
        return vlanIfs;
    }

    public static List<VlanIfDto> getSwitchVlanIfs(NodeDto node) {
        final List<VlanIfDto> vlanIfs = new ArrayList<VlanIfDto>();
        if (node == null) {
            return vlanIfs;
        }
        for (NodeElementDto sub : node.getSubElements()) {
            if (!(sub instanceof VlanIfDto)) {
                continue;
            }
            VlanIfDto vlan = (VlanIfDto) sub;
            if (vlan.getTrafficDomain() == null) {
                continue;
            }
            vlanIfs.add(vlan);
        }
        return vlanIfs;
    }

    public static List<VlanIfDto> getVlanIfs(NodeDto node) {
        final List<VlanIfDto> vlanIfs = new ArrayList<VlanIfDto>();
        if (node == null) {
            return vlanIfs;
        }
        for (NodeElementDto sub : node.getSubElements()) {
            if (!(sub instanceof VlanIfDto)) {
                continue;
            }
            VlanIfDto vlan = (VlanIfDto) sub;
            if (vlan.getTrafficDomain() == null) {
                continue;
            }
            vlanIfs.add(vlan);
        }
        return vlanIfs;
    }

    public static List<VlanIfDto> getVlanIfs(PortDto port) {
        final List<VlanIfDto> vlanIfs = new ArrayList<VlanIfDto>();
        if (port == null) {
            return vlanIfs;
        } else if (!isVlanCapablePort(port)) {
            return vlanIfs;
        }
        for (NodeElementDto ne : port.getSubElements()) {
            if (VlanIfDto.class.isInstance(ne)) {
                VlanIfDto vif = (VlanIfDto) ne;
                if (NodeUtil.isImplicitPort(vif)) {
                    List<VlanIfDto> secondaries = getVlanIfs(vif);
                    vlanIfs.addAll(secondaries);
                } else {
                    if (vif.getTrafficDomain() != null) {
                        vlanIfs.add(vif);
                    } else {
                    }
                }

            }
        }
        return vlanIfs;
    }

    public static List<VlanIfDto> getBoundVlanIfs(PortDto port) {
        final List<VlanIfDto> vlanIfs = new ArrayList<VlanIfDto>();
        if (port == null) {
            return vlanIfs;
        }
        if (!isVlanCapablePort(port)) {
            return vlanIfs;
        }
        NodeDto node = port.getNode();
        VLAN_LOOP:
        for (PortDto p : node.getPorts()) {
            if (!(p instanceof VlanIfDto)) {
                continue;
            } else if (!DtoUtil.mvoEquals(p.getOwner(), node)) {
                continue;
            }
            VlanIfDto vlanIf = (VlanIfDto) p;
            for (PortDto tagged : vlanIf.getTaggedVlans()) {
                if (DtoUtil.mvoEquals(port, tagged)) {
                    vlanIfs.add(vlanIf);
                    continue VLAN_LOOP;
                }
            }
            for (PortDto untagged : vlanIf.getUntaggedVlans()) {
                if (DtoUtil.mvoEquals(port, untagged)) {
                    vlanIfs.add(vlanIf);
                    continue VLAN_LOOP;
                }
            }
        }
        return vlanIfs;
    }

    public static VlanIfDto getUntaggedVlanIfOn(PortDto untaggedPort) {
        if (untaggedPort == null) {
            return null;
        }
        if (VlanIfDto.class.isInstance(untaggedPort)) {
            return null;
        }
        List<VlanIfDto> result = new ArrayList<VlanIfDto>();
        for (PortDto xconnect : untaggedPort.getCrossConnections()) {
            if (!VlanIfDto.class.isInstance(xconnect)) {
                continue;
            }
            result.add((VlanIfDto) xconnect);
        }
        switch (result.size()) {
            case 0:
                return null;
            case 1:
                return result.get(0);
        }
        throw new IllegalStateException("2 or more untagged bound vlan-if found: "
                + DtoUtil.toDebugString(untaggedPort));
    }

    public static Set<VlanIfDto> getTaggedVlanIfsOn(PortDto taggedPort) {
        Set<VlanIfDto> result = new HashSet<VlanIfDto>();
        if (taggedPort == null) {
            return result;
        }
        for (PortDto upper : taggedPort.getUpperLayers()) {
            if (!VlanIfDto.class.isInstance(upper)) {
                continue;
            }
            result.add((VlanIfDto) upper);
        }
        return result;
    }

    public static List<NodeElementDto> getLocalVlanDomains(NodeDto node) {
        List<NodeElementDto> result = new ArrayList<NodeElementDto>();
        if (node == null) {
            return result;
        }
        if (getVlanType(node) != null) {
            result.add(node);
        }
        for (JackDto jack : node.getJacks()) {
            PortDto port = jack.getPort();
            if (port == null) {
                continue;
            }
            VlanType type = getVlanType(port);
            if (type == null) {
                continue;
            }
            result.add(port);
        }
        return result;
    }

    public static List<PortDto> getVlanEnabledPorts(NodeDto node) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (isVlanEnabled(node)) {
            for (JackDto jack : node.getJacks()) {
                PortDto p = jack.getPort();
                result.add(p);
            }
            return result;
        }
        for (PortDto port : node.getPorts()) {
            if (isVlanEnabled(port)) {
                result.add(port);
            }
        }
        return result;
    }

    public static String getPortMode(PortDto port) {
        return DtoUtil.getStringOrNull(port, getPolicy().getPortModeAttributeName());
    }

    public static String getSwitchPortMode(PortDto port) {
        return DtoUtil.getStringOrNull(port, getPolicy().getSwitchPortModeAttributeName());
    }

    public static boolean isPortModeSwitch(String portMode) {
        if (portMode == null) {
            return false;
        }
        return getPolicy().isPortModeSwitch(portMode);
    }

    public static boolean isPortModeRouter(String portMode) {
        if (portMode == null) {
            return false;
        }
        return getPolicy().isPortModeRouter(portMode);
    }

    public static boolean isSwitchPort(PortDto port) {
        if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
            String portMode = getPortMode(port);
            return getPolicy().isPortModeSwitch(portMode);
        }
        return false;
    }

    public static boolean isSwitchTrunkPort(PortDto port) {
        return isSwitchPort(port) && isTrunk(port);
    }

    public static boolean isRouterPort(PortDto port) {
        if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
            String portMode = getPortMode(port);
            return getPolicy().isPortModeRouter(portMode);
        }
        return false;
    }

    public static boolean isRouterTrunkPort(PortDto port) {
        return isRouterPort(port) && isTrunk(port);
    }

    public static boolean isSimpleEthernetPort(PortDto port) {
        if (port instanceof EthPortDto || port instanceof EthLagIfDto) {
            VlanType vlanType = getVlanType(port.getNode());
            String portMode = getPortMode(port);
            String switchPortMode = getSwitchPortMode(port);
            return Util.isAllNull(vlanType, portMode, switchPortMode);
        }
        return false;
    }

    public static VlanType getVlanType(NodeElementDto element) {
        if (element == null) {
            return null;
        }
        Object o = element.getValue(ATTR.FEATURE_VLAN);
        if (o == null) {
            return null;
        }
        if (!(o instanceof VlanType)) {
            return null;
        }
        return (VlanType) o;
    }

    public static boolean isVlanEnabled(NodeElementDto element) {
        if (element == null) {
            return false;
        }
        return getVlanType(element) != null;
    }

    public static NodeElementDto getVlanOwner(PortDto port) {
        if (isRouterVlanEnabledPort(port)) {
            return port;
        } else {
            return port.getNode();
        }
    }

    public static boolean isRouterVlanCapablePort(PortDto port) {
        return isRouterPort(port) && isTrunk(port);
    }

    public static boolean isRouterVlanEnabledPort(PortDto port) {
        if (!isRouterVlanCapablePort(port)) {
            return false;
        }
        VlanType type = getVlanType(port);
        return type != null;
    }

    public static boolean isSwitchVlanCapablePort(PortDto port) {
        return isSwitchPort(port);
    }

    public static boolean isSwitchVlanEnabledPort(PortDto port) {
        if (!isSwitchVlanCapablePort(port)) {
            return false;
        }
        VlanType type = getVlanType(port.getNode());
        return type != null;
    }

    public static boolean isBridgeCapablePort(PortDto port) {
        if (port instanceof AtmPvcIfDto || port instanceof FrPvcIfDto) {
            return true;
        }
        return false;
    }

    public static boolean isBridgedPort(PortDto port) {
        if (port instanceof AtmPvcIfDto || port instanceof FrPvcIfDto) {
            PortDto bridge = getBridgePort(port);
            return bridge != null;
        } else if (port instanceof EthPortDto) {
            NodeElementDto owner = port.getOwner();
            if (owner != null && owner instanceof AtmPvcIfDto) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBridgePort(PortDto port) {
        if (port instanceof EthPortDto) {
            NodeElementDto owner = port.getOwner();
            if (owner == null) {
                return false;
            }
            if (owner instanceof AtmPvcIfDto) {
                return true;
            } else if (owner instanceof FrPvcIfDto) {
                return true;
            }
        }
        return false;
    }

    public static PortDto getBridgePort(PortDto bridged) {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            Set<NodeElementDto> descendants = facade.getDescendants(bridged, false);
            if (descendants.size() == 0) {
                return null;
            }
            for (NodeElementDto descendant : descendants) {
                if (descendant instanceof EthPortDto) {
                    EthPortDto eth = (EthPortDto) descendant;
                    NodeElementDto owner = eth.getOwner();
                    if (DtoUtil.isSameMvoEntity(bridged, owner)) {
                        return eth;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static boolean isTrunk(PortDto port) {
        String switchPortMode = getSwitchPortMode(port);
        if (switchPortMode == null) {
            return false;
        }
        return getPolicy().isSwitchPortTrunk(switchPortMode);
    }

    public static boolean isAccess(PortDto port) {
        String switchPortMode = getSwitchPortMode(port);
        if (switchPortMode == null) {
            return false;
        }
        return !getPolicy().isSwitchPortTrunk(switchPortMode);
    }

    public static boolean isTrunkLink(LinkDto link) {
        if (link == null) {
            return false;
        }
        if (link.isDemarcationLink()) {
            return false;
        }
        for (PortDto port : link.getMemberPorts()) {
            boolean isTrunkPort = isTrunk(port);
            if (!isTrunkPort) {
                return false;
            }
        }
        return true;
    }

    public static boolean isVlanLinkPort(PortDto port) {
        LinkDto link = NodeUtil.getLayer2Link(port);
        if (link == null) {
            return false;
        }
        PortDto neighbor = NodeUtil.getLayer2Neighbor(port);
        return VlanUtil.isTrunk(port) && VlanUtil.isTrunk(neighbor);
    }

    public static VlanSegmentDto getVlanLinkBetween(VlanIfDto vif1, VlanIfDto vif2) {
        if (vif1 == null || vif2 == null) {
            throw new IllegalArgumentException();
        }
        for (VlanSegmentDto vlanLink : vif1.getVlanLinks()) {
            for (PortDto member : vlanLink.getMemberPorts()) {
                if (DtoUtil.mvoEquals(member, vif2)) {
                    return vlanLink;
                }
            }
        }
        return null;
    }

    public static VlanSegmentDto getVlanLinkOver(VlanIfDto vif, PortDto boundPort) {
        if (vif == null || boundPort == null) {
            throw new IllegalArgumentException();
        }
        if (vif.getTrafficDomain() != null) {
            try {
                return DtoUtil.getNaefDtoFacade(vif).getVlanLink(vif.getTrafficDomain(), boundPort);
            } catch (RemoteException e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
        for (VlanSegmentDto vlanLink : vif.getVlanLinks()) {
            for (NetworkDto lower : vlanLink.getLowerLayerLinks()) {
                for (PortDto member : lower.getMemberPorts()) {
                    if (DtoUtil.mvoEquals(member, boundPort)) {
                        return vlanLink;
                    }
                }
            }
        }
        return null;
    }

    public static String getDomain(VlanIfDto vlanIf) {
        if (vlanIf == null) {
            return null;
        }
        NodeElementDto owner = vlanIf.getOwner();
        if (owner == null) {
            return null;
        }
        if (owner instanceof NodeDto) {
            return "Switch";
        } else if (owner instanceof VlanIfDto) {
            return getDomain((VlanIfDto) owner);
        } else if (owner instanceof PortDto) {
            return NameUtil.getIfName((PortDto) owner);
        }
        return owner.getAbsoluteName();
    }

    public static boolean isRouterVlanIf(NaefDto vlanIf) {
        if (vlanIf == null) {
            return false;
        } else if (!VlanIfDto.class.isInstance(vlanIf)) {
            return false;
        }
        return DtoUtil.isOwnerClass(PortDto.class, (VlanIfDto) vlanIf);
    }

    public static PortDto getParentPort(VlanIfDto routerVlanIf) {
        if (routerVlanIf == null) {
            return null;
        }
        NodeElementDto owner = routerVlanIf.getOwner();
        while (VlanIfDto.class.isInstance(owner)) {
            owner = owner.getOwner();
        }
        if (PortDto.class.isInstance(owner)) {
            return (PortDto) owner;
        }
        return null;
    }

    public static boolean isSwitchVlanIf(VlanIfDto vlanIf) {
        if (vlanIf == null) {
            return false;
        }
        return DtoUtil.isOwnerClass(NodeDto.class, vlanIf);
    }

    public static VlanIfDto getVlanIf(NodeElementDto owner, int id) {
        if (owner == null) {
            return null;
        }
        Set<NodeElementDto> children = owner.getSubElements();
        for (NodeElementDto child : children) {
            if (!(child instanceof VlanIfDto)) {
                continue;
            }
            VlanIfDto vif = (VlanIfDto) child;
            if (vif.getVlanId().intValue() != id) {
                continue;
            }
            return vif;
        }
        return null;
    }

    public static VlanIfDto getSwitchVlanIf(NodeDto node, int id) {
        if (node == null) {
            return null;
        }
        for (NodeElementDto ne : node.getSubElements()) {
            if (!(ne instanceof VlanIfDto)) {
                continue;
            }
            VlanIfDto vif = (VlanIfDto) ne;
            if (vif.getVlanId().intValue() != id) {
                continue;
            } else if (!DtoUtil.isOwnerClass(NodeDto.class, vif)) {
                continue;
            }
            return vif;
        }
        return null;
    }

    public static List<VlanIfDto> getVlanIf(NodeDto node, VlanDto vlan) {
        if (node == null) {
            throw new IllegalStateException("node is null.");
        }
        List<VlanIfDto> result = new ArrayList<VlanIfDto>();
        if (vlan == null) {
            return result;
        }
        for (VlanIfDto vlanIf : vlan.getMemberVlanifs()) {
            Desc<NodeElementDto> owner = vlanIf.get(NodeElementDto.ExtAttr.OWNER);
            if (owner != null) {
                if (DtoUtil.getMvoId(owner).equals(DtoUtil.getMvoId(node))) {
                    result.add(vlanIf);
                    continue;
                }
            }
            Desc<NodeDto> nodeRef = vlanIf.get(NodeElementDto.ExtAttr.NODE);
            if (nodeRef != null) {
                if (DtoUtil.getMvoId(nodeRef).equals(DtoUtil.getMvoId(node))) {
                    result.add(vlanIf);
                    continue;
                }
            }
        }
        return result;
    }

    public static VlanIfDto getRouterVlanIf(PortDto port, VlanDto vlan) {
        if (port == null) {
            return null;
        } else if (vlan == null) {
            return null;
        }
        for (VlanIfDto vlanIf : vlan.getMemberVlanifs()) {
            Desc<NodeElementDto> owner = vlanIf.get(NodeElementDto.ExtAttr.OWNER);
            if (owner == null) {
                continue;
            } else if (DtoUtil.getMvoId(owner).equals(DtoUtil.getMvoId(port))) {
                return vlanIf;
            }
        }
        return null;
    }

    public static List<VlanIfDto> getRouterVlanIfs(NodeDto node) {
        List<VlanIfDto> result = new ArrayList<VlanIfDto>();
        for (VlanIfDto vlanIf : getAllVlanIfs(node)) {
            if (DtoUtil.isOwnerClass(PortDto.class, vlanIf)) {
                result.add(vlanIf);
            }
        }
        return result;
    }

    public static VlanIfDto getAnyVlanIf(NodeElementDto dto, Integer id) {
        if (dto instanceof NodeDto) {
            return getSwitchVlanIf((NodeDto) dto, id);
        } else if (dto instanceof PortDto) {
            return getRouterVlanIf((PortDto) dto, id);
        }
        return null;
    }

    public static VlanIfDto getSwitchVlanIf(NodeDto node, Integer id) {
        if (!isVlanEnabled(node)) {
            return null;
        }
        return getVlanIf(node, id);
    }

    public static VlanIfDto getRouterVlanIf(PortDto port, Integer id) {
        if (!isRouterVlanEnabledPort(port)) {
            return null;
        }
        return getVlanIf(port, id);
    }

    public static VlanIfDto getVlanIf(NodeElementDto owner, Integer id) {
        for (NodeElementDto sub : owner.getSubElements()) {
            if (!VlanIfDto.class.isInstance(sub)) {
                continue;
            }
            VlanIfDto vif = VlanIfDto.class.cast(sub);
            if (vif.getVlanId().equals(id)) {
                return vif;
            }
        }
        return null;
    }

    public static List<VlanIfDto> getOrphanedAllRouterVlanIf(NodeDto node, VlanDto vlan) {
        String vlanName = getVlanName(vlan);
        List<VlanIfDto> result = new ArrayList<VlanIfDto>();
        for (VlanIfDto vlanIf : getRouterVlanIfs(node)) {
            if (vlanIf.getName().equals(vlanName)) {
                result.add(vlanIf);
            }
        }
        return result;
    }

    public static boolean isVlanCapablePort(PortDto port) {
        if (port == null) {
            return false;
        } else if (VlanIfDto.class.isInstance(port)) {
            return true;
        } else if (port instanceof EthPortDto) {
            return true;
        } else if (port instanceof EthLagIfDto) {
            return true;
        }
        return false;
    }

    public static boolean isVlanAllocatablePort(PortDto port) {
        PortDto neighbor = NodeUtil.getLayer2Neighbor(port);
        if (neighbor != null) {
            if (!isSimpleEthernetPort(neighbor) && !isVlanAllocatablePortInner(neighbor)) {
                return false;
            }
        }
        return isVlanAllocatablePortInner(port);
    }

    private static boolean isVlanAllocatablePortInner(PortDto port) {
        if (!isVlanCapablePort(port)) {
            return false;
        }
        if (isSwitchPort(port)) {
            return isVlanEnabled(port.getNode());
        } else if (isRouterPort(port)) {
            return isVlanEnabled(port) && isTrunk(port);
        } else if (port instanceof AtmPvcIfDto) {
            return isVlanEnabled(port.getNode()) && !isBridgedPort(port);
        }
        return false;
    }

    public static Map<String, VlanDto> getAccessVlansOn(NodeDto node) throws InventoryException {
        Map<String, VlanDto> result = new HashMap<String, VlanDto>();
        if (node == null) {
            return result;
        }
        for (VlanIfDto vlanIf : getSwitchVlanIfs(node)) {
            VlanDto vlan = vlanIf.getTrafficDomain();
            if (vlan == null) {
                continue;
            }
            for (PortDto port : vlanIf.getUntaggedVlans()) {
                result.put(DtoUtil.getMvoId(port).toString(), vlan);
            }
        }
        return result;
    }

    public static List<VlanDto> getVlansOn(PortDto port) {
        List<VlanDto> result = new ArrayList<VlanDto>();
        if (port == null) {
            return result;
        }
        Set<MvoId> known = new HashSet<MvoId>();
        for (NetworkDto network : port.getNetworks()) {
            if (!(network instanceof VlanDto)) {
                continue;
            }
            VlanDto vlan = (VlanDto) network;
            VlanIfDto vif = getVlanIf(vlan, port);
            if (vif != null) {
                result.add(vlan);
                known.add(DtoUtil.getMvoId(vlan));
            }
        }
        for (PortDto upper : port.getUpperLayers()) {
            if (!(upper instanceof VlanIfDto)) {
                continue;
            }
            VlanIfDto vif = (VlanIfDto) upper;
            VlanDto vlan = vif.getTrafficDomain();
            if (vlan == null) {
                continue;
            } else if (known.contains(DtoUtil.getMvoId(vlan))) {
                continue;
            }
            result.add(vif.getTrafficDomain());
        }
        return result;
    }

    public static List<PortDto> getTrunkLink(List<PortDto> ports, int vlanId) {
        List<PortDto> result = new ArrayList<PortDto>();
        Set<String> nodeNames = new HashSet<String>();
        for (PortDto port : ports) {
            getTrunkLink(result, nodeNames, port, vlanId);
        }
        return result;
    }

    private static void getTrunkLink(List<PortDto> found, Set<String> foundPortNames, PortDto port, int vlanId) {
        log.trace("getTrunkLink(): port=" + port.getAbsoluteName());
        if (isRouterVlanEnabledPort(port)) {
            foundPortNames.add(port.getAbsoluteName());
            found.add(port);
            return;
        }
        if (isSwitchVlanCapablePort(port)) {
            if (isTrunk(port)) {
                found.add(port);
                foundPortNames.add(port.getAbsoluteName());
            }
            for (PortDto otherTrunkPort : getSwitchTrunkPorts(port.getNode())) {
                if (foundPortNames.contains(otherTrunkPort.getAbsoluteName())) {
                    continue;
                }
                log.debug("getTrunkLink(): otherTrunkPort=" + otherTrunkPort.getAbsoluteName());
                PortDto l2Neighbor = NodeUtil.getLayer2Neighbor(otherTrunkPort);
                if (l2Neighbor == null) {
                    continue;
                } else if (foundPortNames.contains(l2Neighbor.getAbsoluteName())) {
                    continue;
                } else if (!isTrunk(l2Neighbor)) {
                    continue;
                }
                log.debug("getTrunkLink(): l2Neighbor=" + l2Neighbor.getAbsoluteName());
                getTrunkLink(found, foundPortNames, l2Neighbor, vlanId);
            }
        }
    }

    public static List<PortDto> getSwitchTrunkPorts(NodeDto node) {
        List<PortDto> result = new ArrayList<PortDto>();
        if (!isVlanEnabled(node)) {
            return result;
        }
        for (JackDto jack : node.getJacks()) {
            PortDto p = jack.getPort();
            if (isSwitchVlanCapablePort(p) && isTrunk(p)) {
                result.add(p);
            }
        }
        return result;
    }

    public static class VlanComparator implements Comparator<VlanDto> {
        @Override
        public int compare(VlanDto o1, VlanDto o2) {
            return o1.getVlanId().intValue() - o2.getVlanId().intValue();
        }

    }

    public static class VlanIfComparator implements Comparator<VlanIfDto> {
        @Override
        public int compare(VlanIfDto o1, VlanIfDto o2) {
            return o1.getVlanId().intValue() - o2.getVlanId().intValue();
        }
    }

    public static int getNumberOfVlans(PortDto port) {
        int count = 0;
        for (NetworkDto network : port.getNetworks()) {
            if (network instanceof VlanDto) {
                count++;
            }
        }
        return count;
    }

    public static PortDto getAnotherVlanLinkPort(VlanSegmentDto vlanLink, PortDto thisPort) {
        if (vlanLink.isDemarcationLink()) {
            throw new IllegalStateException("vlan-link is demarcation link.");
        }
        HashSet<MvoId> members = new HashSet<MvoId>();
        for (NetworkDto lower : vlanLink.getLowerLayerLinks()) {
            for (PortDto member : lower.getMemberPorts()) {
                members.add(DtoUtil.getMvoId(member));
            }
        }
        members.remove(DtoUtil.getMvoId(thisPort));
        if (members.size() != 1) {
            throw new IllegalStateException("not point-to-point link: " + vlanLink.getAbsoluteName());
        }
        MvoId neighborId = members.iterator().next();
        for (NetworkDto lower : vlanLink.getLowerLayerLinks()) {
            for (PortDto member : lower.getMemberPorts()) {
                if (DtoUtil.getMvoId(member).equals(neighborId)) {
                    return member;
                }
            }
        }
        throw new IllegalStateException("another member port not found. " + neighborId);
    }

    public static VlanIfDto getAnotherVlanLinkVlanIf(VlanSegmentDto vlanLink, VlanIfDto thisVlanIf) {
        if (vlanLink.isDemarcationLink()) {
            throw new IllegalStateException("vlan-link is demarcation link.");
        }
        HashSet<MvoId> members = new HashSet<MvoId>();
        for (PortDto member : vlanLink.getMemberPorts()) {
            if (!(member instanceof VlanIfDto)) {
                throw new IllegalStateException();
            }
            members.add(DtoUtil.getMvoId(member));
        }
        members.remove(DtoUtil.getMvoId(thisVlanIf));
        if (members.size() != 1) {
            throw new IllegalStateException("not point-to-point link: " + vlanLink.getAbsoluteName());
        }
        MvoId neighborId = members.iterator().next();
        for (PortDto member : vlanLink.getMemberPorts()) {
            if (member instanceof VlanIfDto && DtoUtil.getMvoId(member).equals(neighborId)) {
                return (VlanIfDto) member;
            }
        }
        throw new IllegalStateException("another member port not found. " + neighborId);
    }

    public static VlanIfDto getVlanIf(VlanDto vlan, PortDto port) {
        if (vlan == null || port == null) {
            return null;
        }
        for (VlanIfDto member : vlan.getMemberVlanifs()) {
            if (!DtoUtil.mvoEquals(member.getNode(), port.getNode())) {
                continue;
            }
            for (PortDto tagged : member.getTaggedVlans()) {
                if (DtoUtil.mvoEquals(tagged, port)) {
                    return member;
                }
            }
            for (PortDto untagged : member.getUntaggedVlans()) {
                if (DtoUtil.mvoEquals(untagged, port)) {
                    return member;
                }
            }
        }
        return null;
    }

    public static VlanIfDto getSecondaryVlanIf(VlanIfDto primaryVlanIf, int secondaryID) {
        if (primaryVlanIf == null) {
            return null;
        }
        for (NodeElementDto sub : primaryVlanIf.getSubElements()) {
            if (!VlanIfDto.class.isInstance(sub)) {
                continue;
            }
            VlanIfDto subVlan = (VlanIfDto) sub;
            if (subVlan.getVlanId().intValue() == secondaryID) {
                return subVlan;
            }
        }
        return null;
    }

    public static List<VlanIfDto> getSecondaryVlanIfs(VlanIfDto primaryVlanIf) {
        List<VlanIfDto> result = new ArrayList<VlanIfDto>();
        if (primaryVlanIf == null) {
            return result;
        }
        for (NodeElementDto sub : primaryVlanIf.getSubElements()) {
            if (!VlanIfDto.class.isInstance(sub)) {
                continue;
            }
            result.add((VlanIfDto) sub);
        }
        return result;
    }

    public static boolean hasSecondaryVlanIf(VlanIfDto vlanIf) {
        List<VlanIfDto> secondaries = getSecondaryVlanIfs(vlanIf);
        return secondaries.size() > 0;
    }

    public static boolean isSecondaryVlanIf(VlanIfDto vlanIf) {
        if (vlanIf == null) {
            return false;
        }
        return DtoUtil.isOwnerClass(VlanIfDto.class, vlanIf);
    }

    public static VlanSegmentDto getCommonVlanLink(VlanIfDto vif1, VlanIfDto vif2) {
        if (vif1 == null || vif2 == null) {
            return null;
        }
        Set<MvoId> set = new HashSet<MvoId>();
        for (VlanSegmentDto vlink : vif1.getVlanLinks()) {
            set.add(DtoUtil.getMvoId(vlink));
        }
        for (VlanSegmentDto vlink : vif2.getVlanLinks()) {
            if (set.contains(DtoUtil.getMvoId(vlink))) {
                return vlink;
            }
        }
        return null;
    }

    public static boolean isSvi(PortDto port) {
        if (port == null) {
            return false;
        }
        return DtoUtil.getBoolean(port, MPLSNMS_ATTR.SVI_ENABLED);
    }
}