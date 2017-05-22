package voss.multilayernms.inventory.util;

import naef.dto.LinkDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import tef.MVO;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.util.NodeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class L2LinkUtil {

    public static List<LinkDto> getAllL2Links() throws IOException, ExternalServiceException {
        List<LinkDto> links = new ArrayList<LinkDto>();
        Set<MVO.MvoId> known = new HashSet<MVO.MvoId>();
        for (NodeDto node : MplsNmsInventoryConnector.getInstance().getActiveNodes()) {
            for (PortDto port : node.getPorts()) {
                LinkDto l2Link = NodeUtil.getLayer2Link(port);
                if (l2Link == null) {
                    continue;
                }
                if (known.contains(DtoUtil.getMvoId(l2Link))) {
                    continue;
                }
                links.add(l2Link);
                known.add(DtoUtil.getMvoId(l2Link));
            }
        }
        return links;
    }

    public static List<LinkDto> getNodeL2Links(NodeDto node) throws IOException, InventoryException {
        List<LinkDto> links = new ArrayList<LinkDto>();
        Set<MVO.MvoId> known = new HashSet<MVO.MvoId>();
        for (PortDto port : node.getPorts()) {
            LinkDto l2Link = NodeUtil.getLayer2Link(port);
            if (l2Link == null) {
                continue;
            }
            if (known.contains(DtoUtil.getMvoId(l2Link))) {
                continue;
            }
            links.add(l2Link);
            known.add(DtoUtil.getMvoId(l2Link));
        }
        return links;
    }
}