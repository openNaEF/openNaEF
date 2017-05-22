package voss.multilayernms.inventory.nmscore.inventory.accessor;

import jp.iiga.nmt.core.model.PhysicalLink;
import naef.dto.ip.IpSubnetDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.inventory.InventoryIdUtil;
import voss.multilayernms.inventory.nmscore.model.creator.LinkModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.LinkRenderingUtil;
import voss.multilayernms.inventory.renderer.LinkRenderer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LinkHandler {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(LinkHandler.class);

    public static String parseInventoryIdOfPort1(String inventoryId) {
        return InventoryIdDecoder.getLinkPort1(inventoryId);
    }

    public static String parseInventoryIdOfPort2(String inventoryId) {
        return InventoryIdDecoder.getLinkPort2(inventoryId);
    }

    public static List<String> getPortInventoryIDOnLink(String inventoryId) throws ExternalServiceException, IOException {
        List<String> list = new ArrayList<String>();
        list.add(InventoryIdUtil.getInventoryId(LinkRenderer.getPort1(getLinkDto(inventoryId))));
        list.add(InventoryIdUtil.getInventoryId(LinkRenderer.getPort2(getLinkDto(inventoryId))));
        return list;
    }

    public static IpSubnetDto getLinkDto(String inventoryId) throws ExternalServiceException, IOException {

        for (IpSubnetDto link : MplsNmsInventoryConnector.getInstance().getActiveIpSubnets()) {
            if (InventoryIdUtil.getInventoryId(link).equals(inventoryId)) {
                return link;
            }
        }

        return null;
    }

    public static List<PhysicalLink> get(String inventoryId) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<PhysicalLink> linkList = new ArrayList<PhysicalLink>();

        for (IpSubnetDto link : MplsNmsInventoryConnector.getInstance().getActiveIpSubnets()) {
            if (InventoryIdUtil.getInventoryId(link).equals(inventoryId)) {
                linkList.add(LinkModelCreator.createModel(link, InventoryIdUtil.getInventoryId(link)));
            }
        }

        return linkList;
    }

    public static List<PhysicalLink> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<PhysicalLink> linkList = new ArrayList<PhysicalLink>();

        for (IpSubnetDto link : filterLinks(query, MplsNmsInventoryConnector.getInstance().getActiveIpSubnets())) {
            linkList.add(LinkModelCreator.createModel(link, InventoryIdUtil.getInventoryId(link)));
        }

        return linkList;
    }

    private static List<IpSubnetDto> filterLinks(ObjectFilterQuery query, Collection<IpSubnetDto> links) {
        List<IpSubnetDto> result = new ArrayList<IpSubnetDto>();

        for (IpSubnetDto link : links) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = LinkRenderingUtil.rendering(link, field, true);

                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }

            if (!unmatched) {
                result.add(link);
            }
        }
        return result;
    }

}