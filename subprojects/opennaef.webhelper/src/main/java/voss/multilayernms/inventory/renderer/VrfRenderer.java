package voss.multilayernms.inventory.renderer;

import naef.dto.NetworkDto;
import naef.dto.vrf.VrfDto;
import naef.dto.vrf.VrfIfDto;
import voss.core.server.util.DtoUtil;

import java.util.HashSet;
import java.util.Set;

public class VrfRenderer extends GenericRenderer {

    public static String getVpnId(VrfIfDto vrfIf) {
        return vrfIf.getName();
    }

    public static String getRoutingInstanceName(VrfIfDto vrfIf) {
        return getVrfAttribute(vrfIf, "RoutingInstance名");
    }

    public static String getNodeName(VrfIfDto vrfIf) {
        return vrfIf.getNode().getName();
    }

    public static String getRouteDistinguisher(VrfIfDto vrfIf) {
        return getVrfAttribute(vrfIf, "RD番号");
    }

    public static String getCompanyId(VrfIfDto vrfIf) {
        return getVrfAttribute(vrfIf, "企業ID");
    }

    private static String getVrfAttribute(VrfIfDto vrfIf, String attribute) {
        Set<NetworkDto> networks = vrfIf.getNetworks();
        Set<VrfDto> vrfs = new HashSet<VrfDto>();
        for (NetworkDto network : networks) {
            if (network instanceof VrfDto) vrfs.add((VrfDto) network);
        }
        if (vrfs.size() == 1) {
            return DtoUtil.getStringOrNull(vrfs.iterator().next(), attribute);
        }
        return null;
    }

}
