package voss.nms.inventory.builder;

import naef.dto.*;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanLinkDto;
import naef.dto.vlan.VlanSegmentDto;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.InventoryBuilder.IdResolver;
import voss.core.server.builder.InventoryBuilder.PoolResolver;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.core.server.util.VlanUtil;
import voss.nms.inventory.util.LinkUtil;

import java.rmi.RemoteException;
import java.util.*;

public class VlanBuilderUtil {
    private static final Logger log = LoggerFactory.getLogger(VlanBuilderUtil.class);

    public static void bindVlanLinkToVlan(ShellCommands cmd, VlanDto vlan, VlanSegmentDto vlink) {
        if (vlink == null) {
            throw new IllegalArgumentException("vlan-link is null.");
        }
        if (vlink.getMemberPorts().size() != 2) {
            throw new IllegalArgumentException("vlan-link's member isn't 2! (" + DtoUtil.toDebugString(vlink) + ")");
        }
        Iterator<PortDto> it = vlink.getMemberPorts().iterator();
        VlanIfDto vif1 = (VlanIfDto) it.next();
        VlanIfDto vif2 = (VlanIfDto) it.next();
        bindVlanLinkToVlan(cmd, vlan, vif1, vif2);
    }

    public static void bindVlanLinkToVlan(ShellCommands cmd, VlanDto vlan, VlanIfDto vif1, VlanIfDto vif2) {
        bindVlanLinkToVlan(cmd, vlan, vif1.getAbsoluteName(), vif2.getAbsoluteName());
    }

    public static void bindVlanLinkToVlan(ShellCommands cmd, VlanDto vlan, String vifName1, String vifName2) {
        InventoryBuilder.changeContext(cmd, vlan);
        InventoryBuilder.translate(cmd, CMD.INCLUDE_ELEMENT_TO_NETWORK,
                CMD.ARG_FQN, InventoryBuilder.getLinkAbsoluteName(ATTR.TYPE_VLAN_LINK, vifName1, vifName2));
        cmd.addLastEditCommands();
        cmd.addVersionCheckTarget(vlan);
    }

    public static void createLink(ShellCommands cmd, VlanIfDto vif1, VlanIfDto vif2) {
        createLink(cmd, vif1.getAbsoluteName(), vif2.getAbsoluteName());
    }

    protected static void createLink(ShellCommands cmd, String vif1AbsoluteName, String vif2AbsoluteName) {
        List<String> names = Util.sortNames(vif1AbsoluteName, vif2AbsoluteName);
        String name1 = names.get(0);
        String name2 = names.get(1);
        cmd.log("createLink");
        InventoryBuilder.translate(cmd, CMD.LINK_CONNECT, CMD.ARG_TYPE, ATTR.TYPE_VLAN_LINK,
                CMD.ARG_FQN1, name1, CMD.ARG_FQN2, name2);
    }

    public static void stackLowerLayerLink(ShellCommands cmd, NetworkDto lower) {
        if (lower == null) {
            return;
        }
        if (lower.getMemberPorts().size() != 2) {
            log.debug("illegal member ports: " + lower.getAbsoluteName());
            return;
        }
        Iterator<PortDto> members = lower.getMemberPorts().iterator();
        PortDto p1 = members.next();
        PortDto p2 = members.next();
        stackLowerLayerLink(cmd, p1, p2);
    }

    public static void stackLowerLayerLink(ShellCommands cmd, PortDto port1, PortDto port2) {
        InventoryBuilder.translate(cmd, CMD.STACK_LOWER_NETWORK,
                CMD.ARG_LOWER, InventoryBuilder.getLinkAbsoluteName(LinkUtil.getL2LinkTypeName(port1), port1, port2));
    }

    public static void buildVlanLinkCreationCommand(ShellCommands cmd, VlanDto vlan, Integer vlanID, PortDto port1, PortDto port2) {
        cmd.log("buildVlanLinkCreationCommand");
        String vif1 = getVlanIfAbsoluteName(vlan, port1);
        String vif2 = getVlanIfAbsoluteName(vlan, port2);
        String vlanLinkAbsoluteName = InventoryBuilder.getLinkAbsoluteName(ATTR.TYPE_VLAN_LINK, vif1, vif2);
        createLink(cmd, vif1, vif2);
        InventoryBuilder.changeContext(cmd, vlanLinkAbsoluteName);
        stackLowerLayerLink(cmd, port1, port2);

        InventoryBuilder.changeContext(cmd, vlan);
        InventoryBuilder.translate(cmd, CMD.INCLUDE_ELEMENT_TO_NETWORK, "_FQN1_", vif1, "_FQN2_", vif2);
        cmd.addLastEditCommands();
        cmd.addVersionCheckTarget(vlan);

        InventoryBuilder.translate(cmd, CMD.PORT_STACK, CMD.ARG_LOWER, port1.getAbsoluteName(), CMD.ARG_UPPER, vif1);
        InventoryBuilder.translate(cmd, CMD.PORT_STACK, CMD.ARG_LOWER, port2.getAbsoluteName(), CMD.ARG_UPPER, vif2);
    }

