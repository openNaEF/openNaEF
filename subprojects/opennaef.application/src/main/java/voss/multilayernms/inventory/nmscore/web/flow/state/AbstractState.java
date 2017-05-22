package voss.multilayernms.inventory.nmscore.web.flow.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractState implements State {

    protected final Logger log = LoggerFactory.getLogger(AbstractState.class);
    private final StateId stateId;

    public AbstractState(StateId stateId) {
        this.stateId = stateId;
    }

    @Override
    public StateId getStateId() {
        return stateId;
    }

}