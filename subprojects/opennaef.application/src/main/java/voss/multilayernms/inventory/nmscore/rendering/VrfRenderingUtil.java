package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.VrfRenderer;

public class VrfRenderingUtil extends RenderingUtil {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(VrfRenderingUtil.class);

    public static String rendering(VrfIfDto vrf, String field) {
        return convertNull2ZeroString(renderingStaticLengthField(vrf, field));
    }

    public static String renderingStaticLengthField(VrfIfDto vrf, String field) {
        if (field.equals("VPN ID")) {
            return VrfRenderer.getVpnId(vrf);
        } else if (field.equals("Routing Instance")) {
            return VrfRenderer.getRoutingInstanceName(vrf);
        } else if (field.equals("Route Distinguisher")) {
            return VrfRenderer.getRouteDistinguisher(vrf);
        } else if (field.equals("Node")) {
            return VrfRenderer.getNodeName(vrf);
        } else if (field.equals("Facility Status")) {
            return VrfRenderer.getFacilityStatus(vrf);
        } else if (field.equals("Company ID")) {
            return VrfRenderer.getCompanyId(vrf);
        } else if (field.equals("Note")) {
            return VrfRenderer.getNote(vrf);
        }

        return "N/A";
    }

}