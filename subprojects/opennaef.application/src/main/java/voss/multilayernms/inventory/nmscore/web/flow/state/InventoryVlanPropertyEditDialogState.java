package voss.multilayernms.inventory.nmscore.web.flow.state;

import net.phalanx.core.expressions.ObjectFilterQuery;
import voss.multilayernms.inventory.nmscore.view.portedit.VlanPropertyEditDialogMaker;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;


public class InventoryVlanPropertyEditDialogState extends UnificUIViewState {

    public InventoryVlanPropertyEditDialogState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(VlanPropertyEditDialogMaker.getDialogMaker(getQuery(context)).makeDialog());
            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

    private ObjectFilterQuery getQuery(FlowContext context) throws IOException {
        return Operation.getQuery(context);
    }
}