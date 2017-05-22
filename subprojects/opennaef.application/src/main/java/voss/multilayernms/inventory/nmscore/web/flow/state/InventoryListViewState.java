package voss.multilayernms.inventory.nmscore.web.flow.state;

import net.phalanx.core.expressions.ObjectFilterQuery;
import voss.multilayernms.inventory.nmscore.view.ListViewMakerFactory;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryListViewState extends UnificUIViewState {

    public InventoryListViewState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(ListViewMakerFactory.getListViewMaker(getQuery(context)).makeListView());

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