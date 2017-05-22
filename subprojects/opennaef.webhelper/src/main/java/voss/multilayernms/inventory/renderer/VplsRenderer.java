package voss.multilayernms.inventory.renderer;

import naef.dto.NetworkDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIfDto;
import voss.core.server.util.DtoUtil;

import java.util.HashSet;
import java.util.Set;

public class VplsRenderer extends GenericRenderer {

    public static String getVplsId(VplsIfDto vplsIf) {
        return vplsIf.getName();
    }

    public static String getRoutingInstanceName(VplsIfDto vplsIf) {
        return getVplsAttribute(vplsIf, "RoutingInstance名");
    }

    public static String getRouteDistinguisher(VplsIfDto vplsIf) {
        return getVplsAttribute(vplsIf, "RD番号");
    }

    public static String getNodeName(VplsIfDto vplsIf) {
        if (vplsIf == null) {
            return null;
        }
        return vplsIf.getNode().getName();
    }

    public static String getCompanyId(VplsIfDto vplsIf) {
        return getVplsAttribute(vplsIf, "企業ID");
    }

    private static String getVplsAttribute(VplsIfDto vplsIf, String attribute) {
        Set<NetworkDto> networks = vplsIf.getNetworks();
        Set<VplsDto> vplss = new HashSet<VplsDto>();
        for (NetworkDto network : networks) {
            if (network instanceof VplsDto) vplss.add((VplsDto) network);
        }
        if (vplss.size() == 1) {
            return DtoUtil.getStringOrNull(vplss.iterator().next(), attribute);
        }
        return null;
    }
}