package voss.discovery.agent.cisco.ext;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class StormControlBroadcastLevelRenderer extends SimpleExtInfoRenderer<Integer> {

    public StormControlBroadcastLevelRenderer(VlanModel model) {
        super(CiscoExtInfoNames.STORMCONTROL_BROADCAST_LEVEL, model);
    }

    @Override
    public void set(Integer value) {
        if (value != null && value.intValue() == 10000) {
            return;
        }
        super.set(value);
    }

    @Override
    public Integer get() {
        Integer value = super.get();
        if (value == null) {
            return null;
        } else if (value.intValue() == 10000) {
            return null;
        }
        return value;
    }
}