package voss.nms.inventory.diff.network.diffunitbuilder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.GenericAttributeCommandBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;
import voss.model.CidrAddress;
import voss.model.DefaultLogicalEthernetPort;
import voss.model.Port;
import voss.nms.inventory.builder.IpLinkCommandBuilder;
import voss.nms.inventory.builder.TextBasedIpLinkCommandBuilder;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.*;
import voss.nms.inventory.diff.network.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class IpLinkDiffUnitBuilder extends DiffUnitBuilderImpl<IpSubnet, IpSubnetDto> {
    private final static Logger log = LoggerFactory.getLogger(IpLinkDiffUnitBuilder.class);
    private static final String eventMatter = "Link(L3) policy violation found (diff).";
    private final String editorName;
    @SuppressWarnings("unused")
    private final IpAddressDB ipDB;

    public IpLinkDiffUnitBuilder(DiffSet set, String editorName, IpAddressDB ipDB) throws
            InventoryException, ExternalServiceException, IOException {
        super(set, DiffObjectType.L3_LINK.getCaption(), DiffConstants.linkDepth, editorName);
        this.editorName = editorName;
        this.ipDB = ipDB;
    }

    @Override
    protected DiffUnit create(String id, IpSubnet link) throws IOException, DuplicationException,
            InventoryException, ExternalServiceException {
        if (!link.canApplyChange()) {
            return null;
        }
        TextBasedIpLinkCommandBuilder builder = new TextBasedIpLinkCommandBuilder(this.editorName);
        Iterator<IpAddressHolder> it = link.getMembers().iterator();
        IpAddressHolder port1 = it.next();
        IpAddressHolder port2 = it.next();
        builder.setPort1Name(getIpAbsoluteName(port1), port1.getFullyQualifiedName());
        builder.setPort2Name(getIpAbsoluteName(port2), port2.getFullyQualifiedName());
        builder.setFoundOnNetwork(Boolean.TRUE);
        builder.setSource(DiffCategory.DISCOVERY.name());
        builder.setMaxPorts(2);
        builder.setVersionCheck(false);
        super.applyExtraAttributes(DiffOperationType.ADD, builder, link, null);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return null;
        }
        String linkID = getLinkID(link);
        DiffUnit unit = new DiffUnit(linkID, DiffOperationType.ADD);
        unit.addBuilder(builder);
        return unit;
    }

    @Override
    protected DiffUnit update(String id, IpSubnet link, IpSubnetDto subnet)
            throws IOException, DuplicationException, InventoryException, ExternalServiceException {
        if (!link.canApplyChange()) {
            return null;
        }
        String linkID = getLinkID(link);
        IpLinkCommandBuilder builder = new IpLinkCommandBuilder(subnet, this.editorName);
        builder.setLinkId(linkID);
        builder.setFoundOnNetwork(Boolean.TRUE);
        builder.setVersionCheck(false);
        super.applyExtraAttributes(DiffOperationType.UPDATE, builder, link, subnet);
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return null;
        }
        DiffUnit unit = new DiffUnit(subnet.getSubnetName(), DiffOperationType.UPDATE);
        unit.addBuilder(builder);
        return unit;
    }

    @Override
    protected DiffUnit delete(String id, IpSubnetDto subnet) throws IOException,
            InventoryException, ExternalServiceException, DuplicationException {
        IpLinkCommandBuilder builder = new IpLinkCommandBuilder(subnet, this.editorName);
        builder.setVersionCheck(false);
        super.applyExtraAttributes(DiffOperationType.REMOVE, builder, null, subnet);
        builder.buildDeleteCommand();
        DiffUnit unit = new DiffUnit(subnet.getSubnetName(), DiffOperationType.REMOVE);
        unit.addBuilder(builder);
        return unit;
    }

    @Override
    protected DiffUnit changeDeletedStatus(String id, IpSubnetDto db) throws
            IOException, InventoryException, ExternalServiceException {
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

    private String getIpAbsoluteName(IpAddressHolder ip) {
        if (ip == null) {
            return null;
        }
        log.debug("getIpAbsoluteName: network-fqn=" + ip.getFullyQualifiedName());
        String ipAddress = ip.getIpAddress();
        if (ipAddress == null || !ip.isDuplicated()) {
            log.debug("- getIpAbsoluteName: invalid id: " + ip.toString());
            return null;
        }
        log.debug("- getIpAbsoluteName: found ip=" + ip.getAbsoluteName());
        return ip.getAbsoluteName();
    }

    public static String getLinkID(IpSubnet subnet) {
        List<String> list = new ArrayList<String>();
        for (IpAddressHolder ipHolder : subnet.getMembers()) {
            Port port = ipHolder.getNetworkPort();
            CidrAddress addr = getAddress(port);
            if (addr == null) {
                continue;
            }
            if (port instanceof DefaultLogicalEthernetPort) {
                port = ((DefaultLogicalEthernetPort) port).getPhysicalPort();
            }
            String portID = port.getFullyQualifiedName();
            list.add(portID);
        }
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String getLinkID(IpSubnetDto subnet) {
        List<String> list = new ArrayList<String>();
        for (PortDto member : subnet.getMemberIpifs()) {
            IpIfDto ip = NodeUtil.toIpIfDto(member);
            if (ip == null) {
                continue;
            }
            for (PortDto port : ip.getAssociatedPorts()) {
                String portID = InventoryIdCalculator.getId(port);
                list.add(portID);
            }
        }
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String getLinkKey(IpSubnetDto subnet) throws UnknownHostException {
        String networkAddress = null;
        Set<String> set = new HashSet<String>();
        for (PortDto m : subnet.getMemberIpifs()) {
            IpIfDto member = NodeUtil.toIpIfDto(m);
            if (member == null) {
                continue;
            }
            String ip = NodeUtil.getIpAddress(member);
            String mask = DtoUtil.getStringOrNull(member, MPLSNMS_ATTR.MASK_LENGTH);
            InetAddress addr = InetAddress.getByName(ip);
            int length = Integer.parseInt(mask);
            CidrAddress cidr = new CidrAddress(addr, length);
            networkAddress = cidr.getNetworkAddress().getHostAddress() + "/" + mask;
            set.add(networkAddress);
        }
        if (set.size() != 1) {
            throw new IllegalStateException("broken link: " + subnet.getAbsoluteName() + " " + set);
        }
        return networkAddress;
    }

    public static Map<String, IpSubnetDto> getInventorySubnets(DiffPolicy policy, Set<NodeDto> discoveredNodes,
                                                               List<PolicyViolation> illegalEvents)
            throws IOException, InventoryException, ExternalServiceException {
        Map<String, IpSubnetDto> dbMap = new HashMap<String, IpSubnetDto>();
        Map<String, Object> ipAddressToObject = new HashMap<String, Object>();
        IpSubnetNamespaceDto pool = InventoryConnector.getInstance().getActiveRootIpSubnetNamespace(
                policy.getDefaultIpSubnetPoolName());
        List<IpSubnetDto> subnets = new ArrayList<IpSubnetDto>();
        NEXT_SUBNET:
        for (IpSubnetDto subnet : pool.getUsers()) {
            if (subnet.getMemberIpifs().size() != 2) {
                createIllegalMemberWarning(subnet, illegalEvents, eventMatter);
                continue;
            }
            List<PolicyViolation> violations = checkMember(subnet, ipAddressToObject);
            if (violations.size() > 0) {
                illegalEvents.addAll(violations);
                continue;
            }
            for (PortDto member : subnet.getMemberIpifs()) {
                if (!IpAddressDB.isContained(discoveredNodes, member.getNode())) {
                    log.debug("link found on NOT discovered node: " +
                            "ignoring because non-discovered link must be conitnued.");
                    continue NEXT_SUBNET;
                }
            }
            if (isAnonymousLink(subnet)) {
                log.debug("anonymous-link found: ignoring because anonymous link is out of diff's scope.");
                continue;
            }
            subnets.add(subnet);
        }
        for (IpSubnetDto subnet : subnets) {
            try {
                String id = getLinkKey(subnet);
                dbMap.put(id, subnet);
                log.debug("found link on DB: [" + id + "]");
            } catch (Exception e) {
                log.error("error on link:" + subnet.getAbsoluteName(), e);
            }
        }
        return dbMap;
    }

    private static List<PolicyViolation> checkMember(IpSubnetDto subnet, Map<String, Object> ipAddressToObject) {
        List<PolicyViolation> illegalEvents = new ArrayList<PolicyViolation>();
        for (PortDto m : subnet.getMemberIpifs()) {
            IpIfDto member = NodeUtil.toIpIfDto(m);
            if (member == null) {
                throw new IllegalStateException("non ip-if member on ip-subnet found: " + m.getAbsoluteName());
            }
            if (member.getAssociatedPorts().size() == 0) {
                String msg = "[DB] found ip-if without associated-port : "
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

    private static void createIllegalMemberWarning(IpSubnetDto subnet, List<PolicyViolation> illegalEvents, String eventMatter) {
        for (PortDto member : subnet.getMemberIpifs()) {
            IpIfDto ipIf = NodeUtil.toIpIfDto(member);
            if (ipIf == null) {
                continue;
            }
            if (ipIf.getAssociatedPorts().size() == 0) {
                String msg2 = "[DB] loopback or orphaned ip-if can't be a member of ip-subnet:" + ipIf.getAbsoluteName();
                createPolicyViolation(illegalEvents, ipIf, eventMatter, msg2);
            } else {
                String msg = "[DB] number of ports isn't 2";
                createPolicyViolation(illegalEvents, ipIf, eventMatter, msg);
            }
        }
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
            String msg = "[DB] duplicated ip: " + ip + ", " + ((IpIfDto) previous).getAbsoluteName() + "; "
                    + member.getAbsoluteName();
            createPolicyViolation(illegalEvents, ipIf, eventMatter, msg);
        } else {
            String msg = "[DB] duplicated ip? : " + ip + ", " + previous.getClass().getName()
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
            result.put(entry.getKey(), subnet);
            log.debug("found link on NW: " + subnet.getNetworkAddress() + " " + subnet.toString());
        }
        return result;
    }

    private static CidrAddress getAddress(Port port) {
        Set<CidrAddress> addrs = port.getDevice().getIpAddresses(port);
        if (addrs.size() != 1) {
            log.debug("secondary IP Address found: " + port.getFullyQualifiedName());
            return null;
        }
        CidrAddress addr = addrs.iterator().next();
        return addr;
    }
}