package voss.discovery.agent.fortigate;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.model.VlanModel;

public class VdomIdRenderer extends SimpleExtInfoRenderer<Integer> {

    public VdomIdRenderer(VlanModel model) {
        super(FortigateExtInfoNames.VDOM_INDEX, model);
    }

}