package voss.discovery.agent.fortigate;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.discovery.agent.fortigate.FortigateMib.FortigateHaMode;
import voss.model.VlanModel;

public class HaModeRenderer extends SimpleExtInfoRenderer<Integer> {

    public HaModeRenderer(VlanModel model) {
        super(FortigateExtInfoNames.HA_MODE, model);
    }

    public FortigateHaMode getMode() {
        Integer value = get();
        if (value == null) {
            return FortigateHaMode.STANDALONE;
        }
        FortigateHaMode mode = FortigateHaMode.getByMode(value.intValue());
        if (mode != null) {
            return mode;
        }
        return FortigateHaMode.STANDALONE;
    }
}