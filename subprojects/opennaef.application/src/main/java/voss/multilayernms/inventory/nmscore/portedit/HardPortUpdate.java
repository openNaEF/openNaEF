package voss.multilayernms.inventory.nmscore.portedit;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.*;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthPortDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.nms.inventory.builder.LinkCommandBuilder;
import voss.nms.inventory.builder.PhysicalPortCommandBuilder;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HardPortUpdate implements IPortEditUpdate {
    private HardPortDto port;
    private PortEditModel model;
    private String user;

    public HardPortUpdate(PortDto port, PortEditModel model, String user) {
        this.port = (HardPortDto) port;
        this.model = model;
        this.user = user;
    }

    @Override
    public void update() throws RuntimeException, IOException, InventoryException, ExternalServiceException {
        HardPort model = (HardPort) this.model;

        try {
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            NodeElementDto owner = port.getOwner();
            if (owner instanceof JackDto) {
                owner = owner.getOwner();
            }
            PhysicalPortCommandBuilder builder = new PhysicalPortCommandBuilder(owner, port, user);

            PortType ifType = PortType.valueOf(model.getIfType());

            switch (ifType) {
                case ATM:
                    builder.setConstraint(AtmPortDto.class);
                    break;
                case POS:
                    builder.setConstraint(PosPortDto.class);
                    break;
                case SERIAL:
                    builder.setConstraint(SerialPortDto.class);
                    break;
                case ETHERNET:
                    builder.setConstraint(EthPortDto.class);
                    break;
                default:
                    throw new IllegalArgumentException("not supported-type: " + ifType.getCaption());
            }

            if (!(model.getIpAddress() == null || model.getIpAddress().isEmpty())) {
                if (model.getVpnPrefix() != null) {
                    builder.setNewIpAddress(model.getVpnPrefix(), model.getIpAddress(), model.getSubnetLength());
                } else {
                    builder.setNewIpAddress(null, model.getIpAddress(), model.getSubnetLength());
                }
            } else if (model.getIpAddress() == null || model.getIpAddress().isEmpty()) {
                log.debug("HardPortUpdate setNewIpAddress(null, null, null)");
                builder.setNewIpAddress(null, null, null);
            }
            if (!model.getAdminStatus().equals("N/A")) {
                builder.setAdminStatus(model.getAdminStatus());
            }
            builder.setPortDescription(model.getDescription());
            builder.setBandwidth(model.getBandwidth());

            if (model.getPortMode() != null && model.getSwitchPortMode() != null) {
                builder.setPortMode(model.getPortMode());
                builder.setSwitchPortMode(model.getSwitchPortMode());
            } else if (model.getPortMode() == null && model.getSwitchPortMode() == null) {
            }

            builder.setValue(MPLSNMS_ATTR.PORTSPEED_ADMIN, model.getAdministrativeSpeed());
            builder.setValue(MPLSNMS_ATTR.DUPLEX_ADMIN, model.getAdministrativeDuplex());
            builder.setValue(MPLSNMS_ATTR.AUTO_NEGO, model.getAutoNegotiation());
            builder.setValue(CustomerConstants.NOTICES, model.getNotices());

            String stormControlBroadcastLevel = model.getStormcontrolBroadcastLevel();
            if (stormControlBroadcastLevel != null && !stormControlBroadcastLevel.isEmpty()) {
                builder.setStormControlBroadcastLevel(stormControlBroadcastLevel);
            }

            List<String> scAction = model.getStormcontrolActions();
            List<String> scActionCurrent = PortRenderer.getStormControlActions(port);
            if (scActionCurrent != null) {
                for (String action : scActionCurrent) {
                    builder.removeStormControlAction(action);
                }
            }
            if (scAction != null) {
                for (String action : scAction) {
                    if (action != null && !action.isEmpty()) {
                        builder.addStormControlAction(action);
                    }
                }
            }

            builder.buildCommand();
            commandBuilderList.add(builder);

            String connectPort = model.getConnectPortMvoId();
            if (connectPort != null && !connectPort.equals("")) {
                log.debug("HardPortUpdate link builder start");
                PortDto port2 = MplsNmsInventoryConnector.getInstance().getMvoDto(connectPort, PortDto.class);

                for (NetworkDto link : port2.getNetworks()) {
                    if (link instanceof LinkDto) {
                        throw new IllegalArgumentException(port.getIfname() + " is already connected");
                    }
                }

                for (NetworkDto link : port.getNetworks()) {
                    if (link instanceof LinkDto) {
                        log.debug("link delete");
                        CommandBuilder linkBuilder = new LinkCommandBuilder((LinkDto) link, user);
                        linkBuilder.buildDeleteCommand();
                        commandBuilderList.add(linkBuilder);
                    }
                }

                CommandBuilder linkBuilder = new LinkCommandBuilder((PortDto) port, port2, user);
                linkBuilder.buildCommand();
                commandBuilderList.add(linkBuilder);
            }

            ShellConnector.getInstance().executes(commandBuilderList);
        } catch (InventoryException e) {
            log.debug("InventoryException", e);
            if (e.getCause() == null) {
                throw new InventoryException(e);
            } else {
                throw new InventoryException(e.getCause().getMessage());
            }
        } catch (ExternalServiceException e) {
            log.debug("ExternalServiceException", e);
            if (e.getCause() == null) {
                throw new ExternalServiceException(e);
            } else {
                throw new ExternalServiceException(e.getCause().getMessage());
            }
        } catch (IOException e) {
            log.debug("IOException", e);
            if (e.getCause() == null) {
                throw new IOException(e);
            } else {
                throw new IOException(e.getCause().getMessage());
            }
        } catch (RuntimeException e) {
            log.debug("RuntimeException", e);
            if (e.getCause() == null) {
                throw new RuntimeException(e);
            } else {
                throw new RuntimeException(e.getCause().getMessage());
            }
        }
    }

}