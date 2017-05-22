package voss.nms.inventory.builder.complement;

import naef.dto.NodeDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetAddressDto;
import naef.dto.ip.IpSubnetDto;
import naef.mvo.ip.IpAddress;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.MvoDtoSet;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;
import java.util.Set;

public class IpSubnetAddressComplementBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final MvoDtoSet<IpIfDto> targetIpIfs = new MvoDtoSet<IpIfDto>();
    private final InventoryConnector conn;
    private final NaefDtoFacade facade;

    private boolean createSubnet = false;

    public IpSubnetAddressComplementBuilder(String editorName) {
        this(editorName, true);
    }

    public IpSubnetAddressComplementBuilder(String editorName, boolean versionCheckRequired) {
        super(IpIfDto.class, null, editorName, versionCheckRequired);
        try {
            this.conn = InventoryConnector.getInstance();
            this.facade = this.conn.getDtoFacade();
        } catch (IOException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        } catch (ExternalServiceException e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public void setCreateSubnet(boolean value) {
        this.createSubnet = value;
    }

    public void addTargetIpIf(IpIfDto ipIf) {
        if (ipIf == null) {
            throw new IllegalArgumentException();
        }
        this.targetIpIfs.add(ipIf);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException, InventoryException, ExternalServiceException {
        if (this.targetIpIfs.size() == 0) {
            for (NodeDto node : conn.getActiveNodes()) {
                Set<IpIfDto> ipIfs = facade.selectNodeElements(
                        node, IpIfDto.class, SearchMethod.REGEXP, MPLSNMS_ATTR.IP_ADDRESS, ".*");
                this.targetIpIfs.addAll(ipIfs);
            }
        }
        for (IpIfDto ip : this.targetIpIfs) {
            try {
                processIpIf(ip);
            } catch (Exception e) {
                log.warn("failed to process: " + DtoUtil.toDebugString(ip), e);
            }
        }
        return BuildResult.SUCCESS;
    }

    private void processIpIf(IpIfDto ip) throws IOException, InventoryException, ExternalServiceException {
        String ipAddress = DtoUtil.getStringOrNull(ip, ATTR.IP_ADDRESS);
        if (ipAddress == null) {
            remove(ip);
        } else {
            assign(ip);
        }
    }

    private void assign(IpIfDto ip) throws IOException, InventoryException, ExternalServiceException {
        String vpnPrefix = DtoUtil.getStringOrNull(ip, ATTR.VPN_PREFIX);
        String ipAddress = DtoUtil.getStringOrNull(ip, ATTR.IP_ADDRESS);
        String maskLengthValue = DtoUtil.getStringOrNull(ip, ATTR.MASK_LENGTH);
        Integer maskLength = Integer.valueOf(maskLengthValue);
        String key = AbsoluteNameFactory.toIpSubnetAddressName(vpnPrefix, ipAddress, maskLength);
        if (maskLength == null) {
            log.debug("skipped: mask-length is null: " + DtoUtil.toDebugString(ip));
            return;
        }
        IpAddress address = IpAddress.gain(ipAddress);
        IpSubnetAddressDto root = conn.getRootIpSubnetAddressByVpn(vpnPrefix);
        if (root == null) {
            log.debug("skipped: no root ip-subnet-address: ip=" + key);
            return;
        }
        IpSubnetAddressDto subnetAddress = facade.getLeafIdPool(root, address);
        if (subnetAddress == null) {
            if (this.createSubnet) {
                log.debug("skipped: subnet-address creation is not implemented: ip=" + key);
            } else {
                log.debug("skipped: no subnet-address found: " + key);
            }
            return;
        }
        Integer maskLen = subnetAddress.getSubnetMask();
        if (!maskLen.equals(maskLength)) {
            log.debug("skipped: mask-length mismatch: ip-if=" + key
                    + ", subnet-address=" + subnetAddress.getName());
            return;
        }
        IpSubnetDto subnet = subnetAddress.getIpSubnet();
        if (subnet == null) {
            log.debug("skipped: no ip-subnet found: subnet-address=" + DtoUtil.toDebugString(subnetAddress));
            return;
        }
        log.debug("bind: " + key + " -> " + subnet.getSubnetName());
        InventoryBuilder.changeContext(this.cmd, ip);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_SUBNET_ADDRESS, subnetAddress.getAbsoluteName());
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_IP_ADDRESS, ipAddress);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_SUBNETMASK_LENGTH, maskLength.toString());
        InventoryBuilder.changeContext(this.cmd, subnet);
        InventoryBuilder.buildBindPortToNetworkCommands(this.cmd, ip);
        recordChange("ip-if_bind", null, key);
    }

    private void remove(IpIfDto ip) {
        InventoryBuilder.changeContext(this.cmd, ip);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_IP_ADDRESS, null);
        InventoryBuilder.buildAttributeSetOrReset(this.cmd, ATTR.ATTR_IPIF_SUBNET_ADDRESS, null);
        IpSubnetDto subnet = ip.getSubnet();
        if (subnet != null) {
            InventoryBuilder.changeContext(this.cmd, subnet);
            InventoryBuilder.buildUnbindPortFromNetworkCommands(this.cmd, ip);
        }
        recordChange("ip-if_unbind", NameUtil.getNodeIfName(ip), null);
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException, InventoryException, ExternalServiceException {
        return BuildResult.NO_CHANGES;
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.OTHER.getCaption();
    }

}