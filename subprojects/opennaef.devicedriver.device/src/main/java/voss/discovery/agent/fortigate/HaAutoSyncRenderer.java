package voss.discovery.agent.fortigate;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class HaAutoSyncRenderer extends SimpleExtInfoRenderer<Integer> {

    public HaAutoSyncRenderer(VlanModel model) {
        super(FortigateExtInfoNames.HA_AUTOSYNC, model);
    }

    public boolean isAutoSyncEnable() {
        Integer value = get();
        if (value == null) {
            return false;
        }
        return value.intValue() == 2;
    }
}