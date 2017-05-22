package voss.multilayernms.inventory.nmscore.inventory.ipsubnet.updater;

import naef.dto.ip.IpSubnetNamespaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.renderer.IpSubnetAddressRenderer;
import voss.multilayernms.inventory.renderer.SubnetRenderer;

import java.io.IOException;

public abstract class IpSubnetNamespaceUpdater {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(IpSubnetNamespaceUpdater.class);

    abstract public void commit() throws ExternalServiceException, IOException, InventoryException;

    protected boolean checkVpnPrefix(String target) throws ExternalServiceException {
        if (target == null) return true;
        for (IpSubnetNamespaceDto namespace : SubnetRenderer.getAllIpSubnetNamespace()) {
            String vpnPrefix = new IpSubnetAddressRenderer(namespace).getVpnPrefix();
            if (target.equals(vpnPrefix)) {
                return false;
            }
        }
        return true;
    }


}