package voss.multilayernms.inventory.nmscore.model.converter;

import net.phalanx.core.models.Vlan;
import voss.multilayernms.inventory.config.NmsCoreVlanConfiguration;

import java.io.IOException;

public class VlanModelDisplayNameConverter extends DisplayNameConverter {

    public VlanModelDisplayNameConverter() throws IOException {
        super(Vlan.class, NmsCoreVlanConfiguration.getInstance());
    }

}