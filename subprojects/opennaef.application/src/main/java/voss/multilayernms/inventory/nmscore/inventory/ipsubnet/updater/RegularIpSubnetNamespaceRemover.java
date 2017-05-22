package voss.multilayernms.inventory.nmscore.inventory.ipsubnet.updater;

import jp.iiga.nmt.core.model.resistvlansubnet.IpSubnetNamespaceModel;
import jp.iiga.nmt.core.model.resistvlansubnet.RegularIpSubnetNamespaceModel;
import naef.dto.ip.IpSubnetNamespaceDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.builder.RegularIpSubnetNamespaceCommandBuilder;

import java.io.IOException;

public class RegularIpSubnetNamespaceRemover extends IpSubnetNamespaceUpdater {
    RegularIpSubnetNamespaceModel model;
    String userName;

    public RegularIpSubnetNamespaceRemover(IpSubnetNamespaceModel target, String userName) {
        model = (RegularIpSubnetNamespaceModel) target;
        this.userName = userName;
    }

    @Override
    public void commit() throws ExternalServiceException, IOException, InventoryException {
        IpSubnetNamespaceDto target = MplsNmsInventoryConnector.getInstance().getMvoDto(model.getMvoId(), IpSubnetNamespaceDto.class);

        RegularIpSubnetNamespaceCommandBuilder builder = new RegularIpSubnetNamespaceCommandBuilder(target, userName);

        BuildResult result = builder.buildDeleteCommand();
        if (result == BuildResult.NO_CHANGES) {
            return;
        } else if (result == BuildResult.FAIL) {
            throw new IllegalStateException("command build failed.");
        }
        ShellConnector.getInstance().execute(builder);
    }
}