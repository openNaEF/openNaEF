package voss.multilayernms.inventory.builder;

import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vlan.VlanLinkDto;
import naef.dto.vlan.VlanSegmentDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vrf.VrfIfDto;
import voss.core.server.builder.CMD;
import voss.core.server.builder.Commands;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.VlanUtil;
import voss.nms.inventory.builder.VlanIfCommandBuilder;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.LinkUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VlanBuilder extends InventoryBuilder {

    public static void crateVlanIf(String nodeName, String vlanIfName, int vlanId, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, ATTR.TYPE_NODE, nodeName);
        translate(commands, CMD.NEW_PORT, CMD.ARG_TYPE, ATTR.TYPE_VLAN_IF, CMD.ARG_NAME, vlanIfName);
        translate(commands, CMD.ATTRIBUTE_SET, CMD.ARG_ATTR, ATTR.ATTR_VLAN_IF_ID, CMD.ARG_VALUE, String.valueOf(vlanId));
        translate(commands, CMD.ATTRIBUTE_SET, CMD.ARG_ATTR, ATTR.IFNAME, CMD.ARG_VALUE, vlanIfName);
        ShellConnector.getInstance().execute2(commands);

    }

    public static void createVlanIf(PortDto port, String vlanIfName, int vlanId, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, port);
        translate(commands, CMD.NEW_PORT, CMD.ARG_TYPE, ATTR.TYPE_VLAN_IF, CMD.ARG_NAME, vlanIfName);
        translate(commands, CMD.ATTRIBUTE_SET, CMD.ARG_ATTR, ATTR.ATTR_VLAN_IF_ID, CMD.ARG_VALUE, String.valueOf(vlanId));
        translate(commands, CMD.ATTRIBUTE_SET, CMD.ARG_ATTR, ATTR.IFNAME, CMD.ARG_VALUE, vlanIfName);
        ShellConnector.getInstance().execute2(commands);

    }

    public static void removeVlanIf(String nodeName, String vlanIfName, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, ATTR.TYPE_NODE, nodeName);
        translate(commands, CMD.REMOVE_ELEMENT, "_TYPE_", ATTR.TYPE_VLAN_IF, "_NAME_", vlanIfName);
        ShellConnector.getInstance().execute2(commands);
    }

    public static void removeVlanIf(PortDto port, String vlanIfName, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, port);
        translate(commands, CMD.REMOVE_ELEMENT, "_TYPE_", ATTR.TYPE_VLAN_IF, "_NAME_", vlanIfName);
        ShellConnector.getInstance().execute2(commands);
    }

    public static void setFacilityStatus(VlanIfDto vlanIf, String facilityStatus, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, vlanIf);
        translateAttribute(commands, MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus);
        ShellConnector.getInstance().execute2(commands);
    }

    public static void createVlanLink(PortDto port1, PortDto port2, VlanIfDto vlanIf1, VlanIfDto vlanIf2, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        translate(commands, CMD.LINK_CONNECT,
                CMD.ARG_TYPE, LinkUtil.getL2LinkTypeName(port1),
                CMD.ARG_FQN1, vlanIf1.getAbsoluteName(),
                CMD.ARG_FQN2, vlanIf2.getAbsoluteName());
        if (port1 instanceof EthPortDto) {
            translate(commands, CMD.STACK_LOWER_NETWORK,
                    InventoryBuilder.getLinkAbsoluteName(ATTR.TYPE_ETH_LINK, port1, port2));
        } else if (port1 instanceof EthLagIfDto) {
            translate(commands, CMD.STACK_LOWER_NETWORK,
                    InventoryBuilder.getLinkAbsoluteName(ATTR.TYPE_LAG_LINK, port1, port2));
        } else {
            throw new InventoryException("Unknown lower layer link.");
        }
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static ShellCommands getCreateVlanLinkCommands(PortDto port1, PortDto port2, String vlanIfName1, String vlanIfName2, NodeElementDto parent1, NodeElementDto parent2, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        translate(commands, CMD.LINK_CONNECT,
                CMD.ARG_TYPE, LinkUtil.getL2LinkTypeName(port1),
                CMD.ARG_FQN1, vlanIfName1,
                CMD.ARG_FQN2, vlanIfName2);
        if (port1 instanceof EthPortDto) {
            translate(commands, CMD.STACK_LOWER_NETWORK,
                    InventoryBuilder.getLinkAbsoluteName(ATTR.TYPE_ETH_LINK, port1, port2));
        } else if (port1 instanceof EthLagIfDto) {
            translate(commands, CMD.STACK_LOWER_NETWORK,
                    InventoryBuilder.getLinkAbsoluteName(ATTR.TYPE_LAG_LINK, port1, port2));
        } else {
            throw new InventoryException("Unknown lower layer link.");
        }
        commands.addLastEditCommands();
        return commands;
    }

    public static void removeVlanLink(PortDto port1, PortDto port2, VlanIfDto vlanIf1, VlanIfDto vlanIf2, String editorName) throws InventoryException {
        VlanSegmentDto vlanLink = VlanUtil.getVlanLinkBetween(vlanIf1, vlanIf2);
        if (vlanLink == null) {
            return;
        }
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, vlanLink);
        for (NetworkDto lower : vlanLink.getLowerLayerLinks()) {
            translate(commands, CMD.UNSTACK_LOWER_NETWORK, CMD.ARG_LOWER, getMvoContext(lower));
        }
        translate(commands, CMD.LINK_DISCONNECT_BY_MVOID, CMD.ARG_MVOID, DtoUtil.getMvoId(vlanLink).toString());
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void removeVlanLinkAndVlanIf(PortDto port1, PortDto port2,
                                               VlanIfDto vlanIf1, VlanIfDto vlanIf2, String editorName)
            throws InventoryException, IOException, ExternalServiceException {
        List<Commands> commandsList = new ArrayList<Commands>();
        ShellCommands commands = new ShellCommands(editorName);
        VlanSegmentDto vlanLink = VlanUtil.getVlanLinkBetween(vlanIf1, vlanIf2);
        if (vlanLink == null) {
            return;
        }
        changeContext(commands, vlanLink);
        for (NetworkDto lower : vlanLink.getLowerLayerLinks()) {
            translate(commands, CMD.UNSTACK_LOWER_NETWORK, CMD.ARG_LOWER, InventoryBuilder.getMvoContext(lower));
        }
        translate(commands, CMD.LINK_DISCONNECT_BY_MVOID, CMD.ARG_MVOID, DtoUtil.getMvoId(vlanLink).toString());
        commands.addLastEditCommands();
        commandsList.add(commands);

        if (vlanIf1.getOwner() instanceof EthPortDto ||
                vlanIf1.getOwner() instanceof EthLagIfDto ||
                (vlanLink instanceof VlanLinkDto && !hasOtherAssociatedObjects(vlanIf1.getNode(), vlanIf1, (VlanLinkDto) vlanLink))) {
            VlanIfCommandBuilder vlanIfCommandBuilder = new VlanIfCommandBuilder(port1, vlanIf1, editorName);
            Set<PortDto> ports = vlanIf1.getUntaggedVlans();
            if (ports != null && ports.size() == 1) {
                PortDto port = ports.iterator().next();
                if (port instanceof VrfIfDto) {
                    vlanIfCommandBuilder.setVpnOwner((VrfIfDto) port);
                } else if (port instanceof VplsIfDto) {
                    commands.addCommand(CMD.CONTEXT_RESET);
                    translate(commands, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                            CMD.ARG_INSTANCE, port.getAbsoluteName(),
                            CMD.ARG_PORT, vlanIf1.getAbsoluteName());
                }
            }
            vlanIfCommandBuilder.setPreCheckEnable(false);
            vlanIfCommandBuilder.setKeepBinding(true);
            vlanIfCommandBuilder.buildDeleteCommand();
            commandsList.add(vlanIfCommandBuilder.getCommand());
        }
        if (vlanIf2.getOwner() instanceof EthPortDto ||
                vlanIf2.getOwner() instanceof EthLagIfDto ||
                (vlanLink instanceof VlanLinkDto && !hasOtherAssociatedObjects(vlanIf2.getNode(), vlanIf2, (VlanLinkDto) vlanLink))) {
            VlanIfCommandBuilder vlanIfCommandBuilder = new VlanIfCommandBuilder(port2, vlanIf2, editorName);
            Set<PortDto> ports = vlanIf2.getUntaggedVlans();
            if (ports != null && ports.size() == 1) {
                PortDto port = ports.iterator().next();
                if (port instanceof VrfIfDto) {
                    vlanIfCommandBuilder.setVpnOwner((VrfIfDto) port);
                } else if (port instanceof VplsIfDto) {
                    commands.addCommand(CMD.CONTEXT_RESET);
                    translate(commands, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                            CMD.ARG_INSTANCE, port.getAbsoluteName(),
                            CMD.ARG_PORT, vlanIf2.getAbsoluteName());
                }
            }
            vlanIfCommandBuilder.setPreCheckEnable(false);
            vlanIfCommandBuilder.setKeepBinding(true);
            vlanIfCommandBuilder.buildDeleteCommand();
            commandsList.add(vlanIfCommandBuilder.getCommand());
        }

        ShellConnector.getInstance().executes2(commandsList);
    }

    private static boolean hasOtherAssociatedObjects(NodeDto targetNode, VlanIfDto targetVlanIf, VlanLinkDto excludeVlanLink) {
        if (targetVlanIf.getUntaggedVlans().size() != 0 ||
                targetVlanIf.getTaggedVlans().size() != 0 ||
                targetVlanIf.getVlanSegmentGatewayIfs().size() != 0) return true;
        Set<VlanSegmentDto> associatedVlanLinks = targetVlanIf.getVlanLinks();
        if (associatedVlanLinks.size() == 1 &&
                DtoUtil.mvoEquals(associatedVlanLinks.iterator().next(), excludeVlanLink)) {
            return false;
        }
        return true;
    }

}