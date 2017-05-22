package voss.nms.inventory.util;

import naef.dto.InterconnectionIfDto;
import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.ui.NaefDtoFacade.SearchMethod;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NodePipeUtil {

    public static List<InterconnectionIfDto> getNodePipes(NodeDto node) {
        try {
            Set<InterconnectionIfDto> pipes = DtoUtil.getNaefDtoFacade(node).selectNodeElements(
                    node, InterconnectionIfDto.class, SearchMethod.REGEXP, MPLSNMS_ATTR.IFNAME, ".*");
            return new ArrayList<InterconnectionIfDto>(pipes);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static List<InterconnectionIfDto> getNodePipesOn(PortDto port) {
        if (port == null) {
            return null;
        }
        List<InterconnectionIfDto> result = new ArrayList<InterconnectionIfDto>();
        List<InterconnectionIfDto> pipes = getNodePipes(port.getNode());
        for (InterconnectionIfDto pipe : pipes) {
            for (PortDto ac : pipe.getAttachedPorts()) {
                if (DtoUtil.isSameMvoEntity(port, ac)) {
                    result.add(pipe);
                    break;
                }
            }
        }
        return result;
    }
}