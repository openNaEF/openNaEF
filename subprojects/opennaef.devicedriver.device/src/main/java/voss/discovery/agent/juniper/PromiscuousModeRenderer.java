package voss.discovery.agent.juniper;

import voss.discovery.agent.common.SimpleBooleanExtInfoRenderer;
import voss.model.VlanModel;

public class PromiscuousModeRenderer extends SimpleBooleanExtInfoRenderer {
    public static final String KEY = "promiscuous-mode";

    public PromiscuousModeRenderer(VlanModel model) {
        super(KEY, model);
    }

    public boolean isPromiscuousMode() {
        return get().booleanValue();
    }
}