package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.FacilityStatusUpdaterFactory;
import voss.multilayernms.inventory.nmscore.view.datamodifier.FacilityStatusUpdater;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.util.Collection;
import java.util.List;

public class InventoryUpdateState extends UnificUIModifyState implements ModifyState {

    private final Logger log = LoggerFactory.getLogger(InventoryUpdateState.class);

    public InventoryUpdateState(StateId stateId) {
        super(stateId);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            Collection<? extends IModel> updateTarget = (Collection<? extends IModel>) getTargets(context);
            FacilityStatusUpdater updater = FacilityStatusUpdaterFactory.getFacilityStatusUpdater(updateTarget, getUserName(context));
            List<? extends IModel> result = updater.update();
            setXmlObject(result);

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

}