    public static String getVlanIfAbsoluteName(VlanDto vlan, PortDto port) {
        if (!VlanUtil.isSwitchPort(port) && !VlanUtil.isRouterPort(port)) {
            throw new IllegalStateException("Port mode is not one of Switch / Router.");
        }
        VlanIfDto vif = VlanUtil.getVlanIf(vlan, port);
        if (vif == null) {
            NodeElementDto owner = VlanUtil.getVlanOwner(port);
            String ifName = VlanUtil.getVlanName(vlan);
            return InventoryBuilder.appendContext(owner, ATTR.TYPE_VLAN_IF, ifName);
        } else {
            return vif.getAbsoluteName();
        }
    }

    public static void removeLink(ShellCommands cmd, VlanDto vlan, PortDto port1, PortDto port2) {
        VlanIfDto vlanIf1 = VlanUtil.getVlanIf(vlan, port1);
        VlanIfDto vlanIf2 = VlanUtil.getVlanIf(vlan, port2);
        removeLink(cmd, vlanIf1, vlanIf2);

        cmd.log("buildVlanLinkDeletionCommand2");
        cmd.addCommand(InventoryBuilder.translate(CMD.PORT_UNSTACK, CMD.ARG_LOWER,
                port1.getAbsoluteName(), CMD.ARG_UPPER, vlanIf1.getAbsoluteName()));
        cmd.addCommand(InventoryBuilder.translate(CMD.PORT_UNSTACK, CMD.ARG_LOWER,
                port2.getAbsoluteName(), CMD.ARG_UPPER, vlanIf2.getAbsoluteName()));
    }


    public static void removeLink(ShellCommands cmd, VlanIfDto vif1, VlanIfDto vif2) {
        log.debug("buildVlanLinkDeletionCommand: " + vif1.getAbsoluteName() + " - " + vif2.getAbsoluteName());
        VlanLinkDto vlanLink = LinkUtil.getVlanLink(vif1, vif2);
        if (vlanLink == null) {
            log.debug("- no vlan-link found: " + vif1.getAbsoluteName() + " - " + vif2.getAbsoluteName());
            return;
        }
        removeLink(cmd, vif1, vif2, vlanLink);
    }

    public static void removeLink(ShellCommands cmd, VlanSegmentDto link) {
        if (link.getMemberPorts().size() == 1) {
            removeDemarcationLink(cmd, link);
        }
        if (link.getMemberPorts().size() != 2) {
            throw new IllegalStateException("illegal members[" + link.getMemberPorts().size()
                    + "] on link [" + link.getAbsoluteName() + "]");
        }
        Iterator<PortDto> it1 = link.getMemberPorts().iterator();
        VlanIfDto vif1 = (VlanIfDto) it1.next();
        VlanIfDto vif2 = (VlanIfDto) it1.next();
        removeLink(cmd, vif1, vif2, link);
    }

    public static void removeLink(ShellCommands cmd, VlanIfDto vif1, VlanIfDto vif2, VlanSegmentDto link) {
        detachVlanLinkFromVlan(cmd, vif1, vif2);
        disconnectVlanLink(cmd, vif1, vif2, link);
    }

    public static void detachVlanLinkFromVlan(ShellCommands cmd, VlanIfDto vif1, VlanIfDto vif2) {
        VlanSegmentDto vlanLink = VlanUtil.getVlanLinkBetween(vif1, vif2);
        if (vlanLink == null) {
            return;
        }
        VlanDto vlan = (VlanDto) vlanLink.getOwner();
        if (vlan == null) {
            return;
        }
        InventoryBuilder.changeContext(cmd, vlan);
        InventoryBuilder.translate(cmd, CMD.EXCLUDE_ELEMENT_FROM_NETWORK,
                CMD.ARG_FQN, InventoryBuilder.getMvoContext(vlanLink));
        cmd.addLastEditCommands();
        cmd.addVersionCheckTarget(vlan);
    }

    public static void disconnectVlanLink(ShellCommands cmd, VlanIfDto vif1, VlanIfDto vif2, VlanSegmentDto link) {
        InventoryBuilder.changeContext(cmd, InventoryBuilder.getMvoContext(link));
        for (NetworkDto lower : link.getLowerLayerLinks()) {
            if (!LinkDto.class.isInstance(lower)) {
                log.debug("- unexpected lower-layer-link found: '" + DtoUtil.toDebugString(lower)
                        + "' in '" + DtoUtil.toDebugString(link) + "'");
                continue;
            } else if (lower.getMemberPorts().size() > 2) {
                throw new IllegalStateException("not p2p link: " + DtoUtil.toDebugString(link));
            }
            InventoryBuilder.translate(cmd, CMD.UNSTACK_LOWER_NETWORK,
                    CMD.ARG_LOWER, InventoryBuilder.getMvoContext(lower));
        }

        cmd.addCommand(CMD.CONTEXT_RESET);
        InventoryBuilder.translate(cmd, CMD.LINK_DISCONNECT_BY_MVOID,
                CMD.ARG_MVOID, DtoUtil.getMvoId(link).toString());
    }


