package voss.multilayernms.inventory.nmscore.inventory.ipsubnet.updater;

import jp.iiga.nmt.core.model.resistvlansubnet.IpSubnetNamespaceModel;
import jp.iiga.nmt.core.model.resistvlansubnet.RootIpSubnetNamespaceModel;
import naef.dto.ip.IpSubnetNamespaceDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.builder.RootIpSubnetNamespaceCommandBuilder;

import java.io.IOException;

public class RootIpSubnetNamespaceRemover extends IpSubnetNamespaceUpdater {
    RootIpSubnetNamespaceModel model;
    String userName;

    public RootIpSubnetNamespaceRemover(IpSubnetNamespaceModel target, String userName) {
        model = (RootIpSubnetNamespaceModel) target;
        this.userName = userName;
    }

    @Override
    public void commit() throws ExternalServiceException, IOException, InventoryException {
        IpSubnetNamespaceDto target = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), IpSubnetNamespaceDto.class);

        RootIpSubnetNamespaceCommandBuilder builder = new RootIpSubnetNamespaceCommandBuilder(target, userName);

        BuildResult result = builder.buildDeleteCommand();
        if (result == BuildResult.NO_CHANGES) {
            return;
        } else if (result == BuildResult.FAIL) {
            throw new IllegalStateException("command build failed.");
        }
        ShellConnector.getInstance().execute(builder);
    }

}