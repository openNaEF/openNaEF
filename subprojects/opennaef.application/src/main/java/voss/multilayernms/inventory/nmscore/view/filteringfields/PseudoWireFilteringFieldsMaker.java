package voss.multilayernms.inventory.nmscore.view.filteringfields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.config.INmsCoreInventoryObjectConfiguration;
import voss.multilayernms.inventory.config.NmsCorePseudoWireConfiguration;

import java.io.IOException;

public class PseudoWireFilteringFieldsMaker extends FilteringFieldsMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(PseudoWireFilteringFieldsMaker.class);

    @Override
    protected INmsCoreInventoryObjectConfiguration getConfig() throws IOException {
        return NmsCorePseudoWireConfiguration.getInstance();
    }

}