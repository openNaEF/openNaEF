package voss.multilayernms.inventory.nmscore.web.flow.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.FilteringFieldsMakerFactory;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;

public class InventoryFilteringFieldsState extends UnificUIViewState {

    static Logger log = LoggerFactory.getLogger(InventoryFilteringFieldsState.class);

    public InventoryFilteringFieldsState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(FilteringFieldsMakerFactory.getFilteringFieldsMaker(getObjectType(context)).makeFilteringFieldsView());
            super.execute(context);
        } catch (Exception e) {
            log.error(e.toString());
            throw new ServletException(e);
        }
    }

    private String getObjectType(FlowContext context) {
        return Operation.getObjectType(context);
    }
}