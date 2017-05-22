package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import naef.dto.PortDto;
import net.phalanx.core.models.SubnetIp;
import voss.core.server.database.ATTR;
import voss.multilayernms.inventory.config.NmsCoreSubnetIpConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.SubnetRenderingUtil;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.util.SubnetListUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class SubnetIpModelCreator {
    public static SubnetIp createModel(PortDto port, String inventoryId) throws IOException {
        SubnetIp model = new SubnetIp();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreSubnetIpConfiguration.getInstance().getPropertyFields()) {
            String value = SubnetRenderingUtil.rendering(port, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(inventoryId);
        model.setText(getText(port));

        return model;
    }

    public static SubnetIp createIp(long ip, String id) {
        return createIp(SubnetListUtil.int2IpAddressString(ip), id);
    }

    public static SubnetIp createIp(String addressStr, String id) {
        SubnetIp model = new SubnetIp();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("SubnetIP", addressStr);
        properties.put(ATTR.VPN_PREFIX, id);

        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(addressStr);
        model.setText(id);

        return model;
    }

    private static String getText(PortDto port) {
        return PortRenderer.getVpnPrefix(port);
    }
}