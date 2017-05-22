package voss.discovery.agent.cisco.ext;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class StormControlUnicastLevelRenderer extends SimpleExtInfoRenderer<Integer> {

    public StormControlUnicastLevelRenderer(VlanModel model) {
        super(CiscoExtInfoNames.STORMCONTROL_UNICAST_LEVEL, model);
    }

}