package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import naef.dto.vpls.VplsIfDto;
import net.phalanx.core.models.Vpls;
import voss.multilayernms.inventory.config.NmsCoreVplsConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.VplsRenderingUtil;
import voss.multilayernms.inventory.renderer.VplsRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VplsModelCreator {

    public static Vpls createModel(VplsIfDto vpls, String inventoryId) throws IOException {
        Vpls model = new Vpls();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreVplsConfiguration.getInstance().getPropertyFields()) {
            String value = VplsRenderingUtil.rendering(vpls, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText(VplsRenderer.getVplsId(vpls));

        return model;
    }

}