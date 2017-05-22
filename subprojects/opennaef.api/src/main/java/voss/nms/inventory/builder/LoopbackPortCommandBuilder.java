package voss.nms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.ip.IpIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.BuilderUtil;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.database.ATTR;
import voss.core.server.database.CONSTANTS;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;

public class LoopbackPortCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    public static final String LOOPBACK_TYPE = PortType.LOOPBACK.getCaption();
    public static final String INDEPENDENT_TYPE = CONSTANTS.INTERFACE_TYPE_INDEPENDENT_IP;

    private final String nodeName;
    private final NodeDto node;
    private final IpIfDto port;
    private boolean indepedent = false;
    private boolean originalIndependent = false;
    private String portType = null;

    public LoopbackPortCommandBuilder(NodeDto node, String editorName) {
        super(IpIfDto.class, node, null, editorName);
        setConstraint(IpIfDto.class);
        this.node = node;
        this.nodeName = null;
        this.port = null;
    }

    public LoopbackPortCommandBuilder(String nodeName, String editorName) {
        super(IpIfDto.class, null, null, editorName);
        setConstraint(IpIfDto.class);
        this.nodeName = BuilderUtil.getNodeName(nodeName);
        this.node = null;
        this.port = null;
    }

    public LoopbackPortCommandBuilder(IpIfDto loopback, String editorName) {
        super(IpIfDto.class, loopback.getNode(), loopback, editorName);
        setConstraint(IpIfDto.class);
        this.nodeName = null;
        this.node = loopback.getNode();
        this.port = loopback;
        if (loopback != null) {
            initialize(loopback);
        }
    }

    private void initialize(IpIfDto loopback) {
        this.cmd.addVersionCheckTarget(port);
        this.indepedent = NodeUtil.isIndependentIp(loopback);
        this.originalIndependent = this.indepedent;
        this.portType = DtoUtil.getStringOrNull(loopback, MPLSNMS_ATTR.PORT_TYPE);
    }

    public void setIgpCost(Integer cost) {
        if (cost == null) {
            return;
        }
        setValue(MPLSNMS_ATTR.IGP_COST, cost.toString());
    }

    public void setOspfAreaID(String area) {
        setValue(MPLSNMS_ATTR.OSPF_AREA_ID, area);
    }

    public void setIndependent(boolean value) {
        if (value == this.indepedent) {
            return;
        }
        this.indepedent = value;
    }

    @Override
    public String getPortContext() {
        return AbsoluteNameFactory.getLoopbackAbsoluteName(nodeName, getIfName());
    }

    @Override
    public String getNodeContext() {
        return this.nodeName;
    }

    @Override
    public BuildResult buildPortCommands() {
        String newPortType = null;
        if (this.portType == null || this.indepedent != this.originalIndependent) {
            if (this.indepedent) {
                newPortType = INDEPENDENT_TYPE;
            } else {
                newPortType = LOOPBACK_TYPE;
            }
            recordChange(MPLSNMS_ATTR.PORT_TYPE, this.portType, newPortType);
        }
        if (this.port != null && !hasChange()) {
            return BuildResult.NO_CHANGES;
        }
        String ifName = getIfName();
        if (port == null) {
            String registerDate = InventoryBuilder.getInventoryDateString(new Date());
            setValue(MPLSNMS_ATTR.REGISTERED_DATE, registerDate);
            if (this.node != null) {
                InventoryBuilder.changeContext(cmd, node);
            } else {
                InventoryBuilder.changeContext(cmd, ATTR.TYPE_NODE, nodeName);
            }
            SimpleNodeBuilder.buildPortCreationCommands(cmd, ATTR.TYPE_IP_PORT, ifName);
            InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, newPortType);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
            this.cmd.addLastEditCommands();
        } else {
            InventoryBuilder.changeContext(cmd, port);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, port, attributes);
            if (newPortType != null) {
                InventoryBuilder.buildAttributeSetOrReset(cmd, MPLSNMS_ATTR.PORT_TYPE, newPortType);
            }
            this.cmd.addLastEditCommands();
            String currentIfName = DtoUtil.getStringOrNull(port, ATTR.IFNAME);
            if (!currentIfName.equals(ifName)) {
                InventoryBuilder.buildRenameCommands(cmd, ifName);
            }
        }
        return BuildResult.SUCCESS;
    }

    public PortDto getPort() {
        return this.port;
    }

    public String getObjectType() {
        return DiffObjectType.LOOPBACK.getCaption();
    }

}