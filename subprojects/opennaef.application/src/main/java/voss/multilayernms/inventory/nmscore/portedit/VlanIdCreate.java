package voss.multilayernms.inventory.nmscore.portedit;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.builder.VlanCommandBuilder;
import voss.nms.inventory.util.VlanUtil;

import java.io.IOException;

public class VlanIdCreate implements IPortEditUpdate {
    private VlanEditModel model;
    private String user;

    public VlanIdCreate(VlanEditModel model, String user) {
        this.model = model;
        this.user = user;
    }

    @Override
    public void update() throws RuntimeException, IOException, InventoryException, ExternalServiceException {
        Vlan model = (Vlan) this.model;
        try {
            VlanIdPoolDto pool = VlanUtil.getPool(model.getVlanPoolName());
            Integer vlanId = Integer.valueOf(model.getVlanId());

            VlanCommandBuilder builder = new VlanCommandBuilder(pool, user);
            builder.setVlanID(vlanId);
            BuildResult result = builder.buildCommand();
            if (BuildResult.NO_CHANGES == result) {
                return;
            } else if (BuildResult.FAIL == result) {
                throw new IllegalStateException("Failed to create VLAN ID.");
            }
            ShellConnector.getInstance().execute(builder);
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