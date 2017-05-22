package voss.multilayernms.inventory.nmscore.inventory.accessor;

import jp.iiga.nmt.core.model.PhysicalLink;
import naef.dto.LinkDto;
import naef.dto.NaefDto;
import naef.dto.PortDto;
import net.phalanx.core.expressions.ObjectFilterQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.inventory.InventoryIdDecoder;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.nmscore.model.creator.LinkModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.LinkRenderingUtil;
import voss.multilayernms.inventory.util.L2LinkUtil;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class L2LinkHandler {
    private static final Logger log = LoggerFactory.getLogger(L2LinkHandler.class);

    public static String parseInventoryIdOfPort1(String inventoryId) {
        return InventoryIdDecoder.getLinkPort1(inventoryId);
    }

    public static String parseInventoryIdOfPort2(String inventoryId) {
        return InventoryIdDecoder.getLinkPort2(inventoryId);
    }

    public static List<String> getPortInventoryIDOnLink(String inventoryId) throws InventoryException, IOException {
        List<String> list = new ArrayList<String>();
        list.add(InventoryIdDecoder.getLinkPort1(inventoryId));
        list.add(InventoryIdDecoder.getLinkPort2(inventoryId));
        return list;
    }

    public static LinkDto getLinkDto(String inventoryId) throws InventoryException, IOException {
        String port1 = InventoryIdDecoder.getLinkPort1(inventoryId);
        if (port1 == null) {
            return null;
        }
        try {
            NaefDto dto = InventoryIdDecoder.getDto(port1);
            if (dto == null) {
                return null;
            } else if (!(dto instanceof PortDto)) {
                return null;
            }
            PortDto port = (PortDto) dto;
            return NodeUtil.getLayer2Link(port);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static List<PhysicalLink> get(String inventoryId) throws NotBoundException, AuthenticationException, IOException, InventoryException {
        List<PhysicalLink> linkList = new ArrayList<PhysicalLink>();
        LinkDto link = getLinkDto(inventoryId);
        if (link != null) {
            linkList.add(LinkModelCreator.createModel(link, InventoryIdCalculator.getId(link)));
        }
        return linkList;
    }

    public static List<PhysicalLink> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException, IOException, ExternalServiceException {
        List<PhysicalLink> linkList = new ArrayList<PhysicalLink>();
        List<LinkDto> links = L2LinkUtil.getAllL2Links();
        log.debug("l2links: " + links.size());
        for (LinkDto link : filterLinks(query, links)) {
            linkList.add(LinkModelCreator.createModel(link, InventoryIdCalculator.getId(link)));
        }
        return linkList;
    }

    private static List<LinkDto> filterLinks(ObjectFilterQuery query, Collection<LinkDto> links) {
        List<LinkDto> result = new ArrayList<LinkDto>();

        for (LinkDto link : links) {
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