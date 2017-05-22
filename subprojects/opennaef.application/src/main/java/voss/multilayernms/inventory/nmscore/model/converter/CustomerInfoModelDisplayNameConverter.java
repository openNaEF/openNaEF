package voss.multilayernms.inventory.nmscore.model.converter;

import net.phalanx.core.models.CustomerInfo;
import voss.multilayernms.inventory.config.NmsCoreCustomerInfoConfiguration;

import java.io.IOException;

public class CustomerInfoModelDisplayNameConverter extends DisplayNameConverter {

    public CustomerInfoModelDisplayNameConverter() throws IOException {
        super(CustomerInfo.class, NmsCoreCustomerInfoConfiguration.getInstance());
    }

}