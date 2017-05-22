package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import jp.iiga.nmt.core.model.portedit.VlanLink;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.portedit.delete.VlanLinkDelete;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryVlanLinkDeleteState extends UnificUIViewState {

    public InventoryVlanLinkDeleteState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            VlanEditModel model = (VlanEditModel) Operation.getTargets(context);
            String user = context.getUser();
            if (model instanceof VlanLink) {
                new VlanLinkDelete(model, user).delete();
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