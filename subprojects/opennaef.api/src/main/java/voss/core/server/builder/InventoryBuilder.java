package voss.core.server.builder;

import naef.dto.*;
import naef.dto.mpls.RsvpLspHopSeriesDto;
import naef.dto.mpls.RsvpLspHopSeriesIdPoolDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.dto.EntityDto;
import voss.core.common.diff.DiffType;
import voss.core.server.config.AttributePolicy;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.InventoryUtil;
import voss.core.server.util.Util;

import java.util.*;

public class InventoryBuilder {
    private static Logger log = LoggerFactory.getLogger(InventoryBuilder.class);

    private static Logger log() {
        if (log == null) {
            log = LoggerFactory.getLogger(InventoryBuilder.class);
        }
        return log;
    }

    public static void buildRenameCommands(ShellCommands commands, String name) {
        commands.addCommand(translate(CMD.RENAME, "_NAME_", name));
    }

    public static void buildForcedRenameCommands(ShellCommands commands, String name) {
        commands.addCommand(translate(true, CMD.RENAME, "_NAME_", name));
    }

    public static void buildIdRangeCollectionAttributeUpdateCommands(ShellCommands commands, NodeElementDto dto,
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
    }

    public static void buildChangeRangeCommand(ShellCommands commands, String oldRange,
                                               String newRange, boolean allowZero) throws InventoryException {
        oldRange = Util.formatRange(oldRange);
        newRange = Util.formatRange(newRange);
        InventoryUtil.checkRange(oldRange, allowZero);
        InventoryUtil.checkRange(newRange, allowZero);
        String[] oldOne = oldRange.split("-");
        String[] newOne = newRange.split("-");
        long oldLower = Long.parseLong(oldOne[0]);
        long oldUpper = Long.parseLong(oldOne[1]);
        long newLower = Long.parseLong(newOne[0]);
        long newUpper = Long.parseLong(newOne[1]);

        long lower = oldLower;
        long upper = oldUpper;
        if (newUpper < oldLower) {
            commands.addCommand(translate(CMD.POOL_RANGE_ALLOCATE, "_RANGE_", newRange));
            commands.addCommand(translate(CMD.POOL_RANGE_RELEASE, "_RANGE_", oldRange));
        } else if (oldUpper < newLower) {
            commands.addCommand(translate(CMD.POOL_RANGE_ALLOCATE, "_RANGE_", newRange));
            commands.addCommand(translate(CMD.POOL_RANGE_RELEASE, "_RANGE_", oldRange));
        } else {
            if (oldLower != newLower) {
                String range = newLower + "-" + upper;
                commands.addCommand(translate(CMD.POOL_RANGE_ALTER, "_OLD_", oldRange, "_NEW_", range));
                lower = newLower;
            }
            if (oldUpper != newUpper) {
                String range1 = lower + "-" + upper;
                String range2 = lower + "-" + newUpper;
                commands.addCommand(translate(CMD.POOL_RANGE_ALTER, "_OLD_", range1, "_NEW_", range2));
            }
        }
    }

    public static void buildClearRangeCommand(ShellCommands commands, String oldRange,
                                              boolean allowZero) throws InventoryException {
        oldRange = Util.formatRange(oldRange);
        Util.checkRange(oldRange, allowZero);
        commands.addCommand(translate(CMD.POOL_RANGE_RELEASE, "_RANGE_", oldRange));
    }

    public static <S, T extends NetworkDto> void changeIdRange(ShellCommands cmd,
                                                               IdPoolDto<?, ?, T> pool, String newRange, IdResolver<T> idResolver, PoolResolver<T> poolResolver)
            throws InventoryException {
        AttributePolicy policy = CoreConfiguration.getInstance().getAttributePolicy();
        if (cmd == null) {
            throw new IllegalStateException("cmd is null.");
        } else if (pool == null) {
            throw new IllegalStateException("pool is unspecified.");
        } else if (newRange == null) {
            throw new IllegalStateException("range is unspecified.");
        }
        List<String> newRanges = Arrays.asList(newRange.split(","));
        checkRangeCompatible(pool, newRanges, idResolver, poolResolver);
        Map<String, T> users = new HashMap<String, T>();
        for (T user : pool.getUsers()) {
            changeContext(cmd, user);
            buildNetworkIDReleaseCommand(cmd, idResolver.getIdTye(), idResolver.getPoolType());
            if (policy.isToBeInitializeState(user, null)) {
                continue;
            }
            users.put(idResolver.getId(user), user);
        }
        InventoryBuilder.changeContext(cmd, pool);
        for (IdRange<?> range : pool.getIdRanges()) {
            String s = range.lowerBound + "-" + range.upperBound;
            InventoryBuilder.translate(cmd, CMD.POOL_RANGE_RELEASE, CMD.POOL_RANGE_RELEASE_ARG1, s);
        }
        for (String range : newRanges) {
            InventoryBuilder.translate(cmd, CMD.POOL_RANGE_ALLOCATE, CMD.POOL_RANGE_ALLOCATE_ARG1, range);
        }
        for (Map.Entry<String, T> entry : users.entrySet()) {
            String id = entry.getKey();
            T user = entry.getValue();
            changeContext(cmd, getMvoContext(user));
            buildNetworkIDAssignmentCommand(cmd, idResolver.getIdTye(), id,
                    idResolver.getPoolType(), pool.getAbsoluteName());
        }
    }

