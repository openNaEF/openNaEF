package voss.multilayernms.inventory.nmscore.web.flow.event;

import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

public class ParameterMatch implements EventCondition {

    private final String parameterName;
    private final String expectedValue;

    public ParameterMatch(String parameterName, String expectedValue) {
        this.parameterName = parameterName;
        this.expectedValue = expectedValue;
    }

    public boolean matches(FlowContext context) {
        String value = context.getParameter(parameterName);
        if (value == null) {
            return false;
        }
        return value.equals(expectedValue);
    }

}