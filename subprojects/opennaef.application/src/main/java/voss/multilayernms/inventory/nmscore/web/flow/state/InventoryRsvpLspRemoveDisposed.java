package voss.multilayernms.inventory.nmscore.web.flow.state;

import net.phalanx.core.models.LabelSwitchedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.datamodifier.RsvpLspRemover;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;


public class InventoryRsvpLspRemoveDisposed extends UnificUIModifyState {

    private final Logger log = LoggerFactory.getLogger(InventoryRsvpLspRemoveDisposed.class);

    public InventoryRsvpLspRemoveDisposed(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        try {
            setXmlObject(new RsvpLspRemover((LabelSwitchedPath) getTargets(context), getUserName(context)).remove());

            super.execute(context);
        } catch (Exception e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

}