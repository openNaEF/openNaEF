package voss.multilayernms.inventory.diff.web.flow.event;

import voss.multilayernms.inventory.diff.web.flow.FlowContext;


public class Any implements EventCondition {

    public Any() {
        super();
    }

    @Override
    public boolean matches(FlowContext context) {
        return true;
    }

}