package voss.discovery.agent.fortigate;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class HaGroupIdRenderer extends SimpleExtInfoRenderer<Integer> {

    public HaGroupIdRenderer(VlanModel model) {
        super(FortigateExtInfoNames.HA_GROUP_ID, model);
    }

}