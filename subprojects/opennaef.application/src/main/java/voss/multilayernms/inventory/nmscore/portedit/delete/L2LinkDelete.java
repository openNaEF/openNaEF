package voss.multilayernms.inventory.nmscore.portedit.delete;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.L2LinkDto;
import naef.dto.LinkDto;
import naef.dto.NetworkDto;
import naef.dto.PortDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.builder.LinkCommandBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class L2LinkDelete implements IPortDelete {
    private PortDto port;
    private PortEditModel model;
    private String user;

    public L2LinkDelete(PortDto port, PortEditModel model, String user) {
        this.port = port;
        this.model = model;
        this.user = user;
    }

    @Override
    public void delete() throws RuntimeException, IOException, InventoryException, ExternalServiceException {
        if (model instanceof HardPort) {
            PortDto port = this.port;
            if (port != null && port.getNetworks() != null) {
                List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
                for (NetworkDto link : port.getNetworks()) {
                    if (link instanceof L2LinkDto) {
                        CommandBuilder linkBuilder = new LinkCommandBuilder((LinkDto) link, user);
                        linkBuilder.buildDeleteCommand();
                        commandBuilderList.add(linkBuilder);
                    }
                }
                ShellConnector.getInstance().executes(commandBuilderList);
            }
        } else if (model instanceof LagPort) {
            throw new IllegalArgumentException("Target is wrong.");
        } else {
            throw new IllegalArgumentException("Target is wrong.");
        }
    }
}