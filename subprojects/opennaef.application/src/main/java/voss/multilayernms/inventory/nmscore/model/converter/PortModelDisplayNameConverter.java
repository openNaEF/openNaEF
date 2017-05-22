package voss.multilayernms.inventory.nmscore.model.converter;

import jp.iiga.nmt.core.model.PhysicalEthernetPort;
import voss.multilayernms.inventory.config.NmsCorePortConfiguration;

import java.io.IOException;

public class PortModelDisplayNameConverter extends DisplayNameConverter {

    public PortModelDisplayNameConverter() throws IOException {
        super(PhysicalEthernetPort.class,
                NmsCorePortConfiguration.getInstance());
    }

}