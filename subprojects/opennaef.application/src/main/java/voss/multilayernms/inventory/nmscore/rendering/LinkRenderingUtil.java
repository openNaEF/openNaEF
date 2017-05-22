package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.LinkDto;
import naef.dto.ip.IpSubnetDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.L2LinkRenderer;
import voss.multilayernms.inventory.renderer.LinkRenderer;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;

public class LinkRenderingUtil extends RenderingUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(LinkRenderingUtil.class);

    public static String rendering(IpSubnetDto link, String field) {
        return rendering(link, field, false);
    }

    public static String rendering(IpSubnetDto link, String field, boolean forFiltering) {
        return convertNull2ZeroString(renderingStaticLengthField(link, field, forFiltering));
    }

    public static String renderingStaticLengthField(IpSubnetDto link, String field, boolean forFiltering) {
        if (field.equals("Cable Name")) {
            return LinkRenderer.getCableName(link);
        } else if (field.equals("Endpoint node 1")) {
            return NodeRenderer.getNodeName(LinkRenderer.getPort1(link).getNode());
        } else if (field.equals("Endpoint port 1")) {
            return PortRenderer.getIfName(LinkRenderer.getPort1(link));
        } else if (field.equals("Endpoint node 2")) {
            return NodeRenderer.getNodeName(LinkRenderer.getPort2(link).getNode());
        } else if (field.equals("Endpoint port 2")) {
            return PortRenderer.getIfName(LinkRenderer.getPort2(link));
        }
        return "N/A";
    }

    public static String rendering(LinkDto link, String field) {
        return rendering(link, field, false);
    }

    public static String rendering(LinkDto link, String field, boolean forFiltering) {
        return convertNull2ZeroString(renderingStaticLengthField(link, field, forFiltering));
    }

    public static String renderingStaticLengthField(LinkDto link, String field, boolean forFiltering) {
        if (field.equals("Cable Name")) {
            return L2LinkRenderer.getName(link);
        } else if (field.equals("Endpoint node 1")) {
            return NodeRenderer.getNodeName(L2LinkRenderer.getPort1(link).getNode());
        } else if (field.equals("Endpoint port 1")) {
            return PortRenderer.getIfName(L2LinkRenderer.getPort1(link));
        } else if (field.equals("Endpoint LAG 1")) {
            return PortRenderer.getAggregationPortName(L2LinkRenderer.getPort1(link));
        } else if (field.equals("Endpoint node 2")) {
            return NodeRenderer.getNodeName(L2LinkRenderer.getPort2(link).getNode());
        } else if (field.equals("Endpoint port 2")) {
            return PortRenderer.getIfName(L2LinkRenderer.getPort2(link));
        } else if (field.equals("Endpoint LAG 2")) {
            return PortRenderer.getAggregationPortName(L2LinkRenderer.getPort2(link));
        }
        return "N/A";
    }
}