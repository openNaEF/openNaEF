package voss.multilayernms.inventory.nmscore.model.converter;

import net.phalanx.core.models.Subnet;
import voss.multilayernms.inventory.config.NmsCoreSubnetConfiguration;

import java.io.IOException;

public class SubnetModelDisplayNameConverter extends DisplayNameConverter {

    public SubnetModelDisplayNameConverter() throws IOException {
        super(Subnet.class, NmsCoreSubnetConfiguration.getInstance());
    }

}