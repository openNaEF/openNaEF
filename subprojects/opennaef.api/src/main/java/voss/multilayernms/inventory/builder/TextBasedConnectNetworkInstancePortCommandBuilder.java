package voss.multilayernms.inventory.builder;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import voss.core.server.builder.AbstractCommandBuilder;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CMD;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.DiffObjectType;
import voss.core.server.util.Util;

import java.io.IOException;

public class TextBasedConnectNetworkInstancePortCommandBuilder extends AbstractCommandBuilder {
    private static final long serialVersionUID = 1L;
    private NodeDto node;
    private String ifAbsoluteName1;
    private String ifAbsoluteName2;

    public TextBasedConnectNetworkInstancePortCommandBuilder(NodeDto node, String ifAbsoluteName1, String ifAbsoluteName2, String editorName) {
        super(PortDto.class, null, editorName);
        setConstraint(PortDto.class);
        if (Util.isNull(ifAbsoluteName1, ifAbsoluteName2)) {
            throw new IllegalArgumentException();
        }
        this.node = node;
        this.ifAbsoluteName1 = ifAbsoluteName1;
        this.ifAbsoluteName2 = ifAbsoluteName2;
    }


    @Override
    protected BuildResult buildCommandInner() throws IOException {
        InventoryBuilder.changeContext(cmd, node);
        InventoryBuilder.translate(cmd,
                CMD.CONNECT_NETWORK_INSTANCE_PORT,
                CMD.ARG_INSTANCE, ifAbsoluteName1,
                CMD.ARG_PORT, ifAbsoluteName2);
        cmd.addLastEditCommands();

        recordChange("create", ifAbsoluteName1, null);
        return BuildResult.SUCCESS;
    }

    @Override
    protected BuildResult buildDeleteCommandInner() throws IOException {
        InventoryBuilder.changeContext(cmd, node);
        InventoryBuilder.translate(cmd,
                CMD.DISCONNECT_NETWORK_INSTANCE_PORT,
                CMD.ARG_INSTANCE, ifAbsoluteName1,
                CMD.ARG_PORT, ifAbsoluteName2);
        cmd.addLastEditCommands();
        recordChange("create", ifAbsoluteName1, null);
        return BuildResult.SUCCESS;
    }

    @Override
    public String getObjectType() {
        return DiffObjectType.PORT.getCaption();
    }

}