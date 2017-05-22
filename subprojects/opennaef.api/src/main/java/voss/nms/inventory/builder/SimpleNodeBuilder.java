package voss.nms.inventory.builder;

import naef.dto.*;
import naef.dto.ip.IpIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.builder.ShellCommands;
import voss.core.server.database.ATTR;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.database.MetadataManager;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleNodeBuilder extends InventoryBuilder {
    private static final Logger log = LoggerFactory.getLogger(SimpleNodeBuilder.class);

    public static void buildVlanFeatureEnableCommands(ShellCommands commands) {
        buildAttributeSetOrReset(commands, ATTR.FEATURE_VLAN, ATTR.FEATURE_VLAN_DOT1Q);
    }

    public static void buildVlanFeatureDisableCommands(ShellCommands commands) {
        buildAttributeSetOrReset(commands, ATTR.FEATURE_VLAN, null);
    }

    public static void buildAtmPvcFeatureEnableCommand(ShellCommands commands) {
        buildAttributeSetOrReset(commands, ATTR.FEATURE_ATM_PVC, "true");
    }

    public static void buildAtmPvcFeatureDisableCommand(ShellCommands commands) {
        commands.addCommand(translateResetAttribute(ATTR.FEATURE_ATM_PVC));
    }

    public static void buildFrameRelayFeatureEnableCommand(ShellCommands commands, String encapsulation) {
        if (encapsulation == null) {
            encapsulation = "ANSI";
        }
        buildAttributeSetOrReset(commands, ATTR.ATTR_FR_ENCAPSULATION, encapsulation);
    }

    public static void buildFrameRelayFeatureDisableCommand(ShellCommands commands) {
        commands.addCommand(translateResetAttribute(ATTR.ATTR_FR_ENCAPSULATION));
    }

    public static void buildChangeNodeContextCommand(ShellCommands commands, String name) {
        changeContext(commands, "node", name);
    }

    public static void buildNodeCreationCommands(ShellCommands commands,
                                                 String nodeName, String vendor, String nodeType)
            throws IOException, InventoryException {
        MetadataManager mgr = MetadataManager.getInstance();
        commands.addCommands(translate(mgr.getNodeMetadata(vendor, nodeType), "_NODENAME_", nodeName));
        changeContext(commands, ATTR.TYPE_NODE, nodeName);
        commands.addLastEditCommands();
    }

    public static void updateStringAttributes(NodeElementDto dto, String key,
                                              List<String> values, String editorName) throws ExternalServiceException {
        if (values.size() == 0) {
            return;
        }
        ShellCommands commands = new ShellCommands(editorName);
        commands.addVersionCheckTarget(dto);
        changeContext(commands, dto);
        buildCollectionAttributeUpdateCommands(commands, dto, key, values);
        commands.addCommand(CMD.CONTEXT_DOWN);
        try {
            ShellConnector.getInstance().execute2(commands);
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static void buildCollectionAttributeSetCommands(ShellCommands commands,
                                                           String key, List<String> values) {
        for (String value : values) {
            commands.addCommand(translateAddCollectionAttribute(key, value));
        }
        commands.addLastEditCommands();
    }

    public static void buildCollectionAttributeUpdateCommands(ShellCommands commands, NodeElementDto dto,
                                                              String key, List<String> values) {
        List<String> original = DtoUtil.getStringList(dto, key);
        ArrayList<String> removedItems = new ArrayList<String>(original);
        for (String value : values) {
            if (!original.contains(value)) {
                commands.addCommand(translateAddCollectionAttribute(key, value));
            }
            removedItems.remove(value);
        }
        for (String removed : removedItems) {
            commands.addCommand(translateRemoveCollectionAttribute(key, removed));
        }
        commands.addLastEditCommands();
    }

    public static void buildIntegerIdRangeCollectionAttributeUpdateCommands(ShellCommands commands, NodeElementDto dto,
                                                                            String key, List<String> values) {
        if (!DtoUtil.isSupportedAttribute(dto, key)) {
            return;
        }
        List<IdRange<Integer>> original = DtoUtil.getIdRanges(dto, key);
        ArrayList<String> originalItems = new ArrayList<String>();
        ArrayList<String> removedItems = new ArrayList<String>();
        for (IdRange<Integer> range : original) {
            String range_ = range.lowerBound + "-" + range.upperBound;
            originalItems.add(range_);
            removedItems.add(range_);
        }
        for (String value : values) {
            if (!originalItems.contains(value)) {
                commands.addCommand(translateAddCollectionAttribute(key, value));
            }
            removedItems.remove(value);
        }
        for (String removed : removedItems) {
            commands.addCommand(translateRemoveCollectionAttribute(key, removed));
        }
        commands.addLastEditCommands();
    }

    public static void buildChassisInsertionCommands(ShellCommands commands, NodeDto node, String chassisName,
                                                     String chassisType) throws IOException, InventoryException {
        if (chassisType == null) {
            throw new IllegalStateException("Chassis type is not specified.");
        }
        if (chassisName == null) {
            chassisName = "";
        }
        MetadataManager mgr = MetadataManager.getInstance();
        String vendor = DtoUtil.getString(node, "ベンダー名");
        changeContext(commands, node);
        commands.addVersionCheckTarget(node);
        commands.addLastEditCommands();
        List<String> chassisCommands = mgr.getChassisMetadata(vendor, chassisType);
        chassisCommands = translate(chassisCommands, "_CHASSIS_NAME_", chassisName);
        commands.addCommands(chassisCommands);
        changeContext(commands, node, ATTR.TYPE_CHASSIS, chassisName);
        commands.addLastEditCommands();
    }

    public static void buildChassisRemoveCommands(ShellCommands commands, ChassisDto chassis) {
        if (chassis == null) {
            return;
        }
        for (NodeElementDto hardware : chassis.getSubElements()) {
            if (!(hardware instanceof HardwareDto)) {
                continue;
            }
            log.debug("remove target: " + hardware.getAbsoluteName());
            if (hardware instanceof SlotDto) {
                SlotDto subSlot = (SlotDto) hardware;
                buildModuleRemoveCommands(commands, subSlot);
                changeContext(commands, subSlot);
                commands.addCommand(CMD.CONTEXT_DOWN);
                commands.addCommand(translate(CMD.REMOVE_ELEMENT,
                        "_TYPE_", subSlot.getObjectTypeName(),
                        "_NAME_", subSlot.getName()));
            } else if (hardware instanceof JackDto) {
                buildJackRemoveCommands(commands, (JackDto) hardware);
            } else {
                throw new IllegalStateException("Unexpected hardware found: " + hardware.getClass().getName());
            }
        }
        changeContext(commands, chassis);
        commands.addCommand(CMD.CONTEXT_DOWN);
        commands.addCommand(translate(CMD.REMOVE_ELEMENT,
                "_TYPE_", chassis.getObjectTypeName(),
                "_NAME_", chassis.getName()));
        commands.addLastEditCommands();
    }

    public static void buildJackRemoveCommands(ShellCommands commands, JackDto jack) {
        changeContext(commands, jack);
        commands.addVersionCheckTarget(jack);
        PortDto port = jack.getPort();
        if (port != null) {
            buildRemovePortCommands(commands, port);
        }
        commands.addCommand(CMD.CONTEXT_DOWN);
        commands.addCommand(translate(CMD.REMOVE_ELEMENT,
                "_TYPE_", jack.getObjectTypeName(),
                "_NAME_", jack.getName()));
    }

    public static void buildModuleInsertionCommands(ShellCommands commands, SlotDto slot,
                                                    String moduleTypeName, Map<String, String> initialAttributes) throws IOException, InventoryException {
        MetadataManager mgr = MetadataManager.getInstance();
        String vendor = DtoUtil.getString(slot.getNode(), MPLSNMS_ATTR.VENDOR_NAME);
        changeContext(commands, slot);
        commands.addVersionCheckTarget(slot);
        commands.addLastEditCommands();
        List<String> moduleCommands = mgr.getModuleMetadata(vendor, moduleTypeName);
        commands.addCommands(moduleCommands);
        changeContext(commands, slot, ATTR.TYPE_MODULE, "");
        commands.addLastEditCommands();
    }

    public static void buildModuleInsertionCommands(ShellCommands commands, String vendor, String slotName,
                                                    String moduleTypeName, Map<String, String> initialAttributes) throws IOException, InventoryException {
        MetadataManager mgr = MetadataManager.getInstance();
        changeContext(commands, slotName);
        commands.addLastEditCommands();
        List<String> moduleCommands = mgr.getModuleMetadata(vendor, moduleTypeName);
        commands.addCommands(moduleCommands);
        changeContext(commands, slotName);
        changeContext(commands, ATTR.TYPE_MODULE, "");
        commands.addLastEditCommands();
    }

    public static void buildModuleRemoveCommands(ShellCommands commands, SlotDto slot) {
        if (slot == null) {
            return;
        } else if (slot.getModule() == null) {
            return;
        }
        buildHardwareRemoveCommands(commands, slot.getModule());
    }

    public static void buildHardwareRemoveCommands(ShellCommands cmd, NodeElementDto element) {
        if (element == null) {
            return;
        }
        Set<NodeElementDto> subElements = element.getSubElements();
        if (subElements != null) {
            for (NodeElementDto subElement : subElements) {
                buildHardwareRemoveCommands(cmd, subElement);
            }
        }
        if (element instanceof PortDto) {
            IpIfDto ip = ((PortDto) element).getPrimaryIpIf();
            if (ip != null) {
                changeContext(cmd, ip.getOwner());
                cmd.addLastEditCommands();
                cmd.addCommand(translate(CMD.REMOVE_ELEMENT,
                        "_TYPE_", ip.getObjectTypeName(),
                        "_NAME_", ip.getName()));
            }
            IpIfDto ip2 = ((PortDto) element).getSecondaryIpIf();
            if (ip2 != null) {
                changeContext(cmd, ip2.getOwner());
                cmd.addLastEditCommands();
                cmd.addCommand(translate(CMD.REMOVE_ELEMENT,
                        "_TYPE_", ip2.getObjectTypeName(),
                        "_NAME_", ip2.getName()));
            }
        }
        log.debug("remove target: " + element.getAbsoluteName());
        changeContext(cmd, element.getOwner());
        cmd.addLastEditCommands();
        cmd.addCommand(translate(CMD.REMOVE_ELEMENT,
                "_TYPE_", element.getObjectTypeName(),
                "_NAME_", element.getName()));
    }

    private static void buildRemovePortCommands(ShellCommands commands, PortDto port) {
        PortDto l1Neighbor = NodeUtil.getLayer1Neighbor(port);
        PortDto l2Neighbor = NodeUtil.getLayer2Neighbor(port);
        if (!Util.isAllNull(l1Neighbor, l2Neighbor)) {
            throw new IllegalStateException("There is a link on port:" + NameUtil.getIfName(port));
        }
        Set<NetworkDto> networks = port.getNetworks();
        if (networks.size() > 0) {
            throw new IllegalStateException("Port[" + NameUtil.getIfName(port) + "] is used by network(s).");
        }
        commands.addVersionCheckTarget(port);
        commands.addCommand(translate(CMD.REMOVE_ELEMENT,
                "_TYPE_", port.getObjectTypeName(),
                "_NAME_", port.getName()));
    }


    public static void buildHardwareCreationCommands(ShellCommands commands, String type, String name) {
        if (name == null || type == null) {
            throw new IllegalArgumentException("hardware-type or name is null.");
        }
        commands.addCommand(translateNewHardware(type, name));
        commands.addLastEditCommands();
    }


    public static void buildPortCreationCommands(ShellCommands commands, String type, String ifName) {
        if (ifName == null || type == null) {
            throw new IllegalArgumentException("ifName or type is null.");
        }
        commands.addCommand(translateNewPort(type, ifName));
        commands.addLastEditCommands();
    }

    public static void buildPortDeletionCommands(ShellCommands commands, PortDto port) {
        commands.addCommand(translate(CMD.REMOVE_ELEMENT, "_TYPE_", port.getObjectTypeName(), "_NAME_", port.getName()));
    }

    public static String translateNewHardware(String type, String id) {
        if (Util.isNull(type, id)) {
            throw new IllegalArgumentException();
        }
        return translate(CMD.NEW_HARDWARE, CMD.NEW_HARDWARE_KEY_1, type, CMD.NEW_HARDWARE_KEY_2, id);
    }

    public static String translateNewPort(String type, String id) {
        if (Util.isNull(type, id)) {
            throw new IllegalArgumentException();
        }
        return translate(CMD.NEW_PORT, CMD.NEW_PORT_KEY_1, type, CMD.NEW_PORT_KEY_2, id);
    }

    public static String translateNewEtherPort(String id) {
        if (Util.isNull(id)) {
            throw new IllegalArgumentException();
        }
        return translate(CMD.NEW_PORT, CMD.NEW_PORT_KEY_1, ATTR.TYPE_ETH_PORT, CMD.NEW_PORT_KEY_2, id);
    }

    private static final String newChassisCommand = CMD.NEW_HARDWARE_BODY + " " + ATTR.TYPE_CHASSIS;

    public static List<String> getChassisId(ShellCommands commands) {
        List<String> result = new ArrayList<String>();
        for (String command : commands.getCommands()) {
            command = command.trim();
            if (command.startsWith(newChassisCommand)) {
                String chassisId = command.replace(newChassisCommand, "");
                chassisId = chassisId.replace("\"", "");
                chassisId = chassisId.replace("\\", "");
                chassisId = chassisId.trim();
                result.add(chassisId);
            }
        }
        return result;
    }
}