package voss.multilayernms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import naef.dto.vpls.VplsIntegerIdPoolDto;
import naef.dto.vpls.VplsStringIdPoolDto;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.Util;
import voss.core.server.util.VplsUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.*;

public class VplsBuilder extends InventoryBuilder {
    public static void createVplsPool(String name, String parent, String range, String editorName) throws
            InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, ATTR.POOL_TYPE_VPLS_STRING_TYPE, "Nationwide");
        changeContext(commands, parent);
        translate(commands, CMD.POOL_CREATE,
                CMD.POOL_CREATE_ARG1, ATTR.POOL_TYPE_VPLS_STRING_TYPE,
                CMD.POOL_CREATE_ARG2, name);
        translate(commands, CMD.POOL_RANGE_ALLOCATE, "_RANGE_", range);
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildVplsPoolCreationCommands(ShellCommands commands, String name, String parent,
                                                     String range, Map<String, String> attributes) {
        changeContext(commands, ATTR.POOL_TYPE_VPLS_STRING_TYPE, "Nationwide");
        translate(commands, CMD.POOL_CREATE,
                CMD.POOL_CREATE_ARG1, ATTR.POOL_TYPE_VPLS_STRING_TYPE,
                CMD.POOL_CREATE_ARG2, name);
        translate(commands, CMD.POOL_RANGE_ALLOCATE, "_RANGE_", range);
        buildAttributeSetOnCurrentContextCommands(commands, attributes);
    }

    public static void createVpls(VplsIntegerIdPoolDto pool, Integer id, String editorName) throws
            InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildNetworkIDCreationCommand(commands, ATTR.NETWORK_TYPE_VPLS,
                ATTR.ATTR_VPLS_ID_INTEGER, id.toString(),
                ATTR.ATTR_VPLS_POOL_INTEGER, pool.getName());
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void changeVplsRange(VplsIntegerIdPoolDto pool, String newRange, String editorName) throws
            InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildChangeRangeCommand(commands, pool.getConcatenatedIdRangesStr(), newRange, false);
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildVplsRangeChangeCommands(ShellCommands commands, VplsIntegerIdPoolDto pool, String newRange)
            throws InventoryException {
        buildChangeRangeCommand(commands, pool.getConcatenatedIdRangesStr(), newRange, false);
    }

    public static void buildVplsAttachedPortsResetCommands(ShellCommands commands, VplsDto vpls)
            throws InventoryException {
        for (VplsIfDto vplsIf : vpls.getMemberVplsifs()) {
            buildVplsIfBoundPortUpdateCommands(commands, vpls, vplsIf.getNode(), new ArrayList<PortDto>());
        }
    }

    public static void updateVplsIfBoundPort(VplsDto vpls, NodeDto node, List<PortDto> selected, String editorName)
            throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        buildVplsIfBoundPortUpdateCommands(commands, vpls, node, selected);
        ShellConnector.getInstance().execute2(commands);
        vpls.renew();
    }

    public static void buildVplsIfBoundPortUpdateCommands(ShellCommands commands, VplsDto vpls, NodeDto node,
                                                          List<PortDto> selected) throws InventoryException {
        if (vpls == null) {
            throw new IllegalArgumentException("vpls is null.");
        } else if (node == null) {
            throw new IllegalArgumentException("node is null.");
        }
        String vplsName = VplsUtil.getVplsIfName(vpls);
        VplsIfDto vplsIf = VplsUtil.getVplsIf(node, vpls);
        if (selected.size() > 0) {
            if (vplsIf == null) {
                vplsIf = VplsUtil.getOrphanedVplsIf(node, vpls);
                if (vplsIf == null) {
                    changeContext(commands, node);
                    translate(commands, CMD.CREATE_VPLSIF, "_VPLSIFNAME_", vplsName);
                    translateAttribute(commands, "VPLS ID", vpls.getStringId());
                    changeContext(commands, vpls);
                    translate(commands, CMD.BIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, VplsUtil.getVplsIfFqn(node, vpls));
                } else {
                    List<VplsIfDto> list = new ArrayList<VplsIfDto>(vpls.getMemberVplsifs());
                    list.add(vplsIf);
                    changeContext(commands, vpls);
                    buildVplsLinkUpdateCommands(commands, vpls, list);
                }
            }
        }
        changeContext(commands, node);
        Set<PortDto> attached = new HashSet<PortDto>();
        if (vplsIf != null) {
            for (PortDto port : vplsIf.getAttachedPorts()) {
                attached.add(port);
            }
        }
        List<PortDto> addedPorts = Util.getAddedList(attached, selected);
        List<PortDto> removedPorts = Util.getRemovedList(attached, selected);
        for (PortDto added : addedPorts) {
            translate(commands, CMD.CONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, vplsName,
                    CMD.ARG_PORT, added.getNodeLocalName());
            commands.addLastEditCommands();
        }
        for (PortDto removed : removedPorts) {
            translate(commands, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                    CMD.ARG_INSTANCE, vplsName,
                    CMD.ARG_PORT, removed.getNodeLocalName());
            commands.addLastEditCommands();
        }
        if (selected.size() == 0 && vplsIf != null) {
            List<VplsIfDto> list = new ArrayList<VplsIfDto>(vpls.getMemberVplsifs());
            list.remove(vplsIf);
            changeContext(commands, vpls);
            buildVplsLinkUpdateCommands(commands, vpls, list);
            changeContext(commands, vplsIf.getNode());
            buildVplsIfRemoveCommand(commands, vplsIf);
        }
    }

    public static void updateVplsLink(VplsDto vpls, List<VplsIfDto> vplsIfs, String editorName) throws
            InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, vpls);
        buildVplsLinkUpdateCommands(commands, vpls, vplsIfs);
        ShellConnector.getInstance().execute2(commands);
        vpls.renew();
    }

    private static void buildVplsLinkUpdateCommands(ShellCommands commands, VplsDto vpls,
                                                    List<VplsIfDto> vplsIfs) throws
            InventoryException {
        List<VplsIfDto> addedVplsIfs = Util.getAddedList(vpls.getMemberVplsifs(), vplsIfs);
        List<VplsIfDto> removedVplsIfs = Util.getRemovedList(vpls.getMemberVplsifs(), vplsIfs);
        for (VplsIfDto added : addedVplsIfs) {
            translate(commands, CMD.BIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, added.getAbsoluteName());
        }
        for (VplsIfDto removed : removedVplsIfs) {
            translate(commands, CMD.UNBIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, removed.getAbsoluteName());
        }
        commands.addLastEditCommands();
    }

    public static void buildVplsIfRemoveCommand(ShellCommands commands, VplsIfDto vplsIf) throws InventoryException {
        if (vplsIf == null) {
            throw new IllegalArgumentException("vplsIf is null.");
        }
        translate(commands, CMD.REMOVE_ELEMENT, CMD.ARG_TYPE, ATTR.TYPE_VPLS_IF, CMD.ARG_NAME, vplsIf.getName());
    }

    public static void updateVplsPoolAttributes(VplsIntegerIdPoolDto pool, Map<String, String> attributes, String editorName)
            throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildAttributeUpdateCommand(commands, pool, attributes);
        commands.addCommand(CMD.CONTEXT_RESET);
        ShellConnector.getInstance().execute2(commands);
    }

    public static void buildVplsPoolAttributeUpdateCommands(ShellCommands commands, VplsIntegerIdPoolDto pool,
                                                            Map<String, String> attributes) {
        buildAttributeUpdateCommand(commands, pool, attributes);
    }

    public static void buildVplsAttributeUpdateCommands(ShellCommands commands, VplsDto vpls,
                                                        Map<String, String> attributes)
            throws InventoryException {
        changeContext(commands, vpls);
        buildAttributeUpdateCommand(commands, vpls, attributes);
    }

    public void createUpdateVplsIf(String vplsIfName, Integer vplsId, Map<String, String> attributes, String editorName, NodeDto node, VplsIfDto vplsIf) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        if (vplsIf == null) {
            changeContext(commands, node);
            commands.addCommand(translate(CMD.CREATE_VPLSIF, "_VPLSIFN AME_", vplsIfName));
            commands.addCommand(translate(CMD.ATTRIBUTE_SET, "VPLS ID", vplsId.toString()));

        } else {
            InventoryBuilder.changeContext(commands, vplsIf);
            commands.addCommand(translate(CMD.CREATE_VPLSIF, "_VPLSIFNAME_", vplsIfName));
            buildAttributeSetOrReset(commands, MPLSNMS_ATTR.IFNAME, vplsIfName);
        }

        for (String key : attributes.keySet()) {
            buildAttributeSetOrReset(commands, key, attributes.get(key));
        }
        ShellConnector.getInstance().execute2(commands);
    }


    public static void createVpls(VplsStringIdPoolDto pool, String id,
                                  String customerId, String rdNo, String routingInstance, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, pool);
        buildNetworkIDCreationCommand(commands, ATTR.NETWORK_TYPE_VPLS,
                ATTR.ATTR_VPLS_ID_STRING, id,
                ATTR.ATTR_VPLS_POOL_STRING, pool.getName());
        InventoryBuilder.buildAttributeSetOrReset(commands, ATTR.ATTR_VPLS_POOL_STRING, pool.getName());
        InventoryBuilder.buildAttributeSetOrReset(commands, ATTR.ATTR_VPLS_ID_STRING, id);
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void removeVpls(VplsDto vpls, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        InventoryBuilder.changeContext(commands, vpls);
        commands.addCommand(translate(CMD.ATTRIBUTE_RESET, CMD.ARG_KEY, ATTR.ATTR_VPLS_ID_STRING));
        commands.addCommand(translate(CMD.ATTRIBUTE_RESET, CMD.ARG_KEY, ATTR.ATTR_VPLS_POOL_STRING));
        commands.addLastEditCommands();
        ShellConnector.getInstance().execute2(commands);
    }

    public static void createVplsIf(NodeDto node, VplsDto vpls, String vplsIfName, String facilityStatus, String vpnId, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, node);
        translate(commands, CMD.CREATE_VPLSIF, "_VPLSIFNAME_", vplsIfName);
        changeContext(commands, vpls);
        translate(commands, CMD.BIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, node.getName() + ATTR.NAME_DELIMITER_PRIMARY + vplsIfName);
        translate(commands, "context node;[nodeName],vpls-if;[vplsIfName]", "[nodeName]", node.getName(), "[vplsIfName]", vplsIfName);
        translateAttribute(commands, MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus);
        translateAttribute(commands, ATTR.IFNAME, vplsIfName);
        ShellConnector.getInstance().execute2(commands);
    }

    public static void removeVplsIf(NodeDto node, VplsDto vpls, VplsIfDto vplsIf, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        changeContext(commands, vpls);
        translate(commands, CMD.UNBIND_NETWORK_INSTANCE, CMD.ARG_INSTANCE, vplsIf.getAbsoluteName());
        changeContext(commands, node);
        translate(commands, CMD.REMOVE_ELEMENT, "_TYPE_", ATTR.TYPE_VPLS_IF, "_NAME_", vplsIf.getName());
        ShellConnector.getInstance().execute2(commands);
    }

    public static void setFacilityStatus(String nodeName, String vplsIfName, String facilityStatus, String editorName) throws InventoryException {
        ShellCommands commands = new ShellCommands(editorName);
        translate(commands, "context node;[nodeName],vpls-if;[vplsIfName]", "[nodeName]", nodeName, "[vplsIfName]", vplsIfName);
        translateAttribute(commands, MPLSNMS_ATTR.FACILITY_STATUS, facilityStatus);
        ShellConnector.getInstance().execute2(commands);
    }
}