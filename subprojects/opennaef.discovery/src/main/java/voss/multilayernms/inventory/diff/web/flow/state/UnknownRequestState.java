package voss.multilayernms.inventory.diff.web.flow.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;

import javax.servlet.ServletException;
import java.io.IOException;


public class UnknownRequestState extends AbstractState {

    private final Logger log = LoggerFactory.getLogger(UnknownRequestState.class);

    public UnknownRequestState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        log.warn("unknown request !!");
    }

}