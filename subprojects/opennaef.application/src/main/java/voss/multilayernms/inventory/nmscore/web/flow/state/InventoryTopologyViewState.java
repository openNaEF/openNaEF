package voss.multilayernms.inventory.nmscore.web.flow.state;

import net.phalanx.core.expressions.ObjectFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.TopologyViewMakerFactory;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import javax.servlet.ServletException;
import java.io.IOException;


public class InventoryTopologyViewState extends UnificUIViewState {

    private final Logger log = LoggerFactory.getLogger(InventoryTopologyViewState.class);

    public InventoryTopologyViewState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(TopologyViewMakerFactory.getTopologyViewMaker(getQuery(context)).makeTopologyView());

            super.execute(context);
        } catch (Exception e) {
            log.error(e.toString());
            throw new ServletException(e);
        }
    }

    private ObjectFilterQuery getQuery(FlowContext context) throws IOException {
        return Operation.getQuery(context);
    }

}