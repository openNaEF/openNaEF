package voss.nms.inventory.builder;

import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import voss.core.server.builder.*;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NameUtil;

import java.io.IOException;
import java.util.*;

public class IpLinkCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final IpSubnetDto link;
    private String id;
    private String originalId;
    private PortDto port1;
    private IpIfDto ip1;
    private PortDto port2;
    private IpIfDto ip2;
    private final IpIfDto original1;
    private final IpIfDto original2;
    private int maxPorts = 2;

    public IpLinkCommandBuilder(IpSubnetDto target, String editorName) {
        super(IpSubnetDto.class, target, editorName);
        setConstraint(IpSubnetDto.class);
        this.link = target;
        if (this.link == null) {
            this.original1 = null;
            this.original2 = null;
        } else {
            if (this.link.getMemberIpifs().size() != 2) {
                throw new IllegalArgumentException("this ip-subnet doesn't have 2 port: " + this.link.getAbsoluteName());
            }
            Iterator<PortDto> iterator = this.link.getMemberIpifs().iterator();
            this.id = link.getSubnetName();
            this.originalId = this.id;
            this.original1 = NodeUtil.toIpIfDto(iterator.next());
            this.ip1 = original1;
            this.original2 = NodeUtil.toIpIfDto(iterator.next());
            this.ip2 = original2;
            this.cmd.addVersionCheckTarget(link);
        }
    }

    public void setPort1(PortDto port) {
        if (port == null) {
            throw new IllegalArgumentException();
        } else if (NodeUtil.getLayer3Link(port) != null) {
            throw new IllegalStateException("already have link: " + port.getAbsoluteName());
        }
        this.port1 = port;
        this.ip1 = NodeUtil.getIpOn(port);
        this.cmd.addVersionCheckTarget(port);
        recordChange("Port1", this.original1.getAbsoluteName(), port.getAbsoluteName());
    }

    public void setPort2(PortDto port) {
        if (port == null) {
            throw new IllegalArgumentException();
        } else if (NodeUtil.getLayer3Link(port) != null) {
            throw new IllegalStateException("already have link: " + port.getAbsoluteName());
        }
        this.port2 = port;
        this.ip2 = NodeUtil.getIpOn(port);
        this.cmd.addVersionCheckTarget(port);
        recordChange("Port2", this.original2.getAbsoluteName(), port.getAbsoluteName());
    }

    public void setFacilityStatus(String status) {
        setValue(MPLSNMS_ATTR.FACILITY_STATUS, status);
    }

    public void setFoundOnNetwork(Boolean value) {
        if (value == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.LINK_FOUND_ON_NETWORK, value.toString());
    }

    public void setMaxPorts(int ports) {
        setValue(ATTR.LINK_MAX_PORTS, String.valueOf(maxPorts));
    }

    public void setFoundOnExternalInventoryDB(Boolean value) {
        if (value == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.LINK_FOUND_ON_EXTERNAL_INVENTORY_DB, value.toString());
    }

    public void setApproved(Boolean value) {
        if (value == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.LINK_APPROVED, value.toString());
    }

    public void setSource(String source) {
        setValue(MPLSNMS_ATTR.SOURCE, source);
    }

    public void setSRLGValue(String sRLG) {
        setValue(MPLSNMS_ATTR.LINK_SRLG_VALUE, sRLG);
    }

    public void setCableName(String cableName) {
        setValue(MPLSNMS_ATTR.LINK_CABLE_NAME, cableName);
    }

    public void setLinkType(String type) {
        setValue(MPLSNMS_ATTR.LINK_TYPE, type);
    }

    public void setLinkAccommodationLimit(String capacity) {
        setValue(MPLSNMS_ATTR.LINK_ACCOMMODATION_LIMIT, capacity);
    }

    public void setLinkId(String id) {
        if (Util.equals(id, this.originalId)) {
            return;
        }
        this.id = id;
        recordChange(MPLSNMS_ATTR.LINK_NAME, this.originalId, id);
    }

    public String getLinkId() {
        if (this.id != null) {
            return this.id;
        }
        List<String> list = new ArrayList<String>();
        list.add(getNodeIfName(port1));
        list.add(getNodeIfName(port2));
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    private String getNodeIfName(PortDto port) {
        if (port instanceof IpIfDto) {
            port = NodeUtil.getAssociatedPort((IpIfDto) port);
        }
        return NameUtil.getNodeIfName(port);
    }

    @Override
    protected BuildResult buildCommandInner() throws IOException {
        if (this.link != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        if (this.link == null) {
            String ip1Name;
            String ip2Name;
            if (this.ip1 == null) {
                String ip1IfName = "noip@" + DtoUtil.getIfName(port1);
                InventoryBuilder.changeContext(cmd, port1.getNode());
                SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_IP_PORT, ip1IfName);
                ip1Name = BuilderUtil.getIpAbsoluteName(ip1IfName, port1);
                InventoryBuilder.changeContext(cmd, port1);
                InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, ip1Name);
            } else {
                ip1Name = ip1.getAbsoluteName();
            }
            if (this.ip2 == null) {
                String ip2IfName = "noip@" + DtoUtil.getIfName(port2);
                InventoryBuilder.changeContext(cmd, port2.getNode());
                SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_IP_PORT, ip2IfName);
                ip2Name = BuilderUtil.getIpAbsoluteName(ip2IfName, port2);
                InventoryBuilder.changeContext(cmd, port2);
                InventoryBuilder.buildPortIpBindAsPrimaryCommand(cmd, ip2Name);
            } else {
                ip2Name = ip2.getAbsoluteName();
            }
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            String id = getLinkId();
            InventoryConnector conn = InventoryConnector.getInstance();
            try {
                IpSubnetNamespaceDto pool = conn.getActiveRootIpSubnetNamespace();
                InventoryBuilder.buildNetworkIDCreationCommand(cmd, ATTR.NETWORK_TYPE_IPSUBNET,
                        ATTR.ATTR_IPSUBNET_ID, id,
                        ATTR.ATTR_IPSUBNET_POOL, pool.getName());
            } catch (ExternalServiceException e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
            InventoryBuilder.buildBindPortToNetworkCommands(cmd, ip1Name);
            InventoryBuilder.buildBindPortToNetworkCommands(cmd, ip2Name);
        } else {
            InventoryBuilder.changeContext(cmd, link);
            String id = getLinkId();
            boolean idChanged = !Util.equals(this.originalId, id);
            boolean ip1Changed = !DtoUtil.isSameMvoEntity(original1, ip1);
            boolean ip2Changed = !DtoUtil.isSameMvoEntity(original2, ip2);
            if (Util.isAllTrue(!idChanged, !ip1Changed, !ip2Changed, attributes.size() == 0)) {
                return BuildResult.NO_CHANGES;
            }
            if (idChanged) {
                InventoryBuilder.buildNetworkIDRenameCommands(cmd, ATTR.ATTR_IPSUBNET_ID, id);
            }
            InventoryBuilder.buildAttributeUpdateCommand(cmd, link, attributes);
            if (ip1Changed) {
                InventoryBuilder.buildUnbindPortFromNetworkCommands(cmd, original1);
                InventoryBuilder.buildBindPortToNetworkCommands(cmd, link, ip1);
            }
            if (ip2Changed) {
                InventoryBuilder.buildUnbindPortFromNetworkCommands(cmd, original2);
                InventoryBuilder.buildBindPortToNetworkCommands(cmd, link, ip2);
            }
        }
        cmd.addLastEditCommands();
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        if (this.link == null) {
            throw new IllegalStateException("link is not exist.");
        }
        InventoryBuilder.changeContext(cmd, link);
        if (this.link.getMemberIpifs() != null) {
            for (PortDto ip : this.link.getMemberIpifs()) {
                InventoryBuilder.translate(cmd,
                        CMD.UNBIND_NETWORK_INSTANCE,
                        CMD.ARG_INSTANCE, ip.getAbsoluteName());
            }
        }
        InventoryBuilder.buildNetworkIDReleaseCommand(cmd, ATTR.ATTR_IPSUBNET_ID, ATTR.ATTR_IPSUBNET_POOL);
        recordChange("Link", "delete", null);
        return BuildResult.SUCCESS;
    }

    public String getObjectType() {
        return DiffObjectType.L3_LINK.getCaption();
    }

}