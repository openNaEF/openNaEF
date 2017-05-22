package voss.multilayernms.inventory.nmscore.portedit;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.builder.IpSubnetCommandBuilder;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.util.VlanUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VlanIdAllocateSubnetAddress implements IPortEditUpdate {
    private VlanEditModel model;
    private String user;
    private VlanDto vlan;

    public VlanIdAllocateSubnetAddress(VlanEditModel model, String user) {
        this.model = model;
        this.user = user;
    }

    @Override
    public void update() throws RuntimeException, IOException, InventoryException, ExternalServiceException {
        Vlan model = (Vlan) this.model;

        try {
            VlanIdPoolDto pool = VlanUtil.getPool(model.getVlanPoolName());
            Integer vlanId = Integer.valueOf(model.getVlanId());
            String subnetAddress = model.getSubnetAddress();
            String mask = model.getMaskLength();
            String vpnDriver = model.getVpnDriver();
            vlan = VlanUtil.getVlan(pool, vlanId);
            List<CommandBuilder> commandBuilderList = new ArrayList<CommandBuilder>();
            IpSubnetCommandBuilder builder = null;
            InventoryConnector conn = InventoryConnector.getInstance();
            IpSubnetNamespaceDto namespace = conn.getActiveRootIpSubnetNamespace(vpnDriver);
            if (namespace == null) {
                List<IpSubnetNamespaceDto> ipSubnetNamespaces = conn.getActiveRootIpSubnetNamespaces();
                for (IpSubnetNamespaceDto ipSubnetNamespace : ipSubnetNamespaces) {
                    for (IpSubnetNamespaceDto ipSubnetchildren : ipSubnetNamespace.getChildren()) {
                        if (ipSubnetchildren.getName().equals(vpnDriver)) {
                            builder = new IpSubnetCommandBuilder(ipSubnetchildren, user);
                        }
                    }
                }
            } else if (namespace != null) {
                builder = new IpSubnetCommandBuilder(namespace, user);
            }

            builder.setStartAddress(subnetAddress);
            builder.setMaskLength(Integer.valueOf(mask));
            builder.addLowerLayerNetwork(vlan.getAbsoluteName());
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