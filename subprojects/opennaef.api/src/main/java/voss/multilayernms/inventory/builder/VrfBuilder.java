package voss.multilayernms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import naef.dto.vrf.VrfIntegerIdPoolDto;
import naef.dto.vrf.VrfStringIdPoolDto;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.VrfUtil;

import java.util.*;

public class VrfBuilder extends InventoryBuilder {

    public static void createVrfPool(String name, String parent, String range,
                                     Map<String, String> attributes, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        buildVrfIdPoolCreationCommands(commands, name, parent, range, attributes);
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildVrfIdPoolCreationCommands(ShellCommands commands, String name,
                                                      String parent, String range, Map<String, String> attributes) {
        changeContext(commands, ATTR.POOL_TYPE_VRF_STRING_TYPE, "Nationwide");
        changeContext(commands, parent);
        translate(commands, CMD.POOL_CREATE, CMD.POOL_CREATE_ARG1, ATTR.POOL_TYPE_VRF_STRING_TYPE,
                CMD.POOL_CREATE_ARG2, name);
        translate(commands, CMD.POOL_RANGE_ALLOCATE, "_RANGE_", range);
        buildAttributeSetOnCurrentContextCommands(commands, attributes);
    }

    public static void createVrf(VrfIntegerIdPoolDto pool, Integer id, String editorName) throws
            InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildNetworkIDCreationCommand(commands, ATTR.NETWORK_TYPE_VRF,
                ATTR.ATTR_VRF_ID_INTEGER, id.toString(),
                ATTR.ATTR_VRF_POOL_INTEGER, pool.getAbsoluteName());
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void changeVrfRange(VrfIntegerIdPoolDto pool, String newRange, String editorName) throws
            InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildChangeRangeCommand(commands, pool.getConcatenatedIdRangesStr(), newRange, false);
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildVrfRangeChangeCommands(ShellCommands commands, VrfIntegerIdPoolDto pool, String newRange)
            throws InventoryException {
        buildChangeRangeCommand(commands, pool.getConcatenatedIdRangesStr(), newRange, false);
    }

    public static void buildVrfAttachedPortsResetCommands(ShellCommands commands, VrfDto vrf)
            throws InventoryException {
        for (VrfIfDto vrfIf : vrf.getMemberVrfifs()) {
            buildVrfIfUpdateCommands(commands, vrf, vrfIf.getNode(), new ArrayList<PortDto>());
        }
    }

    public static void updateVrfIfBoundPort(VrfDto vrf, NodeDto node, List<PortDto> selected, String editorName)
            throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        buildVrfIfUpdateCommands(commands, vrf, node, selected);
        ShellConnector.getInstance().execute2(commands);
        vrf.renew();
    }

    public static void buildVrfIfUpdateCommands(ShellCommands commands, VrfDto vrf, NodeDto node, List<PortDto> selected)
            throws InventoryException {
        if (vrf == null) {
            throw new IllegalArgumentException("vpls is null.");
        } else if (node == null) {
            throw new IllegalArgumentException("node is null.");
        }
        String vrfName = VrfUtil.getVrfIfName(vrf);
        VrfIfDto vrfIf = VrfUtil.getVrfIf(node, vrf);
        if (selected.size() > 0) {
            if (vrfIf == null) {
                vrfIf = VrfUtil.getOrphanedVrfIf(node, vrf);
                if (vrfIf == null) {
                    changeContext(commands, node);
                    translate(commands, CMD.CREATE_VRFIF, "_VRFIFNAME_", VrfUtil.getVrfIfName(vrf));
                    translateAttribute(commands, "VRF ID", vrf.getStringId());
                    commands.addLastEditCommands();
                    changeContext(commands, vrf);
                    translate(commands, CMD.BIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, VrfUtil.getVrfIfFqn(node, vrf));
                    commands.addLastEditCommands();
                } else {
                    List<VrfIfDto> list = new ArrayList<VrfIfDto>(vrf.getMemberVrfifs());
                    list.add(vrfIf);
                    changeContext(commands, vrf);
                    buildVrfLinkUpdateCommands(commands, vrf, list);
                }
            }
        }
        changeContext(commands, node);
        Set<PortDto> attachedPorts = new HashSet<PortDto>();
        if (vrfIf != null) {
            for (PortDto port : vrfIf.getAttachedPorts()) {
                attachedPorts.add(port);
            }
        }
        List<PortDto> addedPorts = Util.getAddedList(attachedPorts, selected);
        List<PortDto> removedPorts = Util.getRemovedList(attachedPorts, selected);
        for (PortDto added : addedPorts) {
            translate(commands, CMD.CONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, vrfName,
                    CMD.ARG_PORT, added.getNodeLocalName());
        }
        for (PortDto removed : removedPorts) {
            translate(commands, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, vrfName,
                    CMD.ARG_PORT, removed.getNodeLocalName());
        }
        if (selected.size() == 0 && vrfIf != null) {
            List<VrfIfDto> list = new ArrayList<VrfIfDto>(vrf.getMemberVrfifs());
            list.remove(vrfIf);
            changeContext(commands, vrf);
            buildVrfLinkUpdateCommands(commands, vrf, list);
            changeContext(commands, vrfIf.getNode());
            buildVrfIfRemoveCommand(commands, vrfIf);
        }
    }

