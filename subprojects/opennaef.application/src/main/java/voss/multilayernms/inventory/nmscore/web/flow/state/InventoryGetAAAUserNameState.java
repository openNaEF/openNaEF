package voss.multilayernms.inventory.nmscore.web.flow.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryGetAAAUserNameState extends UnificUIViewState {

    private final static Logger log = LoggerFactory.getLogger(InventoryGetAAAUserNameState.class);

    public InventoryGetAAAUserNameState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        setXmlObject(context.getUser());
        super.execute(context);
    }
}