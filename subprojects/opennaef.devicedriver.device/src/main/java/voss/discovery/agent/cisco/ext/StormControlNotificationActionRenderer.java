package voss.discovery.agent.cisco.ext;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class StormControlNotificationActionRenderer extends SimpleExtInfoRenderer<Integer> {

    public StormControlNotificationActionRenderer(VlanModel model) {
        super(CiscoExtInfoNames.STORMCONTROL_ACTION_TRAP, model);
    }

    public boolean isTrapEnable() {
        Integer value = get();
        if (value == null) {
            return false;
        }
        int val = value.intValue();
        return 1 < val;
    }

}