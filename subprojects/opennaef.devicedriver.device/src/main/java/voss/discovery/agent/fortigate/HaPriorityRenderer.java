package voss.discovery.agent.fortigate;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class HaPriorityRenderer extends SimpleExtInfoRenderer<Integer> {

    public HaPriorityRenderer(VlanModel model) {
        super(FortigateExtInfoNames.HA_PRIORITY, model);
    }
}