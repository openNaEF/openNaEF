package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.NodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.NodeRenderer;

public class NodeRenderingUtil extends RenderingUtil {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(NodeRenderingUtil.class);

    public static String rendering(NodeDto node, String field) {
        return convertNull2ZeroString(renderingStaticLengthField(node, field));
    }

    public static String renderingStaticLengthField(NodeDto node, String field) {

        if (field.equals("Node Name")) {
            return NodeRenderer.getNodeName(node);
        } else if (field.equals("Accommodation Location")) {
            return NodeRenderer.getLocationName(node);
        } else if (field.equals("POP Name")) {
            return NodeRenderer.getPopName(node);
        } else if (field.equals("Vendor Name")) {
            return NodeRenderer.getVendorName(node);
        } else if (field.equals("Node Type")) {
            return NodeRenderer.getNodeType(node);
        } else if (field.equals("OS Type")) {
            return NodeRenderer.getOsType(node);
        } else if (field.equals("OS Version")) {
            return NodeRenderer.getOsVersion(node);
        } else if (field.equals("Operational Status")) {
            return NodeRenderer.getOperStatus(node);
        } else if (field.equals("Facility Status")) {
            return NodeRenderer.getFacilityStatus(node);
        } else if (field.equals("Management IP Address")) {
            return NodeRenderer.getManagementIpAddress(node);
        } else if (field.equals("SNMP Mode")) {
            return NodeRenderer.getSnmpMode(node);
        } else if (field.equals("SNMP Community")) {
            return NodeRenderer.getSnmpCommunity(node);
        } else if (field.equals("Purpose")) {
            return NodeRenderer.getPurpose(node);
        } else if (field.equals("Note")) {
            return NodeRenderer.getNote(node);
        }
        return "N/A";
    }
}