package voss.multilayernms.inventory.nmscore.view.portedit;

import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import naef.dto.ip.IpSubnetNamespaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.multilayernms.inventory.renderer.SubnetRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetIpSubnetNamespaces {
    Logger log = LoggerFactory.getLogger(GetIpSubnetNamespaces.class);
    VlanEditModel model;

    public GetIpSubnetNamespaces(VlanEditModel model) {
        this.model = model;
    }

    public VlanEditModel getIpSubnetNamespaces() throws ExternalServiceException {

        List<IpSubnetNamespaceDto> ipSubnetNamespaces = SubnetRenderer.getAllIpSubnetNamespace();
        List<String> ip_subnet_namespaces = new ArrayList<String>();
        for (IpSubnetNamespaceDto ipSubnetNamespace : ipSubnetNamespaces) {
            ip_subnet_namespaces.add(ipSubnetNamespace.getName());
        }
        Collections.sort(ip_subnet_namespaces);
        model.setIpSubnetNamespaces(ip_subnet_namespaces);

        return model;
    }

}