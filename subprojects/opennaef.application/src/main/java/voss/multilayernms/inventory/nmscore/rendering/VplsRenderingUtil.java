package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.vpls.VplsIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.VplsRenderer;

public class VplsRenderingUtil extends RenderingUtil {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(VplsRenderingUtil.class);

    public static String rendering(VplsIfDto vpls, String field) {
        return convertNull2ZeroString(renderingStaticLengthField(vpls, field));
    }

    public static String renderingStaticLengthField(VplsIfDto vpls, String field) {
        if (field.equals("VPN ID")) {
            return VplsRenderer.getVplsId(vpls);
        } else if (field.equals("VPLS ID")) {
            return VplsRenderer.getVplsId(vpls);
        } else if (field.equals("Routing Instance")) {
            return VplsRenderer.getRoutingInstanceName(vpls);
        } else if (field.equals("Route Distinguisher")) {
            return VplsRenderer.getRouteDistinguisher(vpls);
        } else if (field.equals("Node")) {
            return VplsRenderer.getNodeName(vpls);
        } else if (field.equals("Facility Status")) {
            return VplsRenderer.getFacilityStatus(vpls);
        } else if (field.equals("Company ID")) {
            return VplsRenderer.getCompanyId(vpls);
        } else if (field.equals("Note")) {
            return VplsRenderer.getNote(vpls);
        }

        return "N/A";
    }

}