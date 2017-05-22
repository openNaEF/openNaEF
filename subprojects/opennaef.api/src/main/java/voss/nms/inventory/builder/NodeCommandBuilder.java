package voss.nms.inventory.builder;

import naef.dto.LocationDto;
import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.mvo.Node.VirtualizationHostedType;
import tef.skelton.dto.EntityDto.Desc;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.constant.ModelConstant;
import voss.core.server.database.ATTR;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.LocationUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.VirtualNodeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class NodeCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NodeDto node;
    private NodeDto hyperVisor;
    private final NodeDto originalHyperVisor;
    private final String oldNodeName;
    private String nodeName = null;
    private boolean createChassis = true;
    private boolean useMetadata = false;
    private String vendorName = null;
    private String nodeTypeName = null;
    private String managementIpAddress = null;
    private String snmpMode;
    private String snmpCommunity;
    private String purpose;
    private String consoleType;
    private String adminPassword;
    private String adminUser;
    private String externalInventoryDBID;
    private String externalInventoryDBStatus;
    private String loginUser;
    private String loginPassword;
    private String osType;
    private String osVersion;
    private String note;
    private final List<String> zoneList = new ArrayList<String>();
    private final List<String> originalZoneList = new ArrayList<String>();
    private boolean isVirtualNode = false;
    private boolean isVMHostingEnabled = true;
    private boolean cascadeDelete = false;

    public NodeCommandBuilder(String editorName) {
        this(null, editorName);
    }

    public NodeCommandBuilder(NodeDto node, String editorName) {
        super(NodeDto.class, node, editorName);
        setConstraint(NodeDto.class);
        this.node = node;
        if (node != null) {
            checkHousingStatus(node);
            this.oldNodeName = node.getName();
            this.nodeName = node.getName();
            String _zones = DtoUtil.getStringOrNull(this.node, MPLSNMS_ATTR.ZONE_LIST);
            this.zoneList.addAll(toZoneList(_zones));
            this.originalZoneList.addAll(this.zoneList);
            this.hyperVisor = VirtualNodeUtil.getVirtualizationHostNode(node);
            this.isVirtualNode = DtoUtil.getBoolean(this.node, MPLSNMS_ATTR.VIRTUAL_NODE);
            this.isVMHostingEnabled = DtoUtil.getBoolean(this.node, ATTR.ATTR_VIRTUALIZATION_HOSTING_ENABLED);
            this.originalHyperVisor = this.node.getVirtualizationHostNode();
        } else {
            this.oldNodeName = null;
            this.nodeName = null;
            this.originalHyperVisor = null;
        }
    }

    private void checkHousingStatus(NodeDto node) {
        if (VirtualizationHostedType.MULTI_HOST == VirtualNodeUtil.getVirtualizationHostedType(node)) {
            throw new IllegalStateException("MULTI_HOST can not be handled by this builder.");
        }
    }

    public void setHyperVisor(NodeDto node) {
        if (DtoUtil.mvoEquals(this.hyperVisor, node)) {
            return;
        }
        recordChange("HyperVisor", this.hyperVisor, node);
        this.hyperVisor = node;
    }

    public static List<String> toZoneList(String s) {
        List<String> result = new ArrayList<String>();
        if (s == null) {
            return result;
        }
        for (String e : s.split("\t")) {
            e = e.trim();
            result.add(e);
        }
        return result;
    }

    public static String toZones(List<String> list) {
        if (list == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append("\t");
            }
            s = s.trim();
            sb.append(s);
        }
        return sb.toString();
    }

    public void setCascadeDelete(boolean value) {
        this.cascadeDelete = value;
    }

    public void setCreateChassis(boolean value) {
        this.createChassis = value;
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        checkMandatory();
        checkVirtualizationConfiguration();
        if (this.node != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.node == null) {
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            if (this.useMetadata) {
                buildCommandsUsingMetadata();
            } else {
                buildCommandsUsingAttribute();
            }
        } else {
            InventoryBuilder.changeContext(this.cmd, node);
        }
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
        if (!Util.isSameList(this.originalZoneList, this.zoneList)) {
            String zones = toZones(this.zoneList);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.ZONE_LIST, zones);
        }
        this.cmd.addLastEditCommands();
        maintainHyperVisor();
        if (this.oldNodeName != null && !this.oldNodeName.equals(this.nodeName)) {
            InventoryBuilder.buildRenameCommands(this.cmd, this.nodeName);
        }
        return BuildResult.SUCCESS;
    }

    private void maintainHyperVisor() {
        if (!this.isVirtualNode) {
            return;
        }
        if (this.originalHyperVisor == null) {
            if (this.hyperVisor == null) {
                throw new IllegalArgumentException("no hyper-visor.");
            } else {
                InventoryBuilder.buildAttributeSetOrReset(cmd,
                        ATTR.ATTR_VIRTUALIZATION_HOSTED_TYPE, VirtualizationHostedType.SINGLE_HOST.name());
                InventoryBuilder.buildAttributeAdd(cmd,
                        ATTR.ATTR_VIRTUALIZATION_HOST_NODES, this.hyperVisor.getAbsoluteName());
            }
        } else {
            if (this.hyperVisor == null) {
                InventoryBuilder.buildAttributeRemove(cmd,
                        ATTR.ATTR_VIRTUALIZATION_HOST_NODES, this.hyperVisor.getAbsoluteName());
                InventoryBuilder.buildAttributeSetOrReset(cmd,
                        ATTR.ATTR_VIRTUALIZATION_HOSTED_TYPE, null);
            } else if (DtoUtil.mvoEquals(this.originalHyperVisor, this.hyperVisor)) {
            } else {
                InventoryBuilder.buildAttributeRemove(cmd,
                        ATTR.ATTR_VIRTUALIZATION_HOST_NODES, this.originalHyperVisor.getAbsoluteName());
                InventoryBuilder.buildAttributeAdd(cmd,
                        ATTR.ATTR_VIRTUALIZATION_HOST_NODES, this.hyperVisor.getAbsoluteName());
            }
        }
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        checkDeletable();
        if (cascadeDelete) {
            removeSubElements(this.node);
        }
        removeAliases();
        InventoryBuilder.changeContext(this.cmd, node);
        setValue(MPLSNMS_ATTR.NODE_LOCATION, ModelConstant.LOCATION_TRASH);
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes);
        String newName = getNameForDelete(this.node.getName());
        InventoryBuilder.buildRenameCommands(this.cmd, newName);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.DELETE_FLAG, Boolean.TRUE.toString());
        return BuildResult.SUCCESS;
    }

    private void removeSubElements(NodeElementDto parent) {
        for (NodeElementDto ne : parent.getSubElements()) {
            if (ne instanceof PortDto) {
                PortDto p = (PortDto) ne;
                if (p.getNetworks().size() > 0) {
                    throw new IllegalStateException("port is used by networks: " + p.getAbsoluteName());
                }
            }
            removeSubElements(ne);
            InventoryBuilder.changeContext(cmd, ne.getOwner());
            InventoryBuilder.buildNodeElementDeletionCommands(cmd, ne);
        }
    }

    private void removeAliases() {
        for (PortDto port : this.node.getPorts()) {
            if (!port.isAlias()) {
                continue;
            }
            InventoryBuilder.changeContext(this.cmd, port);
            InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_ALIAS_SOURCE, null);
            InventoryBuilder.changeContext(this.cmd, port.getOwner());
            InventoryBuilder.buildNodeElementDeletionCommands(this.cmd, port);
        }
    }

    private void buildCommandsUsingMetadata() throws IOException, InventoryException {
        SimpleNodeBuilder.buildNodeCreationCommands(cmd, nodeName, vendorName, nodeTypeName);
    }

    private void buildCommandsUsingAttribute() {
        InventoryBuilder.translate(cmd, CMD.CREATE_NODE, CMD.CREATE_NODE_KEY_1, nodeName);
        if (!createChassis) {
            return;
        }
        InventoryBuilder.translate(cmd, CMD.NEW_HARDWARE,
                CMD.NEW_HARDWARE_KEY_1, ATTR.TYPE_CHASSIS,
                CMD.NEW_HARDWARE_KEY_2, "");
        this.cmd.addCommand(CMD.CONTEXT_DOWN);
    }

    private void checkMandatory() {
        if (this.node == null) {
            if (this.nodeName == null) {
                throw new IllegalStateException();
            }
        } else {
        }
    }

    private void checkVirtualizationConfiguration() {
        if (this.isVirtualNode) {
            if (this.hyperVisor == null) {
                throw new IllegalStateException("HyperVisor of the virtual node is not specified.");
            }
        } else {
            if (this.hyperVisor != null) {
                throw new IllegalStateException("It is not possible to set hyperVisor on non-virtual nodes.");
            }
        }
    }

    private void checkDeletable() {
        if (this.node == null) {
            throw new IllegalStateException("node is null.");
        }
        checkGuestFree();
        checkAliasFree();
    }

    private void checkGuestFree() {
        if (VirtualNodeUtil.getVirtualizationGuestNodes(this.node).size() > 0) {
            throw new IllegalStateException("A virtual node exists on this node.");
        }
    }

    private void checkAliasFree() {
        for (PortDto port : this.node.getPorts()) {
            Set<Desc<PortDto>> refs = port.get(PortDto.ExtAttr.ALIASES);
            if (refs.size() > 0) {
                log().debug("alias port found: " + DtoUtil.toDebugString(port) + " " + DtoUtil.toStringOrMvoString(refs));
                throw new IllegalStateException("There is a virtual node referring to this port: " + NameUtil.getIfName(port));
            }
        }
    }

    public NodeDto getNode() {
        return this.node;
    }

    public void setNodeName(String nodeName) {
        String oldNodeName = null;
        if (this.node != null) {
            if (this.node.getName().equals(nodeName)) {
                return;
            }
            oldNodeName = this.node.getName();
        }
        recordChange(MPLSNMS_ATTR.NAME, oldNodeName, nodeName);
        this.nodeName = nodeName;
    }

    public void setMetadata(String vendor, String nodeType) {
        this.useMetadata = true;
        this.vendorName = vendor;
        this.nodeTypeName = nodeType;
        setValue(MPLSNMS_ATTR.VENDOR_NAME, vendor);
        setValue(MPLSNMS_ATTR.NODE_TYPE, nodeType);
    }

    public void setVendor(String vendor) {
        this.vendorName = vendor;
        setValue(MPLSNMS_ATTR.VENDOR_NAME, this.vendorName);
    }

    public void setNodeType(String nodeType) {
        this.nodeTypeName = nodeType;
        setValue(MPLSNMS_ATTR.NODE_TYPE, this.nodeTypeName);
    }

    public void setLocation(String name) {
        if (name == null) {
            throw new IllegalArgumentException("null location not allowed.");
        } else if (!name.startsWith("location;")) {
            throw new IllegalArgumentException("Location name must be absolute name.");
        }
        LocationDto loc = LocationUtil.getLocation(node);
        if (loc == null || !loc.getAbsoluteName().equals(name)) {
            setValue(MPLSNMS_ATTR.NODE_LOCATION, name);
        }
    }

    public void setIpAddress(String managementIpAddress) {
        this.managementIpAddress = managementIpAddress;
        setValue(MPLSNMS_ATTR.MANAGEMENT_IP, this.managementIpAddress);
    }

    public void setSnmpMode(String snmpMode) {
        this.snmpMode = snmpMode;
        setValue(MPLSNMS_ATTR.SNMP_MODE, this.snmpMode);
    }

    public void setSnmpCommunityRO(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
        setValue(MPLSNMS_ATTR.SNMP_COMMUNITY, this.snmpCommunity);
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
        setValue(MPLSNMS_ATTR.PURPOSE, this.purpose);
    }

    public void setCliMode(String consoleType) {
        this.consoleType = consoleType;
        setValue(MPLSNMS_ATTR.CLI_MODE, this.consoleType);
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
        setValue(MPLSNMS_ATTR.ADMIN_PASSWORD, this.adminPassword);
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
        setValue(MPLSNMS_ATTR.ADMIN_ACCOUNT, this.adminUser);
    }

    public void setExternalInventoryDBID(String externalInventoryDBID) {
        this.externalInventoryDBID = externalInventoryDBID;
        setValue(MPLSNMS_ATTR.EXTERNAL_INVENTORY_DB_ID, this.externalInventoryDBID);
    }

    public void setExternalInventoryDBStatus(String externalInventoryDBStatus) {
        this.externalInventoryDBStatus = externalInventoryDBStatus;
        setValue(MPLSNMS_ATTR.EXTERNAL_INVENTORY_DB_STATUS, this.externalInventoryDBStatus);
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
        setValue(MPLSNMS_ATTR.TELNET_ACCOUNT, this.loginUser);
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
        setValue(MPLSNMS_ATTR.TELNET_PASSWORD, this.loginPassword);
    }

    public void setOsType(String osType) {
        this.osType = osType;
        setValue(MPLSNMS_ATTR.OS_TYPE, this.osType);
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
        setValue(MPLSNMS_ATTR.OS_VERSION, this.osVersion);
    }

    public void setNote(String note) {
        this.note = note;
        setValue(MPLSNMS_ATTR.NOTE, this.note);
    }

    public void setDiffTarget(String diffTarget) {
        setValue(ATTR.DIFF_TARGET, diffTarget);
    }

    public String getObjectType() {
        return DiffObjectType.NODE.getCaption();
    }

    public void setVirtualNode(boolean virtual) {
        this.isVirtualNode = virtual;
        setValue(MPLSNMS_ATTR.VIRTUAL_NODE, virtual);
    }

    public boolean isVirtualNode() {
        return this.isVirtualNode;
    }

    public void setVmHostingEnabled(boolean enabled) {
        this.isVMHostingEnabled = enabled;
        setValue(ATTR.ATTR_VIRTUALIZATION_HOSTING_ENABLED, enabled);
    }

    public boolean isVmHostingEnabled() {
        return this.isVMHostingEnabled;
    }

    public void setZoneList(List<String> zones) {
        if (Util.isSameList(this.zoneList, zones)) {
            return;
        }
        recordChange("Zone", this.zoneList, zones);
        this.zoneList.clear();
        this.zoneList.addAll(zones);
    }

    public void clearZone() {
        if (this.zoneList.size() == 0) {
            return;
        }
        this.zoneList.clear();
        recordChange("Zone", this.zoneList, null);
    }

    public void addZone(String zone) {
        if (!zoneList.contains(zone)) {
            zoneList.add(zone);
            recordChange("Zone", null, zone);
        }
    }

    public void addZones(List<String> zones) {
        for (String zone : zones) {
            addZone(zone);
        }
    }

    public List<String> getZoneList() {
        return this.zoneList;
    }
}