    public static void removeDemarcationLink(ShellCommands cmd, VlanIfDto vif, PortDto tagged)
            throws RemoteException {
        NaefDtoFacade facade = DtoUtil.getNaefDtoFacade(vif);
        VlanSegmentDto vlanLink = facade.getVlanLink(vif.getTrafficDomain(), tagged);
        if (vlanLink == null) {
            return;
        }
        removeDemarcationLink(cmd, vif, tagged, vlanLink);
    }

    public static void removeDemarcationLink(ShellCommands cmd, VlanSegmentDto vlanLink) {
        if (vlanLink == null) {
            return;
        } else if (!vlanLink.isDemarcationLink()) {
            throw new IllegalArgumentException("vlan-link is not demarcation link: " + vlanLink.getAbsoluteName());
        }
        VlanIfDto vif = (VlanIfDto) vlanLink.getMemberPorts().iterator().next();
        NodeDto node = vif.getNode();
        Set<NetworkDto> lowerLayerNetworks = vlanLink.getLowerLayerLinks();
        if (lowerLayerNetworks == null || lowerLayerNetworks.size() == 0) {
            throw new IllegalStateException("Lower layer of vlan-demarcation-link not found. " +
                    "The data may be corrupted: " + vlanLink.getAbsoluteName()
                    + "(" + DtoUtil.getMvoId(vlanLink).toString() + ")");
        } else {
            NetworkDto lowerLayerNetwork = lowerLayerNetworks.iterator().next();
            PortDto here = null;
            for (PortDto member : lowerLayerNetwork.getMemberPorts()) {
                if (DtoUtil.mvoEquals(node, member.getNode())) {
                    if (here != null) {
                        throw new IllegalStateException("It is not possible to handle vlan-demarcatoin-link on return link.");
                    }
                    here = member;
                }
            }
            if (here == null) {
                throw new IllegalStateException("The endpoint port of vlan-demarcation-link is not found on this node as same as a vlan-if." +
                        "The data may be corrupted: " + vlanLink.getAbsoluteName()
                        + "(" + DtoUtil.getMvoId(vlanLink).toString() + ")");
            }
            removeDemarcationLink(cmd, vif, here, vlanLink);
        }
    }

    public static void removeDemarcationLink(ShellCommands cmd, VlanIfDto vif, PortDto tagged,
                                             VlanSegmentDto vlanLink) {
        if (vlanLink == null) {
            return;
        }
        String varName = "DEMARC" + new Object().hashCode();
        InventoryBuilder.changeContext(cmd, vlanLink);
        InventoryBuilder.navigate(cmd, vif.getAbsoluteName());
        InventoryBuilder.assignVar(cmd, varName);
        InventoryBuilder.changeContext(cmd, vif.getTrafficDomain());
        cmd.addCommand(InventoryBuilder.translate(CMD.EXCLUDE_ELEMENT_FROM_NETWORK,
                CMD.ARG_FQN, "$;" + varName));
        InventoryBuilder.changeContext(cmd, vif);
        cmd.addCommand(InventoryBuilder.translate(CMD.DELETE_TRUNK_ALONE, "_IFNAME_", tagged.getNodeLocalName()));
    }

    public static IdResolver<VlanDto> getVlanIdResolver() {
        return new IdResolver<VlanDto>() {
            @Override
            public String getId(VlanDto id) {
                return id.getVlanId().toString();
            }

            @Override
            public String getIdTye() {
                return ATTR.ATTR_VLAN_ID;
            }

            @Override
            public String getPoolType() {
                return ATTR.ATTR_VLAN_POOL;
            }
        };
    }

    public static PoolResolver<VlanDto> getVlanPoolResolver() {
        return new PoolResolver<VlanDto>() {
            private final Map<String, Integer> lowerMap = new HashMap<String, Integer>();
            private final Map<String, Integer> upperMap = new HashMap<String, Integer>();

            @Override
            public boolean isInRange(VlanDto vlan, String range) {
                int id = vlan.getVlanId().intValue();
                int lower = -1;
                int upper = -1;
                String[] arr = range.split("-");
                if (lowerMap.get(range) != null) {
                    lower = lowerMap.get(range).intValue();
                } else {
                    lower = Integer.parseInt(arr[0]);
                    lowerMap.put(range, Integer.valueOf(lower));
                }
                if (upperMap.get(range) != null) {
                    upper = upperMap.get(range).intValue();
                } else {
                    upper = Integer.parseInt(arr[1]);
                    upperMap.put(range, Integer.valueOf(upper));
                }
                return lower <= id && id <= upper;
            }

        };
    }

}