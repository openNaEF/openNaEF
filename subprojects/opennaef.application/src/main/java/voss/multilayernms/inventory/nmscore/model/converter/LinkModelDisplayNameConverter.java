package voss.multilayernms.inventory.nmscore.model.converter;

import jp.iiga.nmt.core.model.PhysicalLink;
import voss.multilayernms.inventory.config.NmsCoreLinkConfiguration;

import java.io.IOException;


public class LinkModelDisplayNameConverter extends DisplayNameConverter {

    public LinkModelDisplayNameConverter() throws IOException {
        super(PhysicalLink.class,
                NmsCoreLinkConfiguration.getInstance());
    }

}