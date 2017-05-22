package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.PortDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.portedit.delete.L2LinkDelete;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryL2LinkDeleteState extends UnificUIViewState {

    public InventoryL2LinkDeleteState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            PortEditModel model = (PortEditModel) Operation.getTargets(context);
            String user = context.getUser();
            PortDto port = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), PortDto.class);
            if (port == null) {
                throw new InventoryException("port is null");
            }
            if (user == null) {
                throw new InventoryException("user is null");
            }
            log.debug("port: " + port.getIfname() + " Version: " + model.getVersion() + " / " + DtoUtil.getMvoVersionString(port) + " " + user);

            if (!DtoUtil.getMvoVersionString(port).equals(model.getVersion().toString())) {
                throw new InventoryException("Version mismatch.");
            } else {
                if (model instanceof HardPort || model instanceof LagPort) {
                    new L2LinkDelete(port, model, user).delete();
                } else {
                    throw new IllegalArgumentException("Target is unknown.");
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