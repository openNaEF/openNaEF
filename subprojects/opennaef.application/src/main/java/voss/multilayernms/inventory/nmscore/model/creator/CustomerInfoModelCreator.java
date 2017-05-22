package voss.multilayernms.inventory.nmscore.model.creator;

import jp.iiga.nmt.core.model.MetaData;
import naef.dto.CustomerInfoDto;
import net.phalanx.core.models.CustomerInfo;
import voss.multilayernms.inventory.config.NmsCoreCustomerInfoConfiguration;
import voss.multilayernms.inventory.nmscore.rendering.CustomerInfoRenderingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerInfoModelCreator {
    public static CustomerInfo createModel(CustomerInfoDto customer, String mvoId) throws IOException {
        CustomerInfo model = new CustomerInfo();

        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : NmsCoreCustomerInfoConfiguration.getInstance().getPropertyFields()) {
            String value = CustomerInfoRenderingUtil.rendering(customer, key);
            properties.put(key, value);
        }
        MetaData data = new MetaData();
        data.setProperties(properties);
        model.setMetaData(data);
        model.setId(mvoId);
        model.setPropertyValue("mvo-id", mvoId);
        model.setText(customer.getAbsoluteName());

        return model;
    }
}