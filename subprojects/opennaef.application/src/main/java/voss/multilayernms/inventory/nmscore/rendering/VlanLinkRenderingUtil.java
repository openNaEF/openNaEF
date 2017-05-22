package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.vlan.VlanLinkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.VlanLinkRenderer;

public class VlanLinkRenderingUtil extends RenderingUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(VlanLinkRenderingUtil.class);

    public static String rendering(VlanLinkDto link, String field) {
        return rendering(link, field, false);
    }

    public static String rendering(VlanLinkDto link, String field, boolean forFiltering) {
        return convertNull2ZeroString(renderingStaticLengthField(link, field, forFiltering));
    }

    public static String renderingStaticLengthField(VlanLinkDto link, String field, boolean forFiltering) {
        if (field.equals("VLAN link name")) {
            return VlanLinkRenderer.getName(link);
        } else if (field.equals("Endpoint node 1")) {
            return NodeRenderer.getNodeName(VlanLinkRenderer.getPort1(link).getNode());
        } else if (field.equals("Endpoint physical port 1")) {
            return PortRenderer.getIfName(VlanLinkRenderer.getPhysicalPort1(link));
        } else if (field.equals("Endpoint VLAN port 1")) {
            return PortRenderer.getIfName(VlanLinkRenderer.getPort1(link));
        } else if (field.equals("Endpoint node 2")) {
            return NodeRenderer.getNodeName(VlanLinkRenderer.getPort2(link).getNode());
        } else if (field.equals("Endpoint physical port 2")) {
            return PortRenderer.getIfName(VlanLinkRenderer.getPhysicalPort2(link));
        } else if (field.equals("Endpoint VLAN port 2")) {
            return PortRenderer.getIfName(VlanLinkRenderer.getPort2(link));
        } else if (field.equals("LastEditTime")) {
            return VlanLinkRenderer.getLastEditTime(link);
        }
        return "N/A";
    }
}