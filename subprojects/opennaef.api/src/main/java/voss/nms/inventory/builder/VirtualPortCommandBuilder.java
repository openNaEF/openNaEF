package voss.nms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.BuilderUtil;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Date;

public class VirtualPortCommandBuilder extends PortCommandBuilder {
    private static final long serialVersionUID = 1L;
    private final String nodeName;
    private final NodeDto node;
    private final PortDto port;
    private PortType portType = null;

    public VirtualPortCommandBuilder(NodeDto node, String editorName) {
        super(PortDto.class, node, null, editorName);
        this.node = node;
        this.nodeName = null;
        this.port = null;
    }

    public VirtualPortCommandBuilder(String nodeName, String editorName) {
        super(PortDto.class, null, null, editorName);
        this.nodeName = BuilderUtil.getNodeName(nodeName);
        this.node = null;
        this.port = null;
    }

    public VirtualPortCommandBuilder(PortDto port, String editorName) {
        super(PortDto.class, port.getNode(), port, editorName);
        this.nodeName = null;
        this.node = port.getNode();
        this.port = port;
        if (port != null) {
            initialize(port);
        }
    }

    private void initialize(PortDto port) {
        this.cmd.addVersionCheckTarget(port);
        String _portType = DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.PORT_TYPE);
        if (_portType != null) {
            this.portType = PortType.getByType(_portType);
        } else {
            this.portType = null;
        }
    }

    public void setPortType(PortType portType) {
        if (portType == null) {
            throw new IllegalArgumentException("port-type is mandatory.");
        }
        this.portType = portType;
    }

    @Override
    public String getPortContext() {
        return AbsoluteNameFactory.getEthernetPortAbsoluteName(nodeName, getIfName());
    }

    @Override
    public String getNodeContext() {
        return AbsoluteNameFactory.getNodeAbsoluteName2(this.nodeName);
    }

    @Override
    public BuildResult buildPortCommands() {
        if (this.portType == null) {
            throw new IllegalStateException("no port-type.");
        } else if (this.port == null) {
            setValue(MPLSNMS_ATTR.PORT_TYPE, this.portType.getCaption());
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
                InventoryBuilder.changeContext(cmd, getNodeContext());
            }
            SimpleNodeBuilder.buildPortCreationCommands(cmd, this.portType.getType(), ifName);
            InventoryBuilder.buildAttributeSetOnCurrentContextCommands(cmd, attributes);
            this.cmd.addLastEditCommands();
        } else {
            InventoryBuilder.changeContext(cmd, port);
            InventoryBuilder.buildAttributeUpdateCommand(cmd, port, attributes);
            this.cmd.addLastEditCommands();
            String currentIfName = DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.IFNAME);
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