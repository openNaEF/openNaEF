package voss.discovery.agent.cisco.ext;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class StormControlMulticastLevelRenderer extends SimpleExtInfoRenderer<Integer> {

    public StormControlMulticastLevelRenderer(VlanModel model) {
        super(CiscoExtInfoNames.STORMCONTROL_MULTICAST_LEVEL, model);
    }

}