package voss.multilayernms.inventory.nmscore.web.flow.state;

import net.phalanx.core.expressions.ObjectFilterQuery;
import voss.multilayernms.inventory.nmscore.view.VlanUnstackFromPortDialogMakerFactory;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryVlanUnstackFromPortDialogState extends UnificUIViewState {

    public InventoryVlanUnstackFromPortDialogState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(VlanUnstackFromPortDialogMakerFactory.getDialogMaker(getQuery(context)).makeDialog());
            super.execute(context);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private ObjectFilterQuery getQuery(FlowContext context) throws IOException {
        return Operation.getQuery(context);
    }
}