    public static void updateVrfLink(VrfDto vrf, List<VrfIfDto> vrfIfs, String editorName) throws
            InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, vrf);
        buildVrfLinkUpdateCommands(commands, vrf, vrfIfs);
        ShellConnector.getInstance().execute2(commands);
        vrf.renew();
    }

    private static void buildVrfLinkUpdateCommands(ShellCommands commands, VrfDto vrf,
                                                   List<VrfIfDto> vrfIfs) {
        List<VrfIfDto> addedVrfIfs = Util.getAddedList(vrf.getMemberVrfifs(), vrfIfs);
        List<VrfIfDto> removedVrfIfs = Util.getRemovedList(vrf.getMemberVrfifs(), vrfIfs);
        for (VrfIfDto added : addedVrfIfs) {
            translate(commands, CMD.BIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, added.getAbsoluteName());
            commands.addLastEditCommands();
        }
        for (VrfIfDto removed : removedVrfIfs) {
            translate(commands, CMD.UNBIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, removed.getAbsoluteName());
            commands.addLastEditCommands();
        }
    }

    public static void buildVrfIfRemoveCommand(ShellCommands commands, VrfIfDto vrfIf) throws InventoryException {
        if (vrfIf == null) {
            throw new IllegalArgumentException("vrfIf is null.");
        }
        translate(commands, CMD.REMOVE_ELEMENT, "_TYPE_", ATTR.TYPE_VRF_IF, "_NAME_", vrfIf.getName());
    }

    public static void buildChangeVrfIdPoolContextCommand(ShellCommands commands, VrfIntegerIdPoolDto pool) {
        changeContext(commands, pool);
    }

    public static void updateVrfIdPoolAttributes(VrfIntegerIdPoolDto pool, Map<String, String> attributes, String editorName)
            throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildAttributeUpdateCommand(commands, pool, attributes);
        commands.addCommand(CMD.CONTEXT_RESET);
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildVrfIdPoolAttributeUpdateCommands(ShellCommands commands, VrfIntegerIdPoolDto pool, Map<String, String> attributes) {
        if (pool != null) {
            changeContext(commands, pool);
        }
        buildAttributeUpdateCommand(commands, pool, attributes);
    }

    public static void buildVrfAttributeUpdateCommands(ShellCommands commands, VrfDto vrf, Map<String, String> attributes)
            throws InventoryException {
        changeContext(commands, vrf);
        buildAttributeUpdateCommand(commands, vrf, attributes);
    }

    public static void createVrf(VrfStringIdPoolDto pool, String vrfId, String customerId, String rdNo, String routingInstance, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildNetworkIDCreationCommand(commands, ATTR.NETWORK_TYPE_VRF, ATTR.ATTR_VRF_ID_STRING, vrfId,
                pool.getObjectTypeName(), pool.getName());
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void removeVrf(VrfDto vrf, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        InventoryBuilder.changeContext(commands, vrf);
        translate(commands, CMD.ATTRIBUTE_RESET, CMD.ARG_KEY, ATTR.ATTR_VRF_ID_STRING);
        translate(commands, CMD.ATTRIBUTE_RESET, CMD.ARG_KEY, ATTR.ATTR_VRF_POOL_STRING);
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void createVrfIf(NodeDto node, VrfDto vrf, String vrfIfName, String facilityStatus, String vpnId, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, node);
        translate(commands, CMD.CREATE_VRFIF, "_VRFIFNAME_", vrfIfName);
        translateAttribute(commands, MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus);
        translateAttribute(commands, ATTR.IFNAME, vrfIfName);
        changeContext(commands, vrf);
        translate(commands, CMD.BIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, node.getName() + ATTR.NAME_DELIMITER_PRIMARY + vrfIfName);
        ShellConnector.getInstance().execute2(commands);
    }

    public static void removeVrfIf(NodeDto node, VrfDto vrf, VrfIfDto vrfIf, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, vrf);
        translate(commands, CMD.UNBIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, vrfIf.getAbsoluteName());
        for (NodeElementDto ne : vrfIf.getSubElements()) {
            if (!(ne instanceof IpIfDto)) {
                throw new InventoryException(
                        "Unexpected type of node-element found on " +
                                vrfIf.getAbsoluteName() + ": " + ne.getName());
            }
            IpIfDto ipIf = (IpIfDto) ne;
            changeContext(commands, ipIf);
            translate(commands, CMD.ATTRIBUTE_RESET, CMD.ARG_KEY, ATTR.IFNAME);
            translate(commands, CMD.ATTRIBUTE_RESET, CMD.ARG_KEY, ATTR.IP_ADDRESS);
            translate(commands, CMD.ATTRIBUTE_RESET, CMD.ARG_KEY, ATTR.MASK_LENGTH);
            commands.addCommand(CMD.CONTEXT_DOWN);
            translate(commands, CMD.REMOVE_ELEMENT, CMD.ARG_TYPE, ATTR.TYPE_IP_PORT, CMD.ARG_NAME, ipIf.getName());
        }
        changeContext(commands, vrfIf);
        translate(commands, CMD.ATTRIBUTE_RESET, CMD.ARG_KEY, ATTR.IFNAME);
        changeContext(commands, node);
        translate(commands, CMD.REMOVE_ELEMENT, CMD.ARG_TYPE, ATTR.TYPE_VRF_IF, CMD.ARG_NAME, vrfIf.getName());
        ShellConnector.getInstance().execute2(commands);
    }

    public static void setFacilityStatus(String nodeName, String vrfIfName, String facilityStatus, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        translate(commands, "context node;[nodeName],vrf-if;[vrfIfName]", "[nodeName]", nodeName, "[vrfIfName]", vrfIfName);
        translateAttribute(commands, MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus);
        ShellConnector.getInstance().execute2(commands);
    }
}