    private static <T extends NetworkDto, R extends IdRange<?>> void
    checkRangeCompatible(IdPoolDto<?, ?, T> pool, List<String> stringRanges, IdResolver<T> idResolver,
                         PoolResolver<T> poolResolver) {
        AttributePolicy policy = CoreConfiguration.getInstance().getAttributePolicy();
        for (T user : pool.getUsers()) {
            if (idResolver.getId(user) == null) {
                continue;
            } else if (policy.isToBeInitializeState(user, null)) {
                continue;
            }
            boolean match = false;
            for (String range : stringRanges) {
                if (poolResolver.isInRange(user, range)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                throw new IllegalStateException("There is an ID that protrudes from the newly specified range: " + idResolver.getId(user));
            }
        }
    }

    public static interface IdResolver<T extends NetworkDto> {
        String getId(T id);

        String getIdTye();

        String getPoolType();
    }

    public static interface PoolResolver<T extends NetworkDto> {
        boolean isInRange(T id, String range);
    }

    public static void changeContext(ShellCommands cmd, NaefDto dto) {
        cmd.addCommand(translate(CMD.CONTEXT_BY_ABSOLUTE_NAME, CMD.ARG_NAME, dto.getAbsoluteName()));
    }

    public static void changeContext(ShellCommands cmd, String absoluteName) {
        if (absoluteName == null) {
            throw new IllegalArgumentException("absolute-name is null.");
        }
        cmd.addCommand(translate(CMD.CONTEXT_BY_ABSOLUTE_NAME, CMD.ARG_NAME, absoluteName));
    }

    public static void changeContext(ShellCommands cmd, String typeName, String objectName) {
        if (typeName == null || objectName == null) {
            throw new IllegalArgumentException("type-name or object-name is null.");
        }
        cmd.addCommand(translate(CMD.CONTEXT_BY_ABSOLUTE_NAME, CMD.ARG_NAME, getRelativeName(typeName, objectName)));
    }

    public static void changeContext(ShellCommands cmd, NaefDto base, String type, String name) {
        String absoluteName = appendContext(base, type, name);
        changeContext(cmd, absoluteName);
    }

    public static void changeMvoContext(ShellCommands cmd, EntityDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("arg is null.");
        }
        if (NaefDto.class.isInstance(dto)) {
            cmd.log("mvo-id of [" + ((NaefDto) dto).getAbsoluteName() + "]");
        }
        translate(cmd, CMD.CONTEXT_BY_MVO, CMD.ARG_MVOID, DtoUtil.getMvoId(dto).toString());
    }

