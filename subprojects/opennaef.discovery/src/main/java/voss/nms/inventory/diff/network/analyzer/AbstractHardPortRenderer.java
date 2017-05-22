package voss.nms.inventory.diff.network.analyzer;

import voss.model.VlanModel;

import java.util.Map;

public abstract class AbstractHardPortRenderer<T extends VlanModel> extends AbstractRenderer<T> {

    public AbstractHardPortRenderer(T model, String parentID, int depth, Map<String, String> map) {
        super(model, parentID, depth, map);
    }
}