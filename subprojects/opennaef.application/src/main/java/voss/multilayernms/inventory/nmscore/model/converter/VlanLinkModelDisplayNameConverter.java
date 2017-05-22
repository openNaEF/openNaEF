package voss.multilayernms.inventory.nmscore.model.converter;

import net.phalanx.core.models.VlanLink;
import voss.multilayernms.inventory.config.NmsCoreVlanLinkConfiguration;

import java.io.IOException;

public class VlanLinkModelDisplayNameConverter extends DisplayNameConverter {

    public VlanLinkModelDisplayNameConverter() throws IOException {
        super(VlanLink.class, NmsCoreVlanLinkConfiguration.getInstance());
    }

}