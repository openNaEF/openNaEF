package voss.multilayernms.inventory.nmscore.web.flow.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;

public class UnknownRequestState extends AbstractState {

    private final Logger log = LoggerFactory.getLogger(UnknownRequestState.class);

    public UnknownRequestState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException {
        log.warn("unknown request");
        throw new IllegalArgumentException("unknown request");
    }

}