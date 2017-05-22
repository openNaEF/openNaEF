package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import naef.dto.vlan.VlanDto;
import net.phalanx.core.models.Vlan;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.config.NmsCoreVlanConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.VlanRenderingUtil;
import voss.multilayernms.inventory.renderer.VlanRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class VlanModelCreator {
    public static Vlan createModel(VlanDto vlan) throws IOException {
        Vlan model = new Vlan();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreVlanConfiguration.getInstance().getPropertyFields()) {
            String value = VlanRenderingUtil.rendering(vlan, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(VlanRenderer.getVlanId(vlan));
        model.setText(VlanRenderer.getVlanId(vlan));
        model.setPropertyValue("mvo-id", DtoUtil.getMvoId(vlan).toString());
        return model;
    }
}