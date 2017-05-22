package voss.multilayernms.inventory.nmscore.web.flow.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.state.State;

import javax.servlet.ServletException;
import java.io.IOException;


public class Event {

    private final Logger log = LoggerFactory.getLogger(Event.class);

    private EventCondition condition;
    private State state;

    public Event(EventCondition condition, State state) {
        this.condition = condition;
        this.state = state;
    }

    public boolean isAdaptable(FlowContext context) throws ServletException {
        return condition.matches(context);
    }

    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        log.info("execute[" + state.getStateId() + "] userName[" + context.getUser() + "]");
        state.execute(context);
    }

}