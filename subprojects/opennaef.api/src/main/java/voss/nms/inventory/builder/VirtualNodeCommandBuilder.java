package voss.nms.inventory.builder;

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
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.VirtualNodeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class VirtualNodeCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final NodeDto node;
    private NodeDto hyperVisor;
    private final NodeDto originalHyperVisor;
    private final String oldGuestNodeName;
    private final String oldNodeName;
    private String guestNodeName = null;
    private String vendorName = null;
    private String nodeTypeName = null;
    private String managementIpAddress = null;
    private String snmpMode;
    private String snmpCommunity;
    private String purpose;
    private String consoleType;
    private String adminPassword;
    private String adminUser;
    private String loginUser;
    private String loginPassword;
    private String osType;
    private String osVersion;
    private String note;
    private final List<String> zoneList = new ArrayList<String>();
    private final List<String> originalZoneList = new ArrayList<String>();
    private boolean cascadeDelete = false;

    public VirtualNodeCommandBuilder(String editorName) {
        this(null, editorName);
    }

    public VirtualNodeCommandBuilder(NodeDto node, String editorName) {
        super(NodeDto.class, node, editorName);
        setConstraint(NodeDto.class);
        this.node = node;
        if (node != null) {
            checkHousingStatus(node);
            this.oldNodeName = node.getName();
            this.oldGuestNodeName = VirtualNodeUtil.getGuestNodeName(node);
            this.guestNodeName = this.oldGuestNodeName;
            String _zones = DtoUtil.getStringOrNull(this.node, MPLSNMS_ATTR.ZONE_LIST);
            this.zoneList.addAll(toZoneList(_zones));
            this.originalZoneList.addAll(this.zoneList);
            this.hyperVisor = VirtualNodeUtil.getVirtualizationHostNode(node);
            if (this.hyperVisor == null) {
                throw new IllegalStateException("not a virtual node: " + DtoUtil.toDebugString(node));
            }
            this.originalHyperVisor = this.hyperVisor;
        } else {
            this.oldGuestNodeName = null;
            this.guestNodeName = null;
            this.oldNodeName = null;
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

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException {
        checkMandatory();
        checkVirtualizationConfiguration();
        if (this.node != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.node == null) {
            if (this.hyperVisor == null) {
                throw new IllegalArgumentException("hyper-visor is null.");
            }
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            InventoryBuilder.translate(cmd, CMD.CREATE_NODE, CMD.CREATE_NODE_KEY_1, getNodeName());
            InventoryBuilder.buildAttributeCopy(cmd, MPLSNMS_ATTR.NODE_LOCATION, this.hyperVisor);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.VIRTUAL_NODE, Boolean.TRUE.toString());
        } else {
            InventoryBuilder.changeContext(this.cmd, node);
        }
        InventoryBuilder.buildAttributeCopy(cmd, MPLSNMS_ATTR.NODE_LOCATION, this.hyperVisor);
        InventoryBuilder.buildAttributeSetOnCurrentContextCommands(this.cmd, this.attributes, false);
        if (!Util.isSameList(this.originalZoneList, this.zoneList)) {
            String zones = toZones(this.zoneList);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.ZONE_LIST, zones);
        }
        this.cmd.addLastEditCommands();
        maintainHyperVisor();

        if (this.oldNodeName != null && !this.oldNodeName.equals(getNodeName())) {
            InventoryBuilder.buildRenameCommands(this.cmd, getNodeName());
        }
        return BuildResult.SUCCESS;
    }

    private void maintainHyperVisor() {
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
        if (this.hyperVisor != null) {
            InventoryBuilder.changeContext(cmd, this.node);
            InventoryBuilder.buildAttributeRemove(cmd, ATTR.ATTR_VIRTUALIZATION_HOST_NODES, this.hyperVisor.getAbsoluteName());
        }
    }

    private void checkMandatory() {
        if (this.node == null) {
            if (this.guestNodeName == null) {
                throw new IllegalStateException();
            }
        } else {
        }
    }

    private void checkVirtualizationConfiguration() {
        if (this.hyperVisor == null) {
            throw new IllegalStateException("HyperVisor of the virtual node is not specified.");
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

    public NodeDto getHyperVisor() {
        return this.hyperVisor;
    }

    public NodeDto getVirtualNode() {
        return this.node;
    }

    public void setGuestNodeName(String nodeName) {
        if (this.guestNodeName != null && this.guestNodeName.equals(nodeName)) {
            return;
        }
        recordChange(MPLSNMS_ATTR.NAME, this.oldGuestNodeName, nodeName);
        this.guestNodeName = nodeName;
    }

    public String getNodeName() {
        if (this.hyperVisor == null) {
            throw new IllegalStateException();
        }
        return AbsoluteNameFactory.getVirtualNodeName(this.hyperVisor.getName(), this.guestNodeName);
    }

    public void setVendor(String vendor) {
        this.vendorName = vendor;
        setValue(MPLSNMS_ATTR.VENDOR_NAME, this.vendorName);
    }

    public void setNodeType(String nodeType) {
        this.nodeTypeName = nodeType;
        setValue(MPLSNMS_ATTR.NODE_TYPE, this.nodeTypeName);
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