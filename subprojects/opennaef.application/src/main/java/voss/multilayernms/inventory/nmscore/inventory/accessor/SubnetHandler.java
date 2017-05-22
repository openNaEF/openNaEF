package voss.multilayernms.inventory.nmscore.inventory.accessor;

import naef.dto.CustomerInfoDto;
import naef.dto.NetworkDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.ip.IpSubnetNamespaceDto;
import naef.dto.vlan.VlanDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import tef.skelton.dto.EntityDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.SubnetModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.SubnetRenderingUtil;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;
import voss.multilayernms.inventory.renderer.SubnetRenderer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class SubnetHandler {

    private static final Logger log = LoggerFactory.getLogger(SubnetHandler.class);

    public static List<Subnet> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException, IOException, InventoryException, ExternalServiceException {
        List<Subnet> subnetList = new ArrayList<Subnet>();

        for (IpSubnetNamespaceDto children : SubnetRenderer.getAllIpSubnetNamespace()) {
            log.debug("SubnetHandler " + children.getName());
            for (IpSubnetDto ipSubnet : filter(query, children.getUsers())) {
                subnetList.add(SubnetModelCreator.createModel(ipSubnet, InventoryIdUtil.getInventoryId(ipSubnet)));
            }
        }
        return subnetList;
    }

    private static List<IpSubnetDto> filter(ObjectFilterQuery query, Collection<IpSubnetDto> ipSubnets) {
        List<IpSubnetDto> result = new ArrayList<IpSubnetDto>();

        for (IpSubnetDto ipSubnet : ipSubnets) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = SubnetRenderingUtil.rendering(ipSubnet, field);

                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }

            if (!unmatched) {
                result.add(ipSubnet);
            }
        }
        return result;
    }

    public static List<Subnet> getList(String mvoId) throws IOException, InventoryException, ExternalServiceException {
        EntityDto dto = MplsNmsInventoryConnector.getInstance().getMvoDtoByMvoId(mvoId);

        if (dto instanceof VlanDto) {
            return getList((VlanDto) dto);
        } else if (dto instanceof CustomerInfoDto) {
            return getList((CustomerInfoDto) dto);
        } else {
            return new ArrayList<Subnet>();
        }
    }

    private static List<Subnet> getList(VlanDto dto) throws IOException {
        List<Subnet> subnetList = new ArrayList<Subnet>();
        for (NetworkDto network : dto.getUpperLayers()) {
            if (network instanceof IpSubnetDto) {
                subnetList.add(SubnetModelCreator.createModel((IpSubnetDto) network, InventoryIdUtil.getInventoryId((IpSubnetDto) network)));
            }
        }
        return subnetList;
    }

    private static List<Subnet> getList(CustomerInfoDto dto) throws IOException {
        List<Subnet> subnetList = new ArrayList<Subnet>();
        for (IpSubnetDto ipSubnet : CustomerInfoRenderer.getMemberIpSubnets(dto)) {
            subnetList.add(SubnetModelCreator.createModel(ipSubnet, InventoryIdUtil.getInventoryId((ipSubnet))));
        }
        return subnetList;
    }
}