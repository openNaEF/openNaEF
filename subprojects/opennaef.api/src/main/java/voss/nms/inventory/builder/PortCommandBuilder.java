package voss.nms.inventory.builder;

import naef.dto.NaefDto;
import naef.dto.NetworkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.dto.EntityDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.database.ATTR;
import voss.core.server.database.CONSTANTS;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.naef.IfNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.IpSubnetAddressUtil;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public abstract class PortCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(PortCommandBuilder.class);
    private final PortDto port;
    private final NodeDto node;
    private String ifName;
    private String ipIfName;
    protected Boolean implicit = Boolean.FALSE;
    private String ipAddress;
    private String maskLength;
    private IpIfDto ipIf;
    private final IpIfDto originalIpIf;
    private String vpnName;
    private String originalVpnName;
    private String vpnOwnerName;
    private VrfIfDto originalVpnOwner;
    private final Set<String> ipAttributeNames = new HashSet<String>();
    private boolean associateIpSubnetAddress = true;
    private boolean allowIpDuplication = false;
    private boolean needIP = true;
    private boolean independentIP = false;
    protected boolean preCheckEnable = true;
    private String targetPortVariableName;

    public PortCommandBuilder(Class<? extends NaefDto> required, NodeDto node, NaefDto target, String editorName) {
        super(required, target, editorName);
        if (!PortDto.class.isAssignableFrom(required)) {
            throw new IllegalArgumentException("required-class must be sub-type of PortDto.");
        }
        if (target != null && !(target instanceof PortDto)) {
            throw new IllegalArgumentException("target is not sub-type of port." + target);
        }
        this.node = node;
        if (target != null) {
            this.port = (PortDto) target;
            this.ipIf = this.port.getPrimaryIpIf();
            if (this.ipIf != null && this.ipIf.getOwner() instanceof VrfIfDto) {
                this.originalVpnOwner = (VrfIfDto) this.ipIf.getOwner();
                this.originalVpnName = this.originalVpnOwner.getName();
                this.vpnOwnerName = this.originalVpnOwner.getAbsoluteName();
                this.vpnName = this.originalVpnName;
            } else {
                this.vpnName = DtoUtil.getStringOrNull(this.ipIf, ATTR.VPN_PREFIX);
            }
            this.ifName = DtoUtil.getIfName(this.port);
            this.originalIpIf = this.ipIf;
            this.ipAddress = DtoUtil.getStringOrNull(this.originalIpIf, MPLSNMS_ATTR.IP_ADDRESS);
            this.maskLength = DtoUtil.getStringOrNull(this.originalIpIf, MPLSNMS_ATTR.MASK_LENGTH);
            this.ipIfName = DtoUtil.getStringOrNull(this.originalIpIf, MPLSNMS_ATTR.IFNAME);
            if (Util.equals(this.ipAddress, this.ipIfName)) {
                this.ipIfName = null;
            }
        } else {
            this.port = null;
            this.originalIpIf = null;
            this.vpnName = null;
        }
    }

    public void setVpnOwnerName(String vrfName, String vrfIfName) {
        this.vpnName = vrfName;
        this.vpnOwnerName = vrfIfName;
    }

    public void setVpnOwner(VrfIfDto vrf) {
        this.vpnName = vrf.getName();
        this.vpnOwnerName = vrf.getAbsoluteName();
    }

    public boolean isAllowIpDuplication() {
        return this.allowIpDuplication;
    }

    public void setAllowIpDuplication(boolean allowIpDuplication) {
        this.allowIpDuplication = allowIpDuplication;
    }

    public boolean isAssociateIpSubnetAddress() {
        return this.associateIpSubnetAddress;
    }

    public void setAssociateIpSubnetAddress(boolean value) {
        this.associateIpSubnetAddress = value;
    }

    public void setIfName(String ifName) {
        if (ifName == null) {
            throw new IllegalStateException("ifName is mandatory.");
        }
        if (Util.stringToNull(ifName) != null && this.node != null) {
            PortDto portByIfName = NodeUtil.getPortByIfName(this.node, ifName);
            if (portByIfName != null && (!DtoUtil.mvoEquals(portByIfName, target) || target == null)) {
                throw new IllegalStateException("Duplicated ifName.");
            }
        }
        this.ifName = ifName;
        setValue(MPLSNMS_ATTR.IFNAME, ifName);
    }

    public void setConfigName(String configName) {
        if (configName == null) {
            throw new IllegalStateException("configName is mandatory.");
        }
        if (Util.stringToNull(ATTR.CONFIG_NAME) != null && this.node != null) {
            PortDto portByConfigName = NodeUtil.getPortByConfigName(this.node, configName);
            if (portByConfigName != null && (!DtoUtil.mvoEquals(portByConfigName, target) || target == null)) {
                throw new IllegalStateException("Duplicated configName.");
            }
        }
        setValue(MPLSNMS_ATTR.CONFIG_NAME, configName);
    }

    public void setImplicitValue(String value) {
        Boolean bool = Boolean.valueOf(value);
        setImplicit(bool);
    }

    public void setImplicit(Boolean value) {
        if (value == null || value.equals(Boolean.FALSE)) {
            setValue(ATTR.IMPLICIT, (Boolean) null);
        } else {
            setValue(ATTR.IMPLICIT, Boolean.TRUE);
        }
    }

    public void setIpNeeds(boolean value) {
        this.needIP = value;
    }

    public void setIpIf(IpIfDto ipIf) {
        if (DtoUtil.mvoEquals(ipIf, this.originalIpIf)) {
            return;
        }
        this.ipIf = ipIf;
        String vpnPrefix = DtoUtil.getStringOrNull(ipIf, ATTR.VPN_PREFIX);
        String ipAddress = DtoUtil.getStringOrNull(ipIf, MPLSNMS_ATTR.IP_ADDRESS);
        String subnetMask = DtoUtil.getStringOrNull(ipIf, MPLSNMS_ATTR.MASK_LENGTH);
        setIpAddressAttribute(vpnPrefix, ipAddress, subnetMask);
    }

    public void setNewIpAddress(String vpnPrefix, String ip, String maskLen, boolean moveEnable) {
        vpnPrefix = Util.stringToNull(vpnPrefix);
        ip = Util.stringToNull(ip);
        maskLen = Util.stringToNull(maskLen);
        if (ip == null) {
            this.ipIf = null;
            setIpAddressAttribute(null, null, null);
            log().debug("setNewIpAddress: clear ip-address.");
            return;
        }
        setIpAddressAttribute(vpnPrefix, ip, maskLen);
        if (this.ipIf != null && !isIpChanged(vpnPrefix, ip, maskLen)) {
            log().debug("setNewIpAddress: keep ip-if (maybe attribute update).");
            return;
        }
        if (!moveEnable) {
            this.ipIf = null;
            log().debug("setNewIpAddress: create");
        }
        if (this.vpnOwnerName == null) {
            IpIfDto _ipIf = NodeUtil.getIpIfByIp(this.node, vpnPrefix, ip);
            this.ipIf = _ipIf;
            log().debug("setNewIpAddress: " + (this.ipIf == null ? "create" : "move"));
            return;
        }
        String absName = this.vpnOwnerName + ATTR.NAME_DELIMITER_PRIMARY + ip;
        EntityDto dto = null;
        try {
            dto = InventoryConnector.getInstance().getMvoDtoByAbsoluteName(absName);
        } catch (Exception e) {
            log().debug("got exception(ignorable).", e);
        }
        if (dto != null) {
            if (!(dto instanceof IpIfDto)) {
                throw new IllegalStateException("not ip-if: " + absName);
            }
            this.ipIf = (IpIfDto) dto;
            log().debug("setNewIpAddress: " + (this.ipIf == null ? "create vpn" : "move vpn") + " [" + ip + "]");
        } else {
            this.ipIf = null;
            log().debug("setNewIpAddress: vrf not found: " + this.vpnOwnerName);
        }
    }

    public void setNewIpAddress(String vpnPrefix, String ip, String mask) {
        setNewIpAddress(vpnPrefix, ip, mask, false);
    }

    private void setIpAddressAttribute(String vpnPrefix, String ip, String mask) {
        if (!DtoUtil.hasStringValue(this.originalIpIf, MPLSNMS_ATTR.VPN_PREFIX, vpnPrefix)) {
            ipAttributeNames.add(MPLSNMS_ATTR.VPN_PREFIX);
            recordChange(this.originalIpIf, MPLSNMS_ATTR.VPN_PREFIX, this.vpnName, vpnPrefix);
            this.vpnName = vpnPrefix;
        }
        if (!DtoUtil.hasStringValue(this.originalIpIf, MPLSNMS_ATTR.IP_ADDRESS, ip)) {
            ipAttributeNames.add(MPLSNMS_ATTR.IP_ADDRESS);
            recordChange(this.originalIpIf, MPLSNMS_ATTR.IP_ADDRESS, this.ipAddress, ip);
            this.ipAddress = ip;
        }
        if (!DtoUtil.hasStringValue(this.originalIpIf, MPLSNMS_ATTR.MASK_LENGTH, mask)) {
            ipAttributeNames.add(MPLSNMS_ATTR.MASK_LENGTH);
            recordChange(this.originalIpIf, MPLSNMS_ATTR.MASK_LENGTH, this.maskLength, mask);
            this.maskLength = mask;
        }
    }

    public void setIndependentIP(boolean value) {
        this.independentIP = value;
    }

    public boolean isIndependentIpInterface() {
        return this.independentIP;
    }

    public void setIpIfName(String ipIfName) {
        IpIfDto primaryIP = null;
        if (port != null) {
            primaryIP = port.getPrimaryIpIf();
        }
        if (primaryIP == null) {
            ipAttributeNames.add(MPLSNMS_ATTR.IFNAME);
            this.ipIfName = ipIfName;
            return;
        }
        if (ipIfName == null) {
            if (this.ipIfName != null) {
                ipAttributeNames.add(MPLSNMS_ATTR.IFNAME);
                recordChange(primaryIP, MPLSNMS_ATTR.IFNAME, DtoUtil.getIfName(primaryIP), null);
                this.ipIfName = null;
            } else {
            }
            return;
        }
        if (!Util.equals(DtoUtil.getIfName(primaryIP), ipIfName)) {
            recordChange(primaryIP, MPLSNMS_ATTR.IFNAME, DtoUtil.getIfName(primaryIP), ipIfName);
            ipAttributeNames.add(MPLSNMS_ATTR.IFNAME);
            this.ipIfName = ipIfName;
        }
    }

    public String getIpIfName() {
        String _ipIfName = (this.ipIfName != null ? this.ipIfName : this.ipAddress);
        return IfNameFactory.getVpnIpIfIfName(this.vpnName, _ipIfName);
    }

    public void setEndUserName(String endUserName) {
        setValue(MPLSNMS_ATTR.END_USER, endUserName);
    }

    final public void setPortDescription(String description) {
        setValue(MPLSNMS_ATTR.DESCRIPTION, description);
    }

    final public void setPurpose(String purpose) {
        setValue(MPLSNMS_ATTR.PURPOSE, purpose);
    }

    final public void setNote(String note) {
        setValue(MPLSNMS_ATTR.NOTE, note);
    }

    final public void setOperStatus(String status) {
        setValue(MPLSNMS_ATTR.OPER_STATUS, status);
    }

    final public void setAdminStatus(String status) {
        setValue(MPLSNMS_ATTR.ADMIN_STATUS, status);
    }

    protected final BuildResult buildCommandInner() throws IOException, InventoryException, ExternalServiceException {
        BuildResult result1 = buildPortCommands();
        if (result1 == BuildResult.FAIL) {
            return BuildResult.FAIL;
        }
        BuildResult result2 = buildIpCommands();
        if (result2 == BuildResult.FAIL) {
            return BuildResult.FAIL;
        }
        BuildResult result3 = buildVpnCommand();
        if (result3 == BuildResult.FAIL) {
            return BuildResult.FAIL;
        }
        if (result1 == BuildResult.NO_CHANGES && result2 == BuildResult.NO_CHANGES && result3 == BuildResult.NO_CHANGES) {
            return BuildResult.NO_CHANGES;
        }
        return BuildResult.SUCCESS;
    }

    abstract public BuildResult buildPortCommands() throws InventoryException;

    abstract public String getPortContext();

    abstract public String getNodeContext();

    public BuildResult buildIpCommands() throws IOException, InventoryException, ExternalServiceException {
        if (!needIP) {
            log.debug("no need to set ip to this i/f:" + this.ifName);
            return BuildResult.NO_CHANGES;
        } else if (ipAttributeNames.size() == 0) {
            log.debug("no ip-attribute:" + this.ifName);
            return BuildResult.NO_CHANGES;
        }

        BuildResult ipIfBuildResult = buildIpIf();
        if (ipIfBuildResult == BuildResult.SUCCESS) {
            return BuildResult.SUCCESS;
        }
        BuildResult ipIfNameResult = buildIpIfName();
        return ipIfNameResult;
    }

    private BuildResult buildIpIf() throws IOException, InventoryException, ExternalServiceException {
        Logger log = LoggerFactory.getLogger(PortCommandBuilder.class);
        if (this.ipIf != null) {
            if (this.originalIpIf != null) {
                if (DtoUtil.mvoEquals(this.originalIpIf, this.ipIf)) {
                    if (isIpChanged(this.vpnName, getIpIfName(), this.maskLength)) {
                        log.debug("update attributes of ip-if.");
                        updateIpIfAttribute(ipIf);
                    } else {
                        log.debug("no changes on ip.");
                        return BuildResult.NO_CHANGES;
                    }
                } else {
                    log.debug("replace with new ip-if.");
                    assignIpIf();
                }
            } else {
                log.debug("assign new ip-if.");
                assignIpIf();
            }
        } else {
            if (this.originalIpIf != null) {
                if (this.ipAddress != null) {
                    log.debug("create or update ip-if.");
                    createOrUpdateNewIpIf();
                } else {
                    if (this.originalIpIf.getOwner() instanceof VrfIfDto) {
                        log.debug("this ip is for vpn, so must be maintained by vpn-builder: " + this.originalIpIf.getAbsoluteName());
                        return BuildResult.NO_CHANGES;
                    } else {
                        log.debug("detach ip-if.");
                        removeIpIf();
                    }
                }
            } else {
                if (this.ipAddress != null) {
                    log.debug("create or update ip-if.");
                    createOrUpdateNewIpIf();
                } else {
                    log.debug("no changes on ip, too.");
                    return BuildResult.NO_CHANGES;
                }
            }
        }
        this.cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    private BuildResult buildIpIfName() {
        log.debug("buildIpIfName(): " + (this.ipIf != null) + ", " + (this.ipAttributeNames.contains(MPLSNMS_ATTR.IFNAME)));
        if (this.ipIf != null && this.ipAttributeNames.contains(MPLSNMS_ATTR.IFNAME)) {
            String ipIfName = getIpIfName();
            log.debug("changes on ip-if#ifName: " + ipIfName);
            InventoryBuilder.changeContext(cmd, this.ipIf);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, ipIfName);
            if (isIndependentIpInterface()) {
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, CONSTANTS.INTERFACE_TYPE_INDEPENDENT_IP);
            } else {
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, null);
            }
            recordChange(this.ipIf, MPLSNMS_ATTR.IFNAME, DtoUtil.getStringOrNull(this.ipIf, MPLSNMS_ATTR.IFNAME), ipIfName);
            return BuildResult.SUCCESS;
        } else {
            log.debug("no changes on ip-if#ifName, too.");
            return BuildResult.NO_CHANGES;
        }
    }

    private void assignIpIf() {
        if (!allowIpDuplication) {
            for (PortDto previous : this.ipIf.getAssociatedPorts()) {
                if (previous != null) {
                    InventoryBuilder.changeContext(cmd, previous);
                    InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, null);
                }
            }
        }
        changeContextToTargetPort();
        InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, this.ipIf.getAbsoluteName());
        buildIpIfName();
    }

    private void removeIpIf() {
        if (this.originalIpIf != null) {
            for (NetworkDto network : this.originalIpIf.getNetworks()) {
                if (IpSubnetAddressDto.class.isInstance(network)) {
                    continue;
                } else if (IpSubnetDto.class.isInstance(network)) {
                    continue;
                }
                throw new IllegalStateException("ip-if has link/network(s). please remove link/network(s) first.");
            }
            InventoryBuilder.changeContext(cmd, this.originalIpIf);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, null);
            if (DtoUtil.isSupportedAttribute(this.originalIpIf, MPLSNMS_ATTR.FIXED_RTT)) {
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.FIXED_RTT, null);
            }
            if (DtoUtil.isSupportedAttribute(this.originalIpIf, MPLSNMS_ATTR.FIXED_RTT)) {
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.FIXED_RTT, null);
            }
        }
        changeContextToTargetPort();
        InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, null);
        buildIpIfName();
    }

    private void createOrUpdateNewIpIf() throws IOException, InventoryException, ExternalServiceException {
        if (NodeUtil.getIpOn(port) != null) {
            updateAnonymousToNamedIpIf();
        } else {
            createNewIpIf();
        }
    }

    private void createNewIpIf() throws IOException, InventoryException, ExternalServiceException {
        String nodeName;
        if (port == null) {
            nodeName = getNodeContext();
            if (nodeName.startsWith("node;")) {
                nodeName = nodeName.replace("node;", "");
            }
        } else {
            nodeName = port.getNode().getName();
        }
        if (this.vpnOwnerName == null) {
            InventoryBuilder.changeContext(cmd, ATTR.TYPE_NODE, nodeName);
        } else {
            InventoryBuilder.changeContext(cmd, this.vpnOwnerName);
        }
        SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_IP_PORT, getIpIfName());
        String ipifVariableName = "IPIF_" + Integer.toHexString(System.identityHashCode(this));
        InventoryBuilder.assignVar(cmd, ipifVariableName);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, getIpIfName());
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.VPN_PREFIX, this.vpnName);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IP_ADDRESS, this.ipAddress);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.MASK_LENGTH, this.maskLength);
        if (isIndependentIpInterface()) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, CONSTANTS.INTERFACE_TYPE_INDEPENDENT_IP);
        } else {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, null);
        }
        changeContextToTargetPort();
        InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, "$;" + ipifVariableName);
        associateIpSubnetAddress(ipifVariableName);
    }

    private void updateAnonymousToNamedIpIf() throws IOException, InventoryException, ExternalServiceException {
        InventoryBuilder.changeContext(cmd, this.originalIpIf);
        String ipifVariableName = "IPIF_" + Integer.toHexString(System.identityHashCode(this));
        InventoryBuilder.assignVar(cmd, ipifVariableName);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.VPN_PREFIX, this.vpnName);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IP_ADDRESS, this.ipAddress);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.MASK_LENGTH, this.maskLength);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, getIpIfName());
        if (isIndependentIpInterface()) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, CONSTANTS.INTERFACE_TYPE_INDEPENDENT_IP);
        } else {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, null);
        }
        InventoryBuilder.buildRenameCommands(cmd, this.ipAddress);
        associateIpSubnetAddress(ipifVariableName);
    }

    private void associateIpSubnetAddress(String varName) throws IOException, InventoryException, ExternalServiceException {
        if (!this.associateIpSubnetAddress) {
            return;
        }
        IpSubnetAddressDto root = InventoryConnector.getInstance().getRootIpSubnetAddressByVpn(this.vpnName);
        if (root == null) {
            return;
        }
        int length = Integer.parseInt(this.maskLength);
        IpSubnetAddressDto addr = IpSubnetAddressUtil.findMostAppropriateSubnet(root, this.ipAddress, length);
        if (addr == null) {
            return;
        }
        IpSubnetDto subnet = addr.getIpSubnet();
        if (subnet == null) {
            log.warn("unexpected data (broken?): no ip-subnet-namespace on ip-subnet-address: " +
                    DtoUtil.toDebugString(addr));
            return;
        }
        String var = "$;" + varName;
        InventoryBuilder.changeContext(cmd, var);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_SUBNET_ADDRESS, addr.getAbsoluteName());
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_IP_ADDRESS, this.ipAddress);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_SUBNETMASK_LENGTH, this.maskLength);
        InventoryBuilder.changeContext(cmd, subnet);
        InventoryBuilder.buildBindPortToNetworkCommands(this.cmd, var);
    }

    private void updateIpIfAttribute(IpIfDto ip) {
        InventoryBuilder.changeContext(cmd, ip);
        InventoryBuilder.buildAttributeSetOrReset(cmd, ATTR.VPN_PREFIX, this.vpnName);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.MASK_LENGTH, this.maskLength);
        InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.IFNAME, getIpIfName());
        if (isIndependentIpInterface()) {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, CONSTANTS.INTERFACE_TYPE_INDEPENDENT_IP);
        } else {
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, null);
        }
        if (isIpNameChanged(this.vpnName, getIpIfName())) {
            InventoryBuilder.buildRenameCommands(this.cmd, getIpIfName());
        }
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        if (port == null) {
            throw new IllegalStateException("port is null.");
        }
        checkNetworkUsage();
        if (port.getPrimaryIpIf() != null) {
            checkIpReleasable(port, port.getPrimaryIpIf());
            InventoryBuilder.changeContext(cmd, port);
            InventoryBuilder.buildPortIpUnbindAsPrimaryCommand(cmd);
        }
        if (port.getSecondaryIpIf() != null) {
            InventoryBuilder.changeContext(cmd, port);
            checkIpReleasable(port, port.getSecondaryIpIf());
            InventoryBuilder.buildPortIpUnbindAsSecondaryCommand(cmd);
        }
        buildPortDeleteCommand();
        recordChange("Port", DtoUtil.getIfName(port), null);
        return BuildResult.SUCCESS;
    }

    protected void buildPortDeleteCommand() {
        if (this.vpnOwnerName != null) {
            buildVpnDeleteCommand();
        }
        InventoryBuilder.changeContext(cmd, port.getOwner());
        cmd.addCommand(InventoryBuilder.translate(CMD.REMOVE_ELEMENT,
                "_TYPE_", port.getObjectTypeName(),
                "_NAME_", port.getName()));
    }

    protected BuildResult buildVpnCommand() {
        if (this.vpnOwnerName == null) {
            if (this.originalVpnOwner != null) {
                buildVpnDeleteCommand();
                return BuildResult.SUCCESS;
            } else {
                return BuildResult.NO_CHANGES;
            }
        }
        if (this.originalVpnOwner != null) {
            if (this.vpnOwnerName.equals(this.originalVpnOwner.getAbsoluteName())) {
                return BuildResult.NO_CHANGES;
            }
        }
        InventoryBuilder.buildConnectPortToNetworkIfCommands(cmd, this.vpnOwnerName, getPortContext());
        return BuildResult.SUCCESS;
    }

    protected void buildVpnDeleteCommand() {
        if (this.port == null) {
            throw new IllegalStateException("port is null.");
        }
        if (this.originalVpnOwner != null) {
            InventoryBuilder.buildDisconnectPortFromNetworkIfCommands(cmd, this.originalVpnOwner, this.port);
        }
    }

    protected void checkNetworkUsage() {
        if (!this.preCheckEnable) {
            return;
        }
        Set<NetworkDto> networks = port.getNetworks();
        if (networks.size() > 0) {
            throw new IllegalStateException("port is used by network.");
        }
    }

    protected void checkIpReleasable(PortDto owner, IpIfDto ip) {
        if (ip == null) {
            return;
        }
        if (!this.preCheckEnable) {
            return;
        }
        Set<NetworkDto> networks = ip.getNetworks();
        if (networks.size() > 0) {
            throw new IllegalStateException("ip of this port is used by network: " + owner.getAbsoluteName());
        }
    }

    public String getIfName() {
        return this.ifName;
    }

    protected void assignTargetPortToShellContextVariable() {
        targetPortVariableName = "TARGET_PORT_" + Integer.toHexString(System.identityHashCode(this));
        InventoryBuilder.assignVar(cmd, targetPortVariableName);
    }

    private void changeContextToTargetPort() {
        if (targetPortVariableName != null) {
            InventoryBuilder.changeContext(cmd, "$;" + targetPortVariableName);
        } else if (port == null) {
            InventoryBuilder.changeContext(cmd, getPortContext());
        } else {
            InventoryBuilder.changeContext(cmd, port);
        }
    }

    public synchronized boolean isPreCheckEnable() {
        return preCheckEnable;
    }

    public synchronized void setPreCheckEnable(boolean preCheckEnable) {
        this.preCheckEnable = preCheckEnable;
    }

    private boolean isIpChanged(String vpnPrefix, String ip, String maskLength) {
        return DtoUtil.isValueChanged(this.ipIf, ATTR.VPN_PREFIX, vpnPrefix) ||
                DtoUtil.isValueChanged(this.ipIf, MPLSNMS_ATTR.IFNAME, ip) ||
                DtoUtil.isValueChanged(this.ipIf, MPLSNMS_ATTR.MASK_LENGTH, maskLength);
    }

    private boolean isIpNameChanged(String vpnPrefix, String ip) {
        return DtoUtil.isValueChanged(this.ipIf, ATTR.VPN_PREFIX, vpnPrefix) ||
                DtoUtil.isValueChanged(this.ipIf, MPLSNMS_ATTR.IFNAME, ip);
    }
}