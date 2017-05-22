package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import naef.dto.vrf.VrfIfDto;
import net.phalanx.core.models.Vrf;
import voss.multilayernms.inventory.config.NmsCoreVrfConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.VrfRenderingUtil;
import voss.multilayernms.inventory.renderer.VrfRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VrfModelCreator {

    public static Vrf createModel(VrfIfDto vrf, String inventoryId) throws IOException {
        Vrf model = new Vrf();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreVrfConfiguration.getInstance().getPropertyFields()) {
            String value = VrfRenderingUtil.rendering(vrf, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText(VrfRenderer.getVpnId(vrf));

        return model;
    }

}