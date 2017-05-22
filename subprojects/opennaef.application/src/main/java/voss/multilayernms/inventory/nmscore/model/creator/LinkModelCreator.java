package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import jp.iiga.nmt.core.model.PhysicalLink;
import naef.dto.LinkDto;
import naef.dto.ip.IpSubnetDto;
import voss.multilayernms.inventory.config.NmsCoreLinkConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.LinkRenderingUtil;
import voss.multilayernms.inventory.renderer.L2LinkRenderer;
import voss.multilayernms.inventory.renderer.LinkRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LinkModelCreator {

    public static PhysicalLink createModel(IpSubnetDto link, String inventoryId) throws IOException {
        PhysicalLink model = new PhysicalLink();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreLinkConfiguration.getInstance().getPropertyFields()) {
            properties.put(key, LinkRenderingUtil.rendering(link, key));
        }

        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText(LinkRenderer.getName(link));

        return model;
    }

    public static PhysicalLink createModel(LinkDto link, String inventoryId) throws IOException {
        PhysicalLink model = new PhysicalLink();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreLinkConfiguration.getInstance().getPropertyFields()) {
            properties.put(key, LinkRenderingUtil.rendering(link, key));
        }

        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText(L2LinkRenderer.getName(link));

        return model;
    }

}