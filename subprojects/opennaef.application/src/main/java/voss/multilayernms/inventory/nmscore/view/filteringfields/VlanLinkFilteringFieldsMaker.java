package voss.multilayernms.inventory.nmscore.view.filteringfields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.config.INmsCoreInventoryObjectConfiguration;
import voss.multilayernms.inventory.config.NmsCoreVlanLinkConfiguration;

import java.io.IOException;

public class VlanLinkFilteringFieldsMaker extends FilteringFieldsMaker {

    Logger log = LoggerFactory.getLogger(VlanLinkFilteringFieldsMaker.class);

    @Override
    protected INmsCoreInventoryObjectConfiguration getConfig() throws IOException {
        return NmsCoreVlanLinkConfiguration.getInstance();
    }

}
