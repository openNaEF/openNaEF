package voss.multilayernms.inventory.nmscore.model.converter;

import jp.iiga.nmt.core.model.Device;
import voss.multilayernms.inventory.config.NmsCoreNodeConfiguration;

import java.io.IOException;

public class NodeModelDisplayNameConverter extends DisplayNameConverter {

    public NodeModelDisplayNameConverter() throws IOException {
        super(Device.class,
                NmsCoreNodeConfiguration.getInstance());
    }

}