package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.Diagram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.datamodifier.LayoutSaver;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;

public class InventorySaveLayoutViewState extends UnificUIModifyState {

    private final Logger log = LoggerFactory.getLogger(InventoryRefreshState.class);

    public InventorySaveLayoutViewState(StateId stateId) {
        super(stateId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(new LayoutSaver((Diagram) getTargets(context), getUserName(context)).save());

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

}