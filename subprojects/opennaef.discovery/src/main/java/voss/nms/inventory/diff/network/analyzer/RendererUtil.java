package voss.nms.inventory.diff.network.analyzer;

import voss.model.VlanModel;

public class RendererUtil {

    public static VlanModel getModel(Renderer renderer) {
        if (renderer == null) {
            return null;
        }
        return renderer.getModel();
    }
}