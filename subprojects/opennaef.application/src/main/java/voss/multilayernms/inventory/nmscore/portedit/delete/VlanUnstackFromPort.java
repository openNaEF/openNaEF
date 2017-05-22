package voss.multilayernms.inventory.nmscore.portedit.delete;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.PortDto;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.VlanUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.builder.VlanIfBindingCommandBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class VlanUnstackFromPort implements IPortDelete {

    private PortEditModel model;
    private String user;
    private PortDto port;

    public VlanUnstackFromPort(PortDto port, PortEditModel model, String user) {
        this.model = model;
        this.user = user;
        this.port = port;
    }

    @Override
    public void delete() throws RuntimeException, IllegalStateException, IOException, InventoryException, ExternalServiceException {

        try {
            List<Integer> vlanids = new ArrayList<Integer>();
            port = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), PortDto.class);
            if (model instanceof HardPort) {
                if (((HardPort) model).getVlanIds().size() > 0) {
                    for (String vid : ((HardPort) model).getVlanIds()) {
                        vlanids.add(Integer.valueOf(vid));
                    }
                } else {
                    throw new InventoryException("VLAN ID is not set.");
                }
            } else if (model instanceof LagPort) {
                if (((LagPort) model).getVlanIds().size() > 0) {
                    for (String vid : ((LagPort) model).getVlanIds()) {
                        vlanids.add(Integer.valueOf(vid));
                    }
                } else {
                    throw new InventoryException("VLAN ID is not set.");
                }
            }

            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            VlanIfBindingCommandBuilder builder = null;

            for (Integer vlanid : vlanids) {
                boolean hasvlanofPort = false;
                VlanIfDto vlanif = VlanUtil.getSwitchVlanIf(port.getNode(), vlanid);
                for (PortDto prt : vlanif.getTaggedVlans()) {
                    if (DtoUtil.isSameMvoEntity(prt, port)) {
                        hasvlanofPort = true;
                    }
                }
                for (PortDto prt : vlanif.getUntaggedVlans()) {
                    if (DtoUtil.isSameMvoEntity(prt, port)) {
                        hasvlanofPort = true;
                    }
                }
                if (vlanif != null && hasvlanofPort) {
                    if (VlanUtil.isSwitchVlanIf((VlanIfDto) vlanif)) {
                        builder = new VlanIfBindingCommandBuilder((VlanIfDto) vlanif, user);
                        builder.removeTaggedPort(port);
                        builder.setPreCheckEnable(false);
                        BuildResult result = builder.buildCommand();
                        if (BuildResult.NO_CHANGES == result) {
                            return;
                        } else if (BuildResult.FAIL == result) {
                            throw new IllegalStateException("Update Fail.");
                        }
                        commandBuilderList.add(builder);
                    } else if (!VlanUtil.isSwitchVlanIf((VlanIfDto) vlanif)) {
                        throw new InventoryException("It is not a targeted device.Node:" + port.getNode());
                    }
                } else if (vlanif == null || !hasvlanofPort) {
                    throw new InventoryException("The specified VLAN ID does not exist on the target port. VLAN ID = " + vlanid);
                }
            }
            ShellConnector.getInstance().executes(commandBuilderList);
        } catch (InventoryException e) {
            log.debug(port.getAbsoluteName(), e);
            throw e;
        } catch (IllegalStateException e) {
            log.debug(port.getAbsoluteName(), e);
            throw e;
        } catch (RuntimeException e) {
            log.debug(port.getAbsoluteName(), e);
            throw e;
        }
    }
}