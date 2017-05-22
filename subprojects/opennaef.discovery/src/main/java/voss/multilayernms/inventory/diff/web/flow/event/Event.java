package voss.multilayernms.inventory.diff.web.flow.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.multilayernms.inventory.diff.web.flow.state.State;

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

    public boolean isAdaptable(FlowContext context) {
        return condition.matches(context);
    }

    public void execute(FlowContext context) throws ServletException, IOException {
        log.debug("execute " + state.getStateId() + " / user " + context.getUserName());
        state.execute(context);
    }
}