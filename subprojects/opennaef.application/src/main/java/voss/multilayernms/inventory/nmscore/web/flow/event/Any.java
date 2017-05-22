package voss.multilayernms.inventory.nmscore.web.flow.event;

import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

public class Any implements EventCondition {

    public Any() {
        super();
    }

    @Override
    public boolean matches(FlowContext context) {
        return true;
    }

}