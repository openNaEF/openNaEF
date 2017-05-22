package voss.multilayernms.inventory.nmscore.web.flow.state;

import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.io.IOException;


public interface State {

    StateId getStateId();

    void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException;

}