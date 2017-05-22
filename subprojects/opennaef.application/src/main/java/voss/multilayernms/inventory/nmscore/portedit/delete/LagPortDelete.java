package voss.multilayernms.inventory.nmscore.portedit.delete;

import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.eth.EthLagIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.builder.EthernetLAGCommandBuilder;
import voss.nms.inventory.builder.PortCommandBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LagPortDelete implements IPortDelete {
    private PortEditModel model;
    private String user;

    public LagPortDelete(PortEditModel model, String user) {
        this.model = model;
        this.user = user;
    }

    @Override
    public void delete() throws RuntimeException, IOException, InventoryException, ExternalServiceException, IllegalStateException {
        PortDto port = MplsNmsInventoryConnector.getInstance().getPortByInventoryID(model.getInventoryId());
        if (port instanceof EthLagIfDto) {
            try {
                if (0 < port.getUpperLayers().size()) {
                    StringBuilder ports = new StringBuilder();
                    for (PortDto p : port.getUpperLayers()) {
                        ports.append(p.getIfname() + " , ");
                    }
                    throw new InventoryException("Vlan exists on the LAG.:" + ports);
                }
                List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
                PortCommandBuilder builder = null;
                builder = new EthernetLAGCommandBuilder((EthLagIfDto) port, user);
                builder.buildDeleteCommand();
                commandBuilderList.add(builder);
                ShellConnector.getInstance().executes(commandBuilderList);
            } catch (InventoryException e) {
                log.debug(port.getAbsoluteName(), e);
                throw e;
            } catch (IllegalStateException e) {
                log.debug(port.getAbsoluteName(), e);
                throw e;
            } catch (IOException e) {
                log.debug(port.getAbsoluteName(), e);
                throw e;
            } catch (RuntimeException e) {
                log.debug(port.getAbsoluteName(), e);
                throw e;
            }
        } else if (port instanceof AtmApsIfDto) {
        }
        if (0 < port.getUpperLayers().size()) {
            throw new IllegalStateException(port.getAbsoluteName() + " Networks of port exists.");
        }
    }
}