package voss.multilayernms.inventory.nmscore.portedit;

import jp.iiga.nmt.core.model.portedit.PortEditModel;
import jp.iiga.nmt.core.model.portedit.SubnetIp;
import naef.dto.*;
import naef.dto.ip.IpSubnetDto;
import naef.dto.vlan.VlanIfDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.SubnetRenderer;
import voss.nms.inventory.builder.PhysicalPortCommandBuilder;
import voss.nms.inventory.builder.PortCommandBuilder;
import voss.nms.inventory.builder.VlanIfCommandBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SubnetIpAssignPort implements IPortEditUpdate {
    private PortDto port;
    private PortEditModel model;
    private String user;

    public SubnetIpAssignPort(PortDto port, PortEditModel model, String user) {
        this.port = port;
        this.model = model;
        this.user = user;
    }

    public SubnetIpAssignPort(PortEditModel model, String user) {
        this.port = null;
        this.model = model;
        this.user = user;
    }

    @Override
    public void update() throws RuntimeException, IOException, InventoryException, ExternalServiceException {
        IpSubnetDto subnet = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getSubnetMvoId(), IpSubnetDto.class);
        if (!model.getVersion().equals(DtoUtil.getMvoVersionString(subnet).toString())) {
            throw new IllegalArgumentException("Version mismatch.");
        }
        try {
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            NodeElementDto owner = port.getOwner();
            if (owner instanceof JackDto) {
                owner = owner.getOwner();
            }
            PortCommandBuilder builder = null;

            if (port instanceof HardPortDto) {
                builder = new PhysicalPortCommandBuilder(owner, (HardPortDto) port, user);

                if (SubnetRenderer.getVpnPrefix(subnet) != null) {
                    builder.setNewIpAddress(SubnetRenderer.getVpnPrefix(subnet), ((SubnetIp) model).getIpAddress(), ((SubnetIp) model).getSubnetLength());
                } else {
                    builder.setNewIpAddress(null, ((SubnetIp) model).getIpAddress(), ((SubnetIp) model).getSubnetLength());
                }
            } else if (port instanceof VlanIfDto) {
                if (owner instanceof NodeDto) {
                    builder = new VlanIfCommandBuilder((NodeDto) owner, (VlanIfDto) port, user);
                } else if (owner instanceof PortDto) {
                    builder = new VlanIfCommandBuilder((PortDto) owner, (VlanIfDto) port, user);
                } else {
                    throw new IllegalArgumentException("cannot cast:" + port.getAbsoluteName());
                }
                if (SubnetRenderer.getVpnPrefix(subnet).isEmpty() || SubnetRenderer.getVpnPrefix(subnet) == null) {
                    builder.setNewIpAddress(null, ((SubnetIp) model).getIpAddress(), ((SubnetIp) model).getSubnetLength());
                } else {
                    builder.setNewIpAddress(SubnetRenderer.getVpnPrefix(subnet), ((SubnetIp) model).getIpAddress(), ((SubnetIp) model).getSubnetLength());
                }
            }
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

    public void create() throws RuntimeException, IOException, InventoryException, ExternalServiceException {
        port = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), PortDto.class);
        update();
    }
}