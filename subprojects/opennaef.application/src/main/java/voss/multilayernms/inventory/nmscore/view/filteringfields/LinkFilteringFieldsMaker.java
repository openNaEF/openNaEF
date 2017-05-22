package voss.multilayernms.inventory.nmscore.view.filteringfields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.config.INmsCoreInventoryObjectConfiguration;
import voss.multilayernms.inventory.config.NmsCoreLinkConfiguration;
import voss.multilayernms.inventory.constants.LinkFacilityStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LinkFilteringFieldsMaker extends FilteringFieldsMaker {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(LinkFilteringFieldsMaker.class);

    @Override
    protected INmsCoreInventoryObjectConfiguration getConfig() throws IOException {
        return NmsCoreLinkConfiguration.getInstance();
    }

    @Override
    protected List<String> getFacilityStatusList() throws ExternalServiceException, IOException {
        List<String> list = new ArrayList<String>();
        list.add("");
        for (LinkFacilityStatus status : LinkFacilityStatus.values()) {
            list.add(status.getDisplayString());
        }
        return list;
    }

}