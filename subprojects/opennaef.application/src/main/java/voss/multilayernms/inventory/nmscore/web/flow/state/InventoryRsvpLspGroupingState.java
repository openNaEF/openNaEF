package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.datamodifier.RsvpLspGroupingModifier;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.util.List;

public class InventoryRsvpLspGroupingState extends UnificUIModifyState {

    private final Logger log = LoggerFactory.getLogger(InventoryRsvpLspGroupingState.class);

    public InventoryRsvpLspGroupingState(StateId stateId) {
        super(stateId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(new RsvpLspGroupingModifier((List<? extends IModel>) getTargets(context), getUserName(context)).grouping());

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

}