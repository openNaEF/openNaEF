package voss.discovery.agent.cisco.ext;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class StormControlActionRenderer extends SimpleExtInfoRenderer<Integer> {

    public StormControlActionRenderer(VlanModel model) {
        super(CiscoExtInfoNames.STORMCONTROL_ACTION_SHUTDOWN, model);
    }

    public boolean isShutdownEnable() {
        Integer value = get();
        if (value == null) {
            return false;
        }
        return value.intValue() == 2;
    }

}