    public static String getMvoContext(EntityDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("arg is null.");
        }
        return getRelativeName(ATTR.TYPE_MVO_ID, DtoUtil.getMvoId(dto).toString());
    }

    public static String appendContext(NaefDto base, String type, String name) {
        return appendContext(base.getAbsoluteName(), type, name);
    }

    public static String appendContext(String base, String type, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(base)
                .append(ATTR.NAME_DELIMITER_PRIMARY)
                .append(getRelativeName(type, name));
        return sb.toString();
    }

    public static String getRelativeName(String type, String name) {
        if (type == null) {
            throw new IllegalArgumentException("type is null.");
        }
        StringBuilder sb = new StringBuilder();
        if (type != null) {
            sb.append(type);
        }
        sb.append(ATTR.NAME_DELIMITER_SECONDARY);
        if (name != null) {
            sb.append(name);
        }
        return sb.toString();
    }

    public static String getPortType(PortDto port) {
        return port.getObjectTypeName();
    }

    public static String getDemarcationLinkContextCommand(String typeName, String name) {
        log().trace("changeContext(): type=" + typeName + ", name=" + name);
        if (typeName == null || name == null) {
            throw new IllegalArgumentException();
        }
        checkString(typeName, name);
        String absoluteName = typeName + ATTR.NAME_DELIMITER_SECONDARY + "{" + name + "}";
        return translate(CMD.CONTEXT_BY_ABSOLUTE_NAME, CMD.ARG_NAME, absoluteName);
    }

    public static void changeContext(ShellCommands commands, EntityDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException();
        } else if (dto instanceof NaefDto) {
            changeContext(commands, (NaefDto) dto);
            return;
        }
        log().warn("cannot resolve type: " + dto.getClass().getName() + ", " + dto.toString());
        String mvoId = DtoUtil.getMvoId(dto).toString();
        commands.addCommand(getMvoDescription(dto));
        commands.addCommand(translate(CMD.CONTEXT_BY_MVO, "_MVO_ID_", mvoId));
    }

    public static void changeContext(ShellCommands cmd, String linkType, PortDto... ports) {
        translate(cmd, CMD.CONTEXT_BY_ABSOLUTE_NAME, CMD.ARG_NAME, getLinkAbsoluteName(linkType, ports));
    }

    public static String getLinkAbsoluteName(String linkType, PortDto... ports) {
        if (Util.isNull(linkType, ports)) {
            throw new IllegalArgumentException();
        } else if (ports.length == 0) {
            throw new IllegalArgumentException();
        }
        List<String> names = new ArrayList<String>();
        for (PortDto port : ports) {
            names.add(port.getAbsoluteName());
        }
        Collections.sort(names);
        String[] array = names.toArray(new String[names.size()]);
        return getLinkAbsoluteName(linkType, array);
    }

    public static String getLinkAbsoluteName(String linkType, String... names) {
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(name);
        }
        sb.insert(0, ";{");
        sb.insert(0, linkType);
        sb.append("}");
        return sb.toString();
    }

    public static void changeTopContext(ShellCommands commands) {
        commands.addCommand(CMD.CONTEXT_RESET);
    }

    public static void changeParentContext(ShellCommands commands) {
        commands.addCommand(CMD.CONTEXT_DOWN);
    }

    public static void changeIfNameContext(ShellCommands cmd, String nodeName, String ifName) {
        AttributePolicy policy = CoreConfiguration.getInstance().getAttributePolicy();
        String nodePart = getRelativeName(ATTR.TYPE_NODE, nodeName);
        String context = appendContext(nodePart, policy.getIfNameAttributeName(), ifName);
        changeContext(cmd, context);
    }

    public static void changeIfIndexContext(ShellCommands cmd, String nodeName, int ifIndex) {
        String nodePart = getRelativeName(ATTR.TYPE_NODE, nodeName);
        String context = appendContext(nodePart, ATTR.IFINDEX, String.valueOf(ifIndex));
        changeContext(cmd, context);
    }

    public static void navigate(ShellCommands commands, String upperLayerName) {
        String line = translate(CMD.NAVIGATE, "_UPPERLAYER_", upperLayerName);
        commands.addCommand(line);
    }

    public static void assignVar(ShellCommands commands, String varName) {
        String line = translate(CMD.ASSIGN, "_VAR_", varName);
        commands.addCommand(line);
    }

    public static void buildHierarchicalModelCreationCommand(ShellCommands cmd, String type, String name) {
        translate(cmd, CMD.HIERARCHICAL_MODEL_CREATE,
                CMD.HIERARCHICAL_MODEL_CREATE_ARG1, type,
                CMD.HIERARCHICAL_MODEL_CREATE_ARG2, name);
    }

    public static void buildHierarchicalModelParentChangeCommand(ShellCommands cmd, String newParent) {
        translate(cmd, CMD.HIERARCHICAL_MODEL_PARENT_CHANGE,
                CMD.HIERARCHICAL_MODEL_PARENT_CHANGE_ARG, newParent);
    }

    public static void buildNetworkIDCreationCommand(ShellCommands commands, String nwType, String idType, String id, String poolType, String poolName) {
        translate(commands, CMD.NETWORK_CREATE, CMD.NETWORK_CREATE_ARG, nwType);
        buildNetworkIDAssignmentCommand(commands, idType, id, poolType, poolName);
    }

    public static void buildNetworkIDAssignmentCommand(ShellCommands commands, String idType, String id, String poolType, String poolName) {
        translate(commands, CMD.NETWORK_SET_POOL1, CMD.NETWORK_SET_POOL1_ARG1, poolType, CMD.NETWORK_SET_POOL1_ARG2, poolName);
        translate(commands, CMD.NETWORK_SET_ID2, CMD.NETWORK_SET_ID2_ARG1, idType, CMD.NETWORK_SET_ID2_ARG2, id);
    }

    public static void buildNetworkIDReleaseCommand(ShellCommands cmd, String idType, String poolType) {
        translate(cmd, CMD.NETWORK_RESET_ID1, CMD.NETWORK_RESET_ID1_ARG, idType);
        translate(cmd, CMD.NETWORK_RESET_POOL2, CMD.NETWORK_RESET_POOL2_ARG, poolType);
    }

    public static void buildNetworkIDRenameCommands(ShellCommands cmd, String idType, String newName) {
        if (newName == null) {
            throw new IllegalArgumentException("newName is null.");
        }
        buildAttributeSetOrReset(cmd, idType, null);
        buildAttributeSetOrReset(cmd, idType, newName);
    }

    public static void buildRsvpLspHopSeriesPoolChangeCommand(ShellCommands cmd, RsvpLspHopSeriesDto id, RsvpLspHopSeriesIdPoolDto newPool) {
        changeContext(cmd, id);
        String name = id.getName();
        buildAttributeSetOrReset(cmd, ATTR.ATTR_PATH_ID, null);
        buildAttributeSetOrReset(cmd, ATTR.ATTR_PATH_POOL, newPool.getAbsoluteName());
        buildAttributeSetOrReset(cmd, ATTR.ATTR_PATH_ID, name);
    }

    public static void buildRsvpLspHopSeriesPoolChangeCommand(ShellCommands cmd, RsvpLspHopSeriesDto id, String newName, RsvpLspHopSeriesIdPoolDto newPool) {
        changeContext(cmd, id);
        buildAttributeSetOrReset(cmd, ATTR.ATTR_PATH_ID, null);
        buildAttributeSetOrReset(cmd, ATTR.ATTR_PATH_POOL, newPool.getAbsoluteName());
        buildAttributeSetOrReset(cmd, ATTR.ATTR_PATH_ID, newName);
    }

    public static void buildNetworkStackCommand(ShellCommands cmd, NetworkDto upper, NetworkDto lower) {
        if (upper == null) {
            throw new IllegalArgumentException();
        }
        changeContext(cmd, upper);
        buildNetworkStackCommand(cmd, lower);
    }

    public static void buildNetworkStackCommand(ShellCommands cmd, NetworkDto lower) {
        if (lower == null) {
            throw new IllegalArgumentException();
        }
        buildNetworkStackCommand(cmd, lower.getAbsoluteName());
    }

    public static void buildNetworkStackCommand(ShellCommands cmd, String lowerNetworkAbsoluteName) {
        translate(cmd, CMD.STACK_LOWER_NETWORK, CMD.ARG_LOWER, lowerNetworkAbsoluteName);
    }

    public static void buildNetworkUnstackCommand(ShellCommands cmd, NetworkDto upper, NetworkDto lower) {
        if (upper == null) {
            throw new IllegalArgumentException();
        }
        changeContext(cmd, upper);
        buildNetworkUnstackCommand(cmd, lower);
    }

    public static void buildNetworkUnstackCommand(ShellCommands cmd, NetworkDto lower) {
        if (lower == null) {
            throw new IllegalArgumentException();
        }
        buildNetworkUnstackCommand(cmd, lower.getAbsoluteName());
    }

    public static void buildNetworkUnstackCommand(ShellCommands cmd, String lowerNetworkAbsoluteName) {
        translate(cmd, CMD.UNSTACK_LOWER_NETWORK, CMD.ARG_LOWER, lowerNetworkAbsoluteName);
    }

    public static void buildPortStackCommand(ShellCommands cmd, PortDto networkIf, PortDto boundPort) {
        buildPortStackCommand(cmd, networkIf.getAbsoluteName(), boundPort.getAbsoluteName());
    }

    public static void buildPortStackCommand(ShellCommands cmd, String vifContext, PortDto boundPort) {
        buildPortStackCommand(cmd, vifContext, boundPort.getAbsoluteName());
    }

    public static void buildPortStackCommand(ShellCommands cmd, String vifContext, String boundPortContext) {
        InventoryBuilder.translate(cmd, CMD.PORT_STACK,
                CMD.ARG_LOWER, boundPortContext,
                CMD.ARG_UPPER, vifContext);
    }

    public static void buildPortUnstackCommand(ShellCommands cmd, PortDto networkIf, PortDto boundPort) {
        buildPortUnstackCommand(cmd, networkIf.getAbsoluteName(), boundPort);
    }

    public static void buildPortUnstackCommand(ShellCommands cmd, String networkIfContext, PortDto boundPort) {
        InventoryBuilder.translate(cmd, CMD.PORT_UNSTACK,
                CMD.ARG_LOWER, boundPort.getAbsoluteName(),
                CMD.ARG_UPPER, networkIfContext);
    }

    public static void buildBindPortToNetworkCommands(ShellCommands commands, NetworkDto network, PortDto port) {
        buildBindPortToNetworkCommands(commands, network, port, false);
    }

    public static void buildBindPortToNetworkCommands(ShellCommands commands, NetworkDto network, PortDto port, boolean changeContext) {
        if (changeContext) {
            InventoryBuilder.changeContext(commands, network);
        }
        commands.addCommand(translate(CMD.BIND_NETWORK_INSTANCE,
                CMD.ARG_INSTANCE, port.getAbsoluteName()));
    }

    public static void buildBindPortToNetworkCommands(ShellCommands commands, String portAbsoluteName) {
        if (portAbsoluteName == null) {
            throw new IllegalArgumentException("port-name is null.");
        }
        commands.addCommand(translate(CMD.BIND_NETWORK_INSTANCE,
                CMD.ARG_INSTANCE, portAbsoluteName));
    }

    public static void buildBindPortToNetworkCommands(ShellCommands commands, PortDto port) {
        if (port == null) {
            throw new IllegalArgumentException("port is null.");
        }
        buildBindPortToNetworkCommands(commands, port.getAbsoluteName());
    }

    public static void buildUnbindPortFromNetworkCommands(ShellCommands commands, PortDto port) {
        buildUnbindPortFromNetworkCommands(commands, port.getAbsoluteName());
    }

    public static void buildUnbindPortFromNetworkCommands(ShellCommands commands, String portAbsoluteName) {
        commands.addCommand(translate(CMD.UNBIND_NETWORK_INSTANCE,
                CMD.ARG_INSTANCE, portAbsoluteName));
    }

    public static void buildConnectPortToNetworkIfCommands(ShellCommands cmd, PortDto networkIf, PortDto port) {
        InventoryBuilder.translate(cmd, CMD.CONNECT_NETWORK_INSTANCE_PORT,
                "_INSTANCE_", networkIf.getAbsoluteName(),
                "_PORT_", port.getAbsoluteName());
    }

    public static void buildConnectPortToNetworkIfCommands(ShellCommands cmd, String networkIf, String port) {
        InventoryBuilder.translate(cmd, CMD.CONNECT_NETWORK_INSTANCE_PORT,
                "_INSTANCE_", networkIf,
                "_PORT_", port);
    }

    public static void buildAddPortToBundleCommand(ShellCommands cmd, PortDto bundle, PortDto adding) {
        buildAddPortToBundleCommand(cmd, bundle.getAbsoluteName(), adding.getAbsoluteName());
    }

    public static void buildAddPortToBundleCommand(ShellCommands cmd, String bundlePort, String memberPort) {
        InventoryBuilder.translate(cmd, CMD.BUNDLE_PORT_INCLUDE,
                CMD.BUNDLE_PORT_ARG1_BUNDLE, bundlePort,
                CMD.BUNDLE_PORT_ARG2_MEMBER, memberPort);
    }

    public static void buildRemovePortFromBundleCommand(ShellCommands cmd, PortDto bundle, PortDto removing) {
        buildRemovePortFromBundleCommand(cmd, bundle.getAbsoluteName(), removing.getAbsoluteName());
    }

    public static void buildRemovePortFromBundleCommand(ShellCommands cmd, String bundlePort, String memberPort) {
        InventoryBuilder.translate(cmd, CMD.BUNDLE_PORT_EXCLUDE,
                CMD.BUNDLE_PORT_ARG1_BUNDLE, bundlePort,
                CMD.BUNDLE_PORT_ARG2_MEMBER, memberPort);
    }

    public static void buildDisconnectPortFromNetworkIfCommands(ShellCommands cmd, PortDto networkIf, PortDto port) {
        InventoryBuilder.translate(cmd, CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                "_INSTANCE_", networkIf.getAbsoluteName(),
                "_PORT_", port.getAbsoluteName());
    }

    public static void buildPortIpBindAsPrimaryCommand(ShellCommands cmd, String ipIfName) {
        buildAttributeSetOrReset(cmd, ATTR.ATTR_PRIMARY_IP, ipIfName);
    }

    public static void buildPortIpBindAsSecondaryCommand(ShellCommands cmd, String ipIfName) {
        buildAttributeSetOrReset(cmd, ATTR.ATTR_SECONDARY_IP, ipIfName);
    }

    public static void buildPortIpUnbindAsPrimaryCommand(ShellCommands cmd) {
        buildAttributeSetOrReset(cmd, ATTR.ATTR_PRIMARY_IP, null);
    }

    public static void buildPortIpUnbindAsSecondaryCommand(ShellCommands cmd) {
        buildAttributeSetOrReset(cmd, ATTR.ATTR_SECONDARY_IP, null);
    }

    public static void buildNodeElementDeletionCommands(ShellCommands commands, NodeElementDto ne) {
        commands.addCommand(translate(CMD.REMOVE_ELEMENT, "_TYPE_", ne.getObjectTypeName(), "_NAME_", ne.getName()));
    }

    public static void buildAddHopCommand(ShellCommands cmd, String type, String name) {
        translate(cmd, CMD.HOP_ADD, CMD.HOP_ADD_ARG1, type, CMD.HOP_ADD_ARG2, name);
    }

    public static void buildRemoveHopCommand(ShellCommands cmd) {
        translate(cmd, CMD.HOP_REMOVE);
    }

    public static void buildCustomerInfoCreationCommands(ShellCommands commands, String id, String idType) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null.");
        }
        String name = (idType == null ? "" : idType + ":") + id;
        commands.addCommand(translate(CMD.CUSTOMER_INFO_CREATE, CMD.CUSTOMER_INFO_CREATE_KEY_1, name));
        commands.addCommands(translateAttributes(ATTR.CUSTOMER_INFO_ID, id, ATTR.CUSTOMER_INFO_ID_TYPE, idType));
    }

    public static void buildCustomerInfoDeletionCommands(ShellCommands commands, String name) {
        if (name == null) {
            throw new IllegalArgumentException("id must not be null.");
        }
        buildAttributeSetOrReset(commands, ATTR.DELETE_FLAG, Boolean.TRUE.toString());
    }

    public static void buildAttributeSetOrReset(ShellCommands cmd, String attr, String value) {
        log().trace("setAttribute(): attr=" + attr + ", value=" + value);
        if (value == null) {
            String line = translate(CMD.ATTRIBUTE_RESET, CMD.ARG_ATTR, attr);
            cmd.addCommand(line);
        } else {
            checkString(attr, value);
            String line = translate(CMD.ATTRIBUTE_SET, CMD.ARG_ATTR, attr, CMD.ARG_VALUE, value);
            cmd.addCommand(line);
        }
    }

    public static void buildAttributeAdd(ShellCommands cmd, String attr, String value) {
        log().trace("buildAttributeAdd(): attr=" + attr + ", value=" + value);
        checkString(attr, value);
        String line = translate(CMD.ATTRIBUTE_ADD, CMD.ARG_ATTR, attr, CMD.ARG_VALUE, value);
        cmd.addCommand(line);
    }

    public static void buildAttributeRemove(ShellCommands cmd, String attr, String value) {
        log().trace("buildAttributeRemove(): attr=" + attr + ", value=" + value);
        checkString(attr, value);
        String line = translate(CMD.ATTRIBUTE_REMOVE, CMD.ARG_ATTR, attr, CMD.ARG_VALUE, value);
        cmd.addCommand(line);
    }

    public static void buildAttributePutOrRemove(ShellCommands cmd, String attr, String key, String value) {
        log().trace("setAttribute(): key=" + key + ", value=" + value);
        if (value == null) {
            String line = translate(CMD.ATTRIBUTE_REMOVE_MAP, CMD.ARG_ATTR, attr, CMD.ARG_KEY, key);
            cmd.addCommand(line);
        } else {
            checkString(key, value);
            String line = translate(CMD.ATTRIBUTE_PUT_MAP, CMD.ARG_ATTR, attr, CMD.ARG_KEY, key, CMD.ARG_VALUE, value);
            cmd.addCommand(line);
        }
    }

    public static void buildCollectionAttributeSetOnCurrentContextCommands(ShellCommands commands,
                                                                           String key, Collection<String> values) {
        for (String value : values) {
            commands.addCommand(translateAddCollectionAttribute(key, value));
        }
    }

    public static void buildCollectionAttributeUpdateCommands(ShellCommands commands, NaefDto dto,
                                                              String key, Collection<String> values) {
        if (dto != null && !DtoUtil.isSupportedAttribute(dto, key)) {
            return;
        }
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
    }

    public static void buildCollectionAttributeRemoveCommands(ShellCommands commands, NaefDto dto,
                                                              String key) {
        if (!DtoUtil.isSupportedAttribute(dto, key)) {
            return;
        }
        List<String> items = DtoUtil.getStringList(dto, key);
        for (String item : items) {
            commands.addCommand(translateRemoveCollectionAttribute(key, item));
        }
    }

    public static void buildMapAttributeRemoveCommands(ShellCommands commands, NaefDto dto,
                                                       String key) {
        if (!DtoUtil.isSupportedAttribute(dto, key)) {
            return;
        }
        Map<String, String> items = DtoUtil.getStringMap(dto, key);
        for (Map.Entry<String, String> entry : items.entrySet()) {
            buildAttributePutOrRemove(commands, key, entry.getKey(), null);
        }
    }

    public static void buildAttributeUpdateCommand(ShellCommands commands, NaefDto dto,
                                                   Map<String, String> attributes) {
        buildAttributeUpdateCommand(commands, dto, attributes, true);
    }

    public static void buildAttributeUpdateCommand(ShellCommands commands, NaefDto dto,
                                                   Map<String, String> attributes, boolean resetOnNotUse) {
        AttributePolicy policy = CoreConfiguration.getInstance().getAttributePolicy();
        Set<String> supportedAttributes = DtoUtil.getSupportedAttributeNames(dto);
        boolean isResetState = policy.isToBeInitializeState(dto, attributes);
        if (resetOnNotUse && isResetState) {
            commands.addCommand("# reset attributes");
            for (String attr : supportedAttributes) {
                if (policy.isCollectionAttribute(attr)) {
                    buildCollectionAttributeRemoveCommands(commands, dto, attr);
                } else if (policy.isMapAttribute(attr)) {
                    buildMapAttributeRemoveCommands(commands, dto, attr);
                } else {
                    buildAttributeCommandOnNotUse(commands, attr, attributes);
                }
            }
        } else {
            for (String attrName : attributes.keySet()) {
                if (policy.isExcludeAttr(attrName)) {
                    continue;
                }
                String before = DtoUtil.getStringOrNull(dto, attrName);
                String after = Util.nullToString(attributes.get(attrName));
                if (Util.hasDiff(before, after)) {
                    if (after != null) {
                        buildAttributeSetOrReset(commands, attrName, after);
                    } else {
                        buildAttributeSetOrReset(commands, attrName, null);
                    }
                }
            }
        }
    }

    public static void buildSetAttributeUpdateCommand(ShellCommands cmd, NaefDto dto,
                                                      Map<String, Map<String, DiffType>> attributes) {
        buildSetAttributeUpdateCommand(cmd, attributes);
    }

    public static void buildSetAttributeUpdateCommand(ShellCommands cmd, Map<String, Map<String, DiffType>> attributes) {
        for (Map.Entry<String, Map<String, DiffType>> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            Map<String, DiffType> values = entry.getValue();
            buildSetAttributeUpdateCommand(cmd, attrName, values);
        }
    }

    public static void buildSetAttributeUpdateCommand(ShellCommands cmd, String attrName, Map<String, DiffType> values) {
        AttributePolicy policy = CoreConfiguration.getInstance().getAttributePolicy();
        if (policy.isExcludeAttr(attrName)) {
            return;
        }
        for (Map.Entry<String, DiffType> entry : values.entrySet()) {
            String value = entry.getKey();
            DiffType type = entry.getValue();
            if (type == null) {
                throw new IllegalStateException("diff-type is null. [" + attrName + "]=[" + value + "]");
            }
            switch (type) {
                case ADD:
                    buildAttributeAdd(cmd, attrName, value);
                    break;
                case KEEP:
                    break;
                case REMOVE:
                    buildAttributeRemove(cmd, attrName, value);
                    break;
            }
        }
    }

    public static void buildAttributeSetOnCurrentContextCommands(ShellCommands commands,
                                                                 Map<String, String> attributes) {
        buildAttributeSetOnCurrentContextCommands(commands, attributes, true);
    }

    public static void buildAttributeSetOnCurrentContextCommands(ShellCommands commands,
                                                                 Map<String, String> attributes, boolean resetOnNotUse) {
        AttributePolicy policy = CoreConfiguration.getInstance().getAttributePolicy();

        if (resetOnNotUse && policy.isToBeInitializeState(null, attributes)) {
            for (String attr : attributes.keySet()) {
                buildAttributeCommandOnNotUse(commands, attr, attributes);
            }
        } else {
            for (String key : attributes.keySet()) {
                if (policy.isExcludeAttr(key)) {
                    continue;
                }
                String value = Util.stringToNull(attributes.get(key));
                buildAttributeSetOrReset(commands, key, value);
            }
        }
    }

    public static void buildAttributeCopy(ShellCommands cmd, String attr, EntityDto source) {
        if (source == null || attr == null) {
            return;
        }
        String value = DtoUtil.getStringOrNull(source, attr);
        buildAttributeSetOrReset(cmd, attr, value);
    }

    public static void buildAttributeCommandOnNotUse(ShellCommands commands, String attr,
                                                     Map<String, String> attributes) {
        AttributePolicy policy = CoreConfiguration.getInstance().getAttributePolicy();
        if (policy.isPersistentAttribute(attr)) {
            boolean hasAttr = attributes.containsKey(attr);
            if (hasAttr) {
                String value = attributes.get(attr);
                buildAttributeSetOrReset(commands, attr, value);
            }
        } else if (policy.isExcludeAttr(attr)) {
            return;
        } else if (policy.isCollectionAttribute(attr)) {
            return;
        } else {
            buildAttributeSetOrReset(commands, attr, null);
        }
    }

    public static List<String> translateAttributes(String... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < args.length; i = i + 2) {
            String key = args[i];
            String value = args[i + 1];
            String line;
            if (value == null) {
                checkString(key, null);
                log().trace("translateAttribute(): key=" + key + ", value=null");
                line = translate(CMD.ATTRIBUTE_RESET, CMD.ARG_ATTR, key);
            } else {
                checkString(key, value);
                log().trace("translateAttribute(): key=" + key + ", value=" + value);
                line = translate(CMD.ATTRIBUTE_SET, CMD.ARG_ATTR, key, CMD.ARG_VALUE, value);
            }
            result.add(line);
        }
        return result;
    }

    public static String translate(boolean noCheck, String template, String... args) {
        if (template == null) {
            return null;
        }
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        String line = template;
        for (int i = 0; i < args.length; i = i + 2) {
            String key = Util.escapeForCommand(args[i]);
            String value = Util.escapeForCommand(args[i + 1]);
            if (!noCheck) {
                checkString(key, value);
            }
            log().trace("translate(): " + line + ", key=" + key + ", value=" + value);
            line = line.replace(key, value);
        }
        return line;
    }

    public static String translate(String template, String... args) {
        return translate(false, template, args);
    }

    public static void translate(ShellCommands commands, String template, String... args) {
        String line = translate(template, args);
        commands.addCommand(line);
    }

    public static List<String> translate(List<String> lines, String... args) {
        if (lines == null) {
            return null;
        }
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        List<String> result = new ArrayList<String>();
        for (String line : lines) {
            String s = translate(line, args);
            if (s == null) {
                continue;
            }
            result.add(s);
        }
        return result;
    }

    public static String translateAttribute(String attr, String value) {
        return translate(CMD.ATTRIBUTE_SET, CMD.ARG_ATTR, attr, CMD.ARG_VALUE, value);
    }

    public static void translateAttribute(ShellCommands cmd, String attr, String value) {
        translate(cmd, CMD.ATTRIBUTE_SET, CMD.ARG_ATTR, attr, CMD.ARG_VALUE, value);
    }

    public static String translateResetAttribute(String attr) {
        return translate(CMD.ATTRIBUTE_RESET, CMD.ARG_ATTR, attr);
    }

    public static void translateResetAttribute(ShellCommands cmd, String attr) {
        translate(cmd, CMD.ATTRIBUTE_RESET, CMD.ARG_ATTR, attr);
    }

    public static String translateAddCollectionAttribute(String... args) {
        return translate(CMD.ATTRIBUTE_ADD, args);
    }

    public static String translateAddCollectionAttribute(String attr, String value) {
        return translate(CMD.ATTRIBUTE_ADD, CMD.ARG_ATTR, attr, CMD.ARG_VALUE, value);
    }

    public static void translateAddCollectionAttribute(ShellCommands cmd, String attr, String value) {
        cmd.addCommand(translateAddCollectionAttribute(attr, value));
    }

    public static String translateRemoveCollectionAttribute(String... args) {
        return translate(CMD.ATTRIBUTE_REMOVE, args);
    }

    public static String translateRemoveCollectionAttribute(String attr, String value) {
        return translate(CMD.ATTRIBUTE_REMOVE, CMD.ARG_ATTR, attr, CMD.ARG_VALUE, value);
    }

    public static void translateRemoveCollectionAttribute(ShellCommands cmd, String attr, String value) {
        cmd.addCommand(translateRemoveCollectionAttribute(attr, value));
    }

    public static String getInventoryDateString(Date date) {
        if (date == null) {
            return null;
        }
        return DtoUtil.getMvoDateFormat().format(date);
    }

    public static void buildMvoVersionCheckCommand(ShellCommands commands, NaefDto dto) {
        commands.addCommand(getMvoVersionCheckCommand(dto));
    }

    public static String getMvoVersionCheckCommand(NaefDto dto) {
        String cmd = "assert-mvo-version " + DtoUtil.getMvoId(dto).toString() + " " + DtoUtil.getMvoTimestamp(dto).getIdString();
        return cmd;
    }

    public static String getMvoDescription(EntityDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("# mvo-id:");
        sb.append(DtoUtil.getMvoId(dto).toString());
        sb.append(" ");
        sb.append(dto.getClass().getName());
        return sb.toString();
    }

    public static boolean isAdded(NaefDto before, NaefDto after) {
        if (after == null) {
            return false;
        }
        if (before != null && !DtoUtil.mvoEquals(before, after)) {
            return true;
        } else if (before == null) {
            return true;
        }
        return false;
    }

    public static boolean isRemoved(NaefDto before, NaefDto after) {
        if (before == null) {
            return false;
        }
        if (after != null && !DtoUtil.mvoEquals(before, after)) {
            return true;
        } else if (after == null) {
            return true;
        }
        return false;
    }

    private static void checkString(String key, String value) {
    }

    public static void checkIDNotUsedAndConfigured(List<? extends Number> usedIDs, List<? extends Number> configuredIDs) {
        StringBuilder sb = new StringBuilder();
        if (usedIDs.size() > 0) {
            sb.append("There are IDs that are not free. ID = [").append(Util.getConcatinatedRange(usedIDs)).append("]");
        }
        if (configuredIDs.size() > 0) {
            sb.append("IDs that have not cleared the configuration port remain. ID = [").append(Util.getConcatinatedRange(configuredIDs)).append("]");
        }
        if (sb.length() > 0) {
            throw new IllegalStateException(sb.toString());
        }
    }

    public static void buildSystemUserCreationCommand(ShellCommands cmd, String userName) {
        if (userName == null) {
            throw new IllegalArgumentException("no userName.");
        }
        translate(cmd, CMD.SYSTEM_USER_CREATE, CMD.ARG_NAME, userName);
    }
}