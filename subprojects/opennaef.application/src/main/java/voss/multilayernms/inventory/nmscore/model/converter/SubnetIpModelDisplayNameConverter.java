package voss.multilayernms.inventory.nmscore.model.converter;

import net.phalanx.core.models.SubnetIp;
import voss.multilayernms.inventory.config.NmsCoreSubnetIpConfiguration;

import java.io.IOException;

public class SubnetIpModelDisplayNameConverter extends DisplayNameConverter {

    public SubnetIpModelDisplayNameConverter() throws IOException {
        super(SubnetIp.class, NmsCoreSubnetIpConfiguration.getInstance());
    }

}