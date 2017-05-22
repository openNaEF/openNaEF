package voss.multilayernms.inventory.nmscore.inventory.ipsubnet.updater;

import jp.iiga.nmt.core.model.resistvlansubnet.IpSubnetNamespaceModel;
import jp.iiga.nmt.core.model.resistvlansubnet.RootIpSubnetNamespaceModel;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.nms.inventory.builder.RootIpSubnetNamespaceCommandBuilder;

import java.io.IOException;

public class RootIpSubnetNamespaceUpdater extends IpSubnetNamespaceUpdater {
    RootIpSubnetNamespaceModel model;
    String userName;

    public RootIpSubnetNamespaceUpdater(IpSubnetNamespaceModel target, String userName) {
        model = (RootIpSubnetNamespaceModel) target;
        this.userName = userName;
    }

    @Override
    public void commit() throws ExternalServiceException, IOException, InventoryException {
        String vpnPrefix = model.getVpnPrefix();
        if (!checkVpnPrefix(vpnPrefix)) {
            throw new IllegalArgumentException("VpnPrefix is duplicated.");
        }

        RootIpSubnetNamespaceCommandBuilder builder = null;
        if (model.getMvoId() == null) {
            builder = new RootIpSubnetNamespaceCommandBuilder(userName);
        } else {
        }
        if (vpnPrefix != null) {
            builder.setVpnPrefix(vpnPrefix);

        }
        builder.setStartAddress(model.getStartAddress());
        builder.setMaskLength(model.getMaskLength());
        BuildResult result = builder.buildCommand();
        if (result == BuildResult.NO_CHANGES) {
            return;
        } else if (result == BuildResult.FAIL) {
            throw new IllegalStateException("command build failed.");
        }
        ShellConnector.getInstance().execute(builder);
    }

}