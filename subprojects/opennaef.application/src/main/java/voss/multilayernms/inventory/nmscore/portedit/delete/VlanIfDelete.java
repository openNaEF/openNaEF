package voss.multilayernms.inventory.nmscore.portedit.delete;

import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.PortDto;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.builder.VlanIfBindingCommandBuilder;
import voss.nms.inventory.builder.VlanIfCommandBuilder;
import voss.nms.inventory.util.VlanUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class VlanIfDelete implements IPortDelete {

    private PortEditModel model;
    private String user;

    public VlanIfDelete(PortEditModel model, String user) {
        this.model = model;
        this.user = user;
    }

    @Override
    public void delete() throws RuntimeException, IllegalStateException, IOException, InventoryException, ExternalServiceException {
        VlanIfDto vlanif = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), VlanIfDto.class);
        try {
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            if (VlanUtil.isRouterVlanIf(vlanif)) {
                VlanIfCommandBuilder builder = null;
                builder = new VlanIfCommandBuilder((PortDto) vlanif.getOwner(), vlanif, user);
                builder.buildDeleteCommand();
                commandBuilderList.add(builder);
            }
            else if (VlanUtil.isSwitchVlanIf(vlanif)) {
                VlanIfBindingCommandBuilder builder = null;
                builder = new VlanIfBindingCommandBuilder((VlanIfDto) vlanif, user);
                for (PortDto p : vlanif.getTaggedVlans()) {
                    builder.removeTaggedPort(p);
                }
                for (PortDto p : vlanif.getUntaggedVlans()) {
                    builder.removeUntaggedPort(p);
                }
                builder.buildCommand();
                commandBuilderList.add(builder);
            }
            ShellConnector.getInstance().executes(commandBuilderList);
        } catch (InventoryException e) {
            log.debug(vlanif.getAbsoluteName(), e);
            throw e;
        } catch (IllegalStateException e) {
            log.debug(vlanif.getAbsoluteName(), e);
            throw e;
        } catch (RuntimeException e) {
            log.debug(vlanif.getAbsoluteName(), e);
            throw e;
        }
    }
}