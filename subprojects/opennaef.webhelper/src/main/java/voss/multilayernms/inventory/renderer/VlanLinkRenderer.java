package voss.multilayernms.inventory.renderer;

import naef.dto.LinkDto;
import naef.dto.NetworkDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanLinkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.util.PortIfNameComparator;
import voss.nms.inventory.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VlanLinkRenderer extends GenericRenderer {
    private static final Logger log = LoggerFactory.getLogger(VlanLinkRenderer.class);

    public static String getName(VlanLinkDto link) {
        if (link == null) {
            return null;
        }
        PortDto port1 = getPort1(link);
        PortDto port2 = getPort2(link);
        StringBuilder sb = new StringBuilder();
        sb.append("VLAN link: ").append(PortRenderer.getNodeIfName(port1)).append(" ~ ").append(PortRenderer.getNodeIfName(port2));
        return sb.toString();
    }

    public static PortDto getPort1(VlanLinkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + link.getMemberPorts().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(link.getMemberPorts());
        Collections.sort(members, new PortIfNameComparator());
        return NodeUtil.getAssociatedPort(members.get(0));
    }

    public static PortDto getPort2(VlanLinkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + link.getMemberPorts().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(link.getMemberPorts());
        Collections.sort(members, new PortIfNameComparator());
        return NodeUtil.getAssociatedPort(members.get(1));
    }

    public static PortDto getPhysicalPort1(VlanLinkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + link.getMemberPorts().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(link.getMemberPorts());
        Collections.sort(members, new PortIfNameComparator());
        PortDto vif1 = NodeUtil.getAssociatedPort(members.get(0));
        PortDto p = null;
        if (link.getLowerLayerLinks().size() == 1) {
            for (NetworkDto plink : link.getLowerLayerLinks()) {
                for (PortDto physicalp : plink.getMemberPorts()) {
                    if (DtoUtil.mvoEquals(physicalp.getNode(), vif1.getNode())) {
                        p = physicalp;
                    }
                }
            }
        }
        return p;
    }

    public static PortDto getPhysicalPort2(VlanLinkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + link.getMemberPorts().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(link.getMemberPorts());
        Collections.sort(members, new PortIfNameComparator());
        PortDto vif2 = NodeUtil.getAssociatedPort(members.get(1));
        PortDto p = null;
        if (link.getLowerLayerLinks().size() == 1) {
            for (NetworkDto plink : link.getLowerLayerLinks()) {
                for (PortDto physicalp : plink.getMemberPorts()) {
                    if (DtoUtil.mvoEquals(physicalp.getNode(), vif2.getNode())) {
                        p = physicalp;
                    }
                }
            }
        }
        return p;
    }

    public static PortDto getPhysicalPort1(LinkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + link.getMemberPorts().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(link.getMemberPorts());
        Collections.sort(members, new PortIfNameComparator());
        return NodeUtil.getAssociatedPort(members.get(0));
    }

    public static PortDto getPhysicalPort2(LinkDto link) {
        if (link == null) {
            return null;
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("member port is not 2: " + link.getMemberPorts().size());
        }
        List<PortDto> members = new ArrayList<PortDto>(link.getMemberPorts());
        Collections.sort(members, new PortIfNameComparator());
        return NodeUtil.getAssociatedPort(members.get(1));
    }

}