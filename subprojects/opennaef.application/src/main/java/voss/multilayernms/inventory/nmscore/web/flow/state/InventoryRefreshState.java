package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.RefresherFactory;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.util.Collection;

public class InventoryRefreshState extends UnificUIModifyState {

    private final Logger log = LoggerFactory.getLogger(InventoryRefreshState.class);

    public InventoryRefreshState(StateId stateId) {
        super(stateId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(RefresherFactory.getRefresher((Collection<? extends IModel>) getTargets(context), getUserName(context)).refresh());

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

}