package voss.multilayernms.inventory.diff.web.flow.event;

import voss.multilayernms.inventory.diff.web.flow.FlowContext;

public interface EventCondition {

    boolean matches(FlowContext context);

}