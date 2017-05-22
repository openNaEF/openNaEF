package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import naef.dto.vlan.VlanLinkDto;
import net.phalanx.core.models.VlanLink;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.config.NmsCoreVlanLinkConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.VlanLinkRenderingUtil;
import voss.multilayernms.inventory.renderer.VlanLinkRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VlanLinkModelCreator {

    public static VlanLink createModel(VlanLinkDto link) throws IOException {
        VlanLink model = new VlanLink();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreVlanLinkConfiguration.getInstance().getPropertyFields()) {
            properties.put(key, VlanLinkRenderingUtil.rendering(link, key));
        }

        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(DtoUtil.getMvoId(link).toString());
        model.setText(VlanLinkRenderer.getName(link));
        model.setPropertyValue("mvo-id", DtoUtil.getMvoId(link).toString());
        return model;
    }

}