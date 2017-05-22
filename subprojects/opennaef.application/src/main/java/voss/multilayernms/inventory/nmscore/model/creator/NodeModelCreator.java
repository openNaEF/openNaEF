package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.Device;
import jp.iiga.nmt.core.model.MetaData;
import naef.dto.NodeDto;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.config.NmsCoreNodeConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.NodeRenderingUtil;
import voss.multilayernms.inventory.renderer.NodeRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NodeModelCreator {

    public static Device createModel(NodeDto node, String inventoryId) throws IOException {
        Device model = new Device();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreNodeConfiguration.getInstance().getPropertyFields()) {
            String value = NodeRenderingUtil.rendering(node, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText(NodeRenderer.getNodeName(node));
        model.setPropertyValue("mvo-id", DtoUtil.getMvoId(node).toString());

        return model;
    }

}