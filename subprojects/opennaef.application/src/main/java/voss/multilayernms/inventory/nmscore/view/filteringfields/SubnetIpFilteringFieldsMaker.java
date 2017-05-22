package voss.multilayernms.inventory.nmscore.view.filteringfields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.config.INmsCoreInventoryObjectConfiguration;
import voss.multilayernms.inventory.config.NmsCoreSubnetIpConfiguration;

import java.io.IOException;

public class SubnetIpFilteringFieldsMaker extends FilteringFieldsMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(SubnetIpFilteringFieldsMaker.class);

    @Override
    protected INmsCoreInventoryObjectConfiguration getConfig() throws IOException {
        return NmsCoreSubnetIpConfiguration.getInstance();
    }

}
