package voss.multilayernms.inventory.nmscore.portedit;

import jp.iiga.nmt.core.model.portedit.PortEditModel;
import jp.iiga.nmt.core.model.portedit.VlanPort;
import naef.dto.NodeDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.nmscore.constraints.PortEditConstraints;
import voss.nms.inventory.builder.VlanIfCommandBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VlanIfUpdate implements IPortEditUpdate {
    private VlanIfDto port;
    private PortEditModel model;
    private String user;

    public VlanIfUpdate(PortDto port, PortEditModel model, String user) {
        this.port = (VlanIfDto) port;
        this.model = model;
        this.user = user;
    }

    @Override
    public void update() throws RuntimeException, IOException, InventoryException, ExternalServiceException {
        VlanPort model = (VlanPort) this.model;

        try {
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            VlanIfCommandBuilder builder = null;

            NodeElementDto owner = port.getOwner();
            if (owner instanceof NodeDto) {
                builder = new VlanIfCommandBuilder((NodeDto) owner, port, user);
            } else if (owner instanceof PortDto) {
                builder = new VlanIfCommandBuilder((PortDto) owner, port, user);
            } else {
                throw new IllegalArgumentException("cannot cast " + port.getAbsoluteName());
            }
            builder.setVlanId(model.getVlanId());
            builder.setIfName(model.getIfName());
            if (model.getConfigName() != null) {
                log.debug("ConfigName written by Phalanx =  " + model.getConfigName());
                if (PortEditConstraints.isPortSetConfigNameEnabled(port)) {
                    builder.setConfigName(model.getConfigName());
                }
            }
            builder.setSviEnable(model.isSviPort());
            if (!model.getAdminStatus().equals("N/A")) {
                builder.setAdminStatus(model.getAdminStatus());
            }
            if (!(model.getIpAddress() == null || model.getIpAddress().isEmpty())) {
                if (model.getVpnPrefix() != null) {
                    builder.setNewIpAddress(model.getVpnPrefix(), model.getIpAddress(), model.getSubnetMask());
                } else {
                    builder.setNewIpAddress(null, model.getIpAddress(), model.getSubnetMask());
                }
            } else if (model.getIpAddress() == null || model.getIpAddress().isEmpty()) {
                builder.setNewIpAddress(null, null, null);
            }
            builder.setBandwidth(model.getBandwidth());
            builder.setPortDescription(model.getDescription());
            builder.setValue(CustomerConstants.NOTICES, model.getNotices());
            builder.setPurpose(model.getPurpose());
            builder.setEndUserName(model.getUser());

            BuildResult result = builder.buildCommand();
            if (BuildResult.NO_CHANGES == result) {
                return;
            } else if (BuildResult.FAIL == result) {
                throw new IllegalStateException("Update Fail.");
            }
            commandBuilderList.add(builder);
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