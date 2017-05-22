package voss.multilayernms.inventory.nmscore.web.flow.state;

import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;

import java.io.IOException;


public class UnificUIModifyState extends UnificUIViewState implements ModifyState {

    public UnificUIModifyState(StateId stateId) {
        super(stateId);
    }

    public Object getTargets(FlowContext context) throws IOException {
        return Operation.getTargets(context);
    }

    public String getUserName(FlowContext context) throws IOException {
        return context.getUser();
    }

}