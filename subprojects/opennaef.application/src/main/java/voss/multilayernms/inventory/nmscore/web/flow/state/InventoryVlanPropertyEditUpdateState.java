package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import voss.multilayernms.inventory.nmscore.portedit.VlanPropertyUpdate;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;

public class InventoryVlanPropertyEditUpdateState extends UnificUIViewState {

    public InventoryVlanPropertyEditUpdateState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            VlanEditModel model = (VlanEditModel) Operation.getTargets(context);
            String user = context.getUser();
            if (model instanceof Vlan) {
                new VlanPropertyUpdate(model, user).update();
            }
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }

    }
}