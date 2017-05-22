package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.HardPort;
import jp.iiga.nmt.core.model.portedit.LagPort;
import jp.iiga.nmt.core.model.portedit.PortEditModel;
import naef.dto.PortDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.portedit.delete.VlanUnstackFromPort;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryVlanUnstackFromPortState extends UnificUIViewState {

    public InventoryVlanUnstackFromPortState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            PortEditModel model = (PortEditModel) Operation.getTargets(context);
            String user = context.getUser();

            PortDto port = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), PortDto.class);

            if (model instanceof HardPort || model instanceof LagPort) {
                String version = model.getVersion();
                if (version != null && !version.equals(DtoUtil.getMvoVersionString(port))) {
                    throw new InventoryException("The versions do not match.\nRefreshes the list view.");
                }
                if (model instanceof HardPort) {
                    new VlanUnstackFromPort(port, model, user).delete();
                } else if (model instanceof LagPort) {
                    new VlanUnstackFromPort(port, model, user).delete();
                }
            } else {
                throw new IllegalArgumentException("Target is wrong.");
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
        } catch (ServletException e) {
            log.error("", e);
            throw e;
        }
    }
}