package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import naef.dto.ip.IpSubnetDto;
import net.phalanx.core.models.Subnet;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.config.NmsCoreSubnetConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.SubnetRenderingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class SubnetModelCreator {
    public static Subnet createModel(IpSubnetDto subnet, String inventoryId) throws IOException {
        Subnet model = new Subnet();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreSubnetConfiguration.getInstance().getPropertyFields()) {
            String value = SubnetRenderingUtil.rendering(subnet, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(DtoUtil.getMvoId(subnet).toString());
        model.setPropertyValue("mvo-id", DtoUtil.getMvoId(subnet).toString());
        model.setText(inventoryId);

        return model;
    }
}