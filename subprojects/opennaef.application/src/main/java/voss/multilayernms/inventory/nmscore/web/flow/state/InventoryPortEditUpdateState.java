package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.*;
import naef.dto.PortDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.portedit.HardPortUpdate;
import voss.multilayernms.inventory.nmscore.portedit.LagPortUpdate;
import voss.multilayernms.inventory.nmscore.portedit.SubnetIpAssignPort;
import voss.multilayernms.inventory.nmscore.portedit.VlanIfUpdate;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryPortEditUpdateState extends UnificUIViewState {

    public InventoryPortEditUpdateState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            PortEditModel model = (PortEditModel) Operation.getTargets(context);
            String user = context.getUser();

            String modelVersion = model.getVersion();
            if (model instanceof SubnetIp) {
                new SubnetIpAssignPort(model, user).create();
            } else if (modelVersion == null) {
                if (model instanceof LagPort) {
                    new LagPortUpdate(model, user).create();
                } else if (model instanceof SubnetIp) {
                    new SubnetIpAssignPort(model, user).create();
                } else {
                    throw new IllegalArgumentException("Target is unknown.");
                }
            } else {
                PortDto port = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), PortDto.class);
                if (port == null) {
                    throw new InventoryException("port is null");
                }
                log.debug("port: " + port.getIfname() + " Version: " + model.getVersion() + " / " + DtoUtil.getMvoVersionString(port));

                if (!DtoUtil.getMvoVersionString(port).equals(model.getVersion().toString())) {
                    throw new InventoryException("Version mismatch.");
                } else {
                    if (model instanceof HardPort) {
                        new HardPortUpdate(port, model, user).update();
                    } else if (model instanceof VlanPort) {
                        new VlanIfUpdate(port, model, user).update();
                    } else if (model instanceof LagPort) {
                        new LagPortUpdate(port, model, user).update();
                    } else {
                        throw new IllegalArgumentException("Target is unknown.");
                    }
                }
            }

            super.execute(context);
        } catch (InventoryException e) {
            log.error("" + e);
            throw e;
        } catch (ExternalServiceException e) {
            log.error("" + e);
            throw e;
        } catch (IOException e) {
            log.error("" + e);
            throw e;
        } catch (RuntimeException e) {
            log.error("" + e);
            throw e;
        }
    }
}