package voss.nms.inventory.diff.network;

import naef.dto.NodeDto;
import naef.dto.ip.IpIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.model.DefaultLogicalEthernetPort;
import voss.model.EthernetPort;
import voss.model.Port;
import voss.nms.inventory.config.DiffConfiguration;
import voss.nms.inventory.constants.IpAddressModel;

import java.util.*;

public class IpAddressDB {
    private static final Logger log = LoggerFactory.getLogger(IpAddressDB.class);
    private final Map<String, IpAddressHolder> holders = new HashMap<String, IpAddressHolder>();
    private final Map<String, IpSubnet> subnets = new HashMap<String, IpSubnet>();
    private final IpAddressModel policy;

    public IpAddressDB() {
        try {
            this.policy = DiffConfiguration.getInstance().getDiffPolicy().getIpAddressModel();
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
        if (this.policy == null) {
            throw new IllegalStateException("No ip-address-model. Check your diff-policy.");
        }
    }

    public void addIpAddress(IpAddressHolder ip) {
        if (ip == null) {
            return;
        }
        this.holders.put(ip.getKey(), ip);
        String networkAddr = ip.getNetworkAddress();
        IpSubnet subnet = this.subnets.get(networkAddr);
        if (subnet == null) {
            log.debug("network found: " + networkAddr);
            subnet = new IpSubnet(networkAddr);
            this.subnets.put(networkAddr, subnet);
        }
        subnet.addMember(ip);
    }

    public Map<String, IpAddressHolder> getIpAddresses() {
        return this.holders;
    }

    public IpAddressHolder getIpAddress(String vpnPrefix, String ip) {
        return this.holders.get(IpAddressHolder.getKey(vpnPrefix, ip));
    }

    public Map<String, IpSubnet> getLinks() {
        return this.subnets;
    }

    public IpSubnet getLinkOn(String ipAddress) {
        IpAddressHolder ip = this.holders.get(ipAddress);
        if (ip == null) {
            return null;
        }
        String networkAddr = ip.getNetworkAddress();
        return getLink(networkAddr);
    }

    public IpSubnet getLink(String networkAddr) {
        return this.subnets.get(networkAddr);
    }

    public IpIfDto getDbPort(Port port) {
        for (IpAddressHolder holder : this.holders.values()) {
            if (!holder.isDuplicated()) {
                continue;
            }
            Port networkPort = holder.getNetworkPort();
            if (port == networkPort) {
                return holder.getDbPort();
            }
            if (port instanceof DefaultLogicalEthernetPort) {
                EthernetPort eth = ((DefaultLogicalEthernetPort) port).getPhysicalPort();
                if (eth == networkPort) {
                    return holder.getDbPort();
                }
            }
        }
        return null;
    }

    public List<PolicyViolation> checkViolation() {
        switch (this.policy) {
            case IP_IF:
                //FIXME:There is no check for IP_IF.
                return new ArrayList<PolicyViolation>();
            case P2P:
                return checkViolationForIpLink();
            case SUBNET:
                return checkViolationForSubnet();
            case NONE:
                return new ArrayList<PolicyViolation>();
            default:
                throw new IllegalStateException("unexpected ip-address-model: " + this.policy.name());
        }
    }

    protected List<PolicyViolation> checkViolationForIpLink() {
        List<PolicyViolation> violations = new ArrayList<PolicyViolation>();
        for (IpAddressHolder ip : this.holders.values()) {
            if (!ip.canApplyChange()) {
                continue;
            }
            if (!ip.isDuplicated()) {
                ip.setApplyChange(false);
                String msg = "duplicated ip: " + ip.getIpAddress();
                PolicyViolation pv = createPolicyViolation(ip, msg, ip.toString());
                violations.add(pv);
            }
        }
        for (IpSubnet subnet : this.subnets.values()) {
            if (!subnet.canApplyChange()) {
                continue;
            } else if (subnet.getMembers().size() > 2) {
                subnet.setApplyChange(false);
                String msg = "invalid subnet members: " + subnet.getMembers().size();
                violations.addAll(createPolicyViolations(subnet, msg));
            } else if (subnet.getMembers().size() == 1) {
                subnet.setApplyChange(false);
                String msg = "invalid subnet members: " + subnet.getNetworkAddress() + " -> " + subnet.getMembers().size();
                log.warn(msg);
            } else if (!subnet.isTypeMatched()) {
                subnet.setApplyChange(false);
                String msg = "port type mismatch: " + subnet.getNetworkAddress() + " -> " + subnet.toString();
                violations.addAll(createPolicyViolations(subnet, msg));
            }
        }
        return violations;
    }

    protected List<PolicyViolation> checkViolationForSubnet() {
        List<PolicyViolation> violations = new ArrayList<PolicyViolation>();
        for (IpAddressHolder ip : this.holders.values()) {
            if (!ip.canApplyChange()) {
                continue;
            }
            if (!ip.isDuplicated()) {
                log.debug("(WARN)found duplicated ip: " + ip.getIpAddress());
            }
        }
        return violations;
    }

    private PolicyViolation createPolicyViolation(IpAddressHolder ip, String matter, String event) {
        String id = ip.getInventoryID();
        String nodeName = ip.getNodeName();
        PolicyViolation pv = new PolicyViolation(id, nodeName, ip.getIpAddress(), matter, event);
        log.warn(pv.toString());
        return pv;
    }

    private List<PolicyViolation> createPolicyViolations(IpSubnet subnet, String matter) {
        List<PolicyViolation> violations = new ArrayList<PolicyViolation>();
        for (IpAddressHolder member : subnet.getMembers()) {
            String id = member.getInventoryID();
            String nodeName = member.getNodeName();
            PolicyViolation pv = new PolicyViolation(id, nodeName, member.getIpAddress(), matter, member.toString());
            violations.add(pv);
            log.warn(pv.toString());
        }
        return violations;
    }

    public void dumpLink() {
        for (Map.Entry<String, IpSubnet> entry : this.subnets.entrySet()) {
            log.debug(entry.getValue().toString());
        }
    }

    public void setDiscoveredNodes(Set<NodeDto> discoveredNodes) {
        LOOP_OUTER:
        for (Map.Entry<String, IpSubnet> entry : this.subnets.entrySet()) {
            IpSubnet subnet = entry.getValue();
            for (IpAddressHolder ip : subnet.getMembers()) {
                try {
                    IpIfDto port = ip.getDbPort();
                    if (port == null) {
                        continue;
                    } else if (!isContained(discoveredNodes, port.getNode())) {
                        ip.setApplyChange(false);
                        subnet.setApplyChange(false);
                        break LOOP_OUTER;
                    }
                } catch (IllegalStateException e) {
                    log.warn("duplicated ip-address on inventory found. ignored.", e);
                    ip.setApplyChange(false);
                    subnet.setApplyChange(false);
                    break LOOP_OUTER;
                }
            }
        }
    }

    public static boolean isContained(Set<NodeDto> nodes, NodeDto node) {
        for (NodeDto n : nodes) {
            if (DtoUtil.mvoEquals(node, n)) {
                return true;
            }
        }
        return false;
    }
}