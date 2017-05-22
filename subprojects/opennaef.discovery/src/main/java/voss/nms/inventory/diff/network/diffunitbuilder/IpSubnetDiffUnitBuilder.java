package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.mvo.ip.IpAddress;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.GenericAttributeCommandBuilder;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;
import voss.nms.inventory.builder.IpSubnetCommandBuilder;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.*;
import voss.nms.inventory.diff.network.*;

import java.io.IOException;
import java.util.*;

public class IpSubnetDiffUnitBuilder extends DiffUnitBuilderImpl<IpSubnet, IpSubnetDto> {
    private final static Logger log = LoggerFactory.getLogger(IpSubnetDiffUnitBuilder.class);
    private static final String eventMatter = "IP-Subnet policy violation found (diff).";
    private final String editorName;
    @SuppressWarnings("unused")
    private final IpAddressDB ipDB;

    public IpSubnetDiffUnitBuilder(DiffSet set, String editorName, IpAddressDB ipDB) throws InventoryException, ExternalServiceException, IOException {
        super(set, DiffObjectType.IP_SUBNET.getCaption(), DiffConstants.ipSubnetDepth, editorName);
        this.editorName = editorName;
        this.ipDB = ipDB;
    }

    @Override
    protected DiffUnit create(String id, IpSubnet networkSubnet) throws IOException,
            InventoryException, ExternalServiceException, DuplicationException {
        if (!networkSubnet.canApplyChange()) {
            return null;
        }
        String vpnPrefix = networkSubnet.getVpnPrefix();
        IpAddress ip = IpAddress.gain(networkSubnet.getStartAddress());
        Integer maskLength = networkSubnet.getMaskLength();
        log.debug("vpn-prefix: " + vpnPrefix + ", ip-address: " + ip + "/" + maskLength.intValue());
        InventoryConnector conn = InventoryConnector.getInstance();
        NaefDtoFacade facade = conn.getDtoFacade();
        IpSubnetAddressDto parentSubnetAddress = conn.getRootIpSubnetAddressByVpn(vpnPrefix);
        IpSubnetAddressDto leaf = facade.getLeafIdPool(parentSubnetAddress, ip);
        if (leaf == null) {
            throw new IllegalStateException("[ERROR] no ip-subnet-address under " + DtoUtil.toDebugString(parentSubnetAddress));
        }
        log.debug("parent ip-subnet-address found: " + leaf.getAbsoluteName());
        Integer leafMaskLength = leaf.getSubnetMask();
        if (leafMaskLength == null) {
            throw new IllegalStateException("[ERROR] no mask-length on subnet-address: " + DtoUtil.toDebugString(leaf));
        }
        if (leafMaskLength.intValue() > maskLength.intValue()) {
            throw new IllegalStateException("[ERROR] parent is lesser subnet: " + leaf.getName() + "(" + DtoUtil.toDebugString(leaf));
        }
        IpSubnetNamespaceDto parent = leaf.getIpSubnetNamespace();
        if (parent == null) {
            throw new IllegalStateException("[ERROR] no parent namespace found: vpn-prefix=" + vpnPrefix);
        }
        IpSubnetCommandBuilder builder = new IpSubnetCommandBuilder(parent, this.editorName);
        builder.setStartAddress(networkSubnet.getStartAddress());
        builder.setMaskLength(networkSubnet.getMaskLength());
        builder.setFoundOnNetwork(Boolean.TRUE);
        builder.setSource(DiffCategory.DISCOVERY.name());
        builder.setVersionCheck(false);
        super.applyExtraAttributes(DiffOperationType.ADD, builder, networkSubnet, null);

        Integer vlanID = networkSubnet.getVlanID();
        if (vlanID != null) {
            String lowerLayer = InventoryBuilder.getRelativeName(ATTR.POOL_TYPE_VLAN_DOT1Q, policy.getDefaultVlanPoolName())
                    + ATTR.NAME_DELIMITER_PRIMARY
                    + InventoryBuilder.getRelativeName(ATTR.NETWORK_TYPE_ID, vlanID.toString());
            builder.addLowerLayerNetwork(lowerLayer);
            log.debug("add lower-layer: vlan" + vlanID.intValue());
        }

        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return null;
        }
        String linkID = networkSubnet.getIpSubnetAddressName();
        DiffUnit unit = new DiffUnit(linkID, DiffOperationType.ADD);
        unit.addBuilder(builder);
        return unit;
    }

    @Override
    protected DiffUnit update(String id, IpSubnet networkSubnet, IpSubnetDto dbSubnet)
            throws IOException, InventoryException, ExternalServiceException, DuplicationException {
        if (!networkSubnet.canApplyChange()) {
            return null;
        }
        IpSubnetCommandBuilder builder = new IpSubnetCommandBuilder(dbSubnet, this.editorName);
        builder.setFoundOnNetwork(Boolean.TRUE);
        builder.setVersionCheck(false);
        super.applyExtraAttributes(DiffOperationType.UPDATE, builder, networkSubnet, dbSubnet);

        Integer vlanID = networkSubnet.getVlanID();
        if (vlanID != null) {
            String lowerLayer = InventoryBuilder.getRelativeName(ATTR.POOL_TYPE_VLAN_DOT1Q, policy.getDefaultVlanPoolName())
                    + ATTR.NAME_DELIMITER_PRIMARY
                    + InventoryBuilder.getRelativeName(ATTR.NETWORK_TYPE_ID, vlanID.toString());
            builder.addLowerLayerNetwork(lowerLayer);
            log.debug("add lower-layer: vlan" + vlanID.intValue());
        }

        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return null;
        }
        DiffUnit unit = new DiffUnit(networkSubnet.getIpSubnetAddressName(), DiffOperationType.UPDATE);
        unit.addBuilder(builder);
        return unit;
    }

    @Override
    protected DiffUnit delete(String id, IpSubnetDto subnet) throws IOException,
            InventoryException, ExternalServiceException, DuplicationException {
        IpSubnetCommandBuilder builder = new IpSubnetCommandBuilder(subnet, this.editorName);
        builder.setVersionCheck(false);
        super.applyExtraAttributes(DiffOperationType.REMOVE, builder, null, subnet);
        builder.buildDeleteCommand();
        DiffUnit unit = new DiffUnit(subnet.getSubnetName(), DiffOperationType.REMOVE);
        unit.addBuilder(builder);
        return unit;
    }

    @Override
    protected DiffUnit changeDeletedStatus(String id, IpSubnetDto db) throws IOException, InventoryException, ExternalServiceException {
        Boolean currentStatus = DtoUtil.getBoolean(db, MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK);
        GenericAttributeCommandBuilder builder = new GenericAttributeCommandBuilder(db, editorName);
        builder.setVersionCheck(false);
        builder.setConstraint(db.getClass());
        if (currentStatus == null || currentStatus.booleanValue()) {
            builder.setAttribute(MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK, Boolean.FALSE.toString());
        }
        BuildResult result = builder.buildCommand();
        if (result != BuildResult.SUCCESS) {
            return null;
        } else if (!builder.hasChange()) {
            return null;
        }
        DiffUnit unit = new DiffUnit(db.getSubnetName(), DiffOperationType.UPDATE);
        unit.addBuilder(builder);
        unit.setSourceSystem(DiffCategory.DISCOVERY.name());
        unit.setStatus(DiffStatus.INITIAL);
        unit.setTypeName(this.typeName);
        unit.setDepth(this.depth);
        return unit;
    }

    public static Map<String, IpSubnetDto> getInventorySubnets(DiffPolicy policy, Set<NodeDto> discoveredNodes,
                                                               List<PolicyViolation> illegalEvents)
            throws IOException, InventoryException, ExternalServiceException {
        Map<String, IpSubnetDto> dbMap = new HashMap<String, IpSubnetDto>();
        Map<String, Object> ipAddressToObject = new HashMap<String, Object>();
        List<IpSubnetNamespaceDto> pools = InventoryConnector.getInstance().getActiveRootIpSubnetNamespaces();
        List<IpSubnetDto> subnets = new ArrayList<IpSubnetDto>();
        for (IpSubnetNamespaceDto pool : pools) {
            for (IpSubnetDto subnet : pool.getUsers()) {
                if (subnet.getSubnetAddress() == null) {
                    log.debug("[WARN] Unexpected ip-subnet without ip-subnet-address found: ignore this.");
                    continue;
                }
                List<PolicyViolation> violations = checkMember(subnet, ipAddressToObject);
                if (violations.size() > 0) {
                    illegalEvents.addAll(violations);
                    continue;
                }
                if (isAnonymousLink(subnet)) {
                    log.debug("anonymous-link found: ignoring because anonymous link is out of diff's scope.");
                    continue;
                }
                subnets.add(subnet);
            }
        }
        for (IpSubnetDto subnet : subnets) {
            IpSubnetAddressDto addr = subnet.getSubnetAddress();
            if (addr == null) {
                continue;
            }
            dbMap.put(addr.getName(), subnet);
            log.debug("found link on DB: [" + addr.getName() + "]");
        }
        return dbMap;
    }

    private static List<PolicyViolation> checkMember(IpSubnetDto subnet, Map<String, Object> ipAddressToObject) {
        List<PolicyViolation> illegalEvents = new ArrayList<PolicyViolation>();
        for (PortDto m : subnet.getMemberIpifs()) {
            IpIfDto member = NodeUtil.toIpIfDto(m);
            if (member == null) {
                throw new IllegalStateException("[ERROR] non ip-if member on ip-subnet found: " + m.getAbsoluteName());
            }
            if (member.getAssociatedPorts().size() == 0) {
                String msg = "[WARN] found ip-if without associated-port : "
                        + "; " + member.getAbsoluteName();
                createPolicyViolation(illegalEvents, member, eventMatter, msg);
            }
            String ip = DtoUtil.getStringOrNull(member, MPLSNMS_ATTR.IP_ADDRESS);
            if (ip == null) {
                continue;
            }
            Object previous = ipAddressToObject.put(ip, member);
            if (previous != null) {
                createPolicyViolation(illegalEvents, previous, member, ip, eventMatter);
                continue;
            }
        }
        return illegalEvents;
    }

    private static boolean isAnonymousLink(IpSubnetDto subnet) {
        for (PortDto member : subnet.getMemberIpifs()) {
            String ip = DtoUtil.getStringOrNull(member, MPLSNMS_ATTR.IP_ADDRESS);
            if (ip == null) {
                return true;
            }
        }
        return false;
    }

    private static void createPolicyViolation(List<PolicyViolation> illegalEvents, PortDto port, String eventMatter,
                                              String msg) {
        PolicyViolation violation = new PolicyViolation(
                InventoryIdCalculator.getId(port),
                port.getNode().getName(),
                DtoUtil.getStringOrNull(port.getNode(), MPLSNMS_ATTR.MANAGEMENT_IP),
                eventMatter, msg);
        log.warn(violation.toString());
        illegalEvents.add(violation);
    }

    private static void createPolicyViolation(List<PolicyViolation> illegalEvents, Object previous, IpIfDto member, String ip,
                                              String eventMatter) {
        if (previous instanceof IpIfDto) {
            IpIfDto ipIf = (IpIfDto) previous;
            String msg = "[WARN] duplicated ip: " + ip + ", " + DtoUtil.toDebugString((IpIfDto) previous) + "; "
                    + member.getAbsoluteName();
            createPolicyViolation(illegalEvents, ipIf, eventMatter, msg);
        } else {
            String msg = "[WARN] duplicated ip? : " + ip + ", " + previous.getClass().getName()
                    + ":" + previous.toString() + "; " + member.getAbsoluteName();
            createPolicyViolation(illegalEvents, member, eventMatter, msg);
        }
    }

    public static Map<String, IpSubnet> getNetworkSubnets(IpAddressDB ipDB) {
        Map<String, IpSubnet> result = new HashMap<String, IpSubnet>();
        for (Map.Entry<String, IpSubnet> entry : ipDB.getLinks().entrySet()) {
            IpSubnet subnet = entry.getValue();
            if (!subnet.canApplyChange()) {
                continue;
            } else if (!subnet.isAlive()) {
                continue;
            }
            String key = subnet.getIpSubnetAddressName();
            result.put(key, subnet);
            log.debug("found ip-subnet on NW: [" + key + "] (" + subnet.getNetworkAddress() + ")");
        }
        return result;
    }
}