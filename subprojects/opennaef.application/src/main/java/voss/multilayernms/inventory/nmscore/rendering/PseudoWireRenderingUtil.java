package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.PortDto;
import naef.dto.mpls.RsvpLspDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.model.FakePseudoWire;
import voss.multilayernms.inventory.renderer.NodeRenderer;
import voss.multilayernms.inventory.renderer.PortRenderer;
import voss.multilayernms.inventory.renderer.PseudoWireRenderer;

public class PseudoWireRenderingUtil extends RenderingUtil {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(PseudoWireRenderingUtil.class);


    public static String rendering(FakePseudoWire fpw, RsvpLspDto lsp, String field,
                                   PortDto acOnIngress, PortDto acOnEgress) {
        return renderingStaticLengthField(fpw, lsp, field, false, acOnIngress, acOnEgress);
    }

    public static String rendering(FakePseudoWire fpw, RsvpLspDto lsp, String field, boolean forFiltering,
                                   PortDto acOnIngress, PortDto acOnEgress) {
        return convertNull2ZeroString(renderingStaticLengthField(fpw, lsp, field, forFiltering, acOnIngress, acOnEgress));
    }

    public static String renderingStaticLengthField(FakePseudoWire pw, RsvpLspDto lsp, String field, boolean forFiltering,
                                                    PortDto acOnIngress, PortDto acOnEgress) {

        if (field.equals("PWID")) {
            return PseudoWireRenderer.getPseudoWireID(pw);
        } else if (field.equals("PW Name")) {
            return PseudoWireRenderer.getPseudoWireName(pw);
        } else if (field.equals("Route Distinguisher")) {
            return PseudoWireRenderer.getRouteDistinguisher(acOnEgress);
        } else if (field.equals("Operational Status")) {
            return PseudoWireRenderer.getOperStatus(pw);
        } else if (field.equals("Facility Status")) {
            return PseudoWireRenderer.getFacilityStatus(pw);
        } else if (field.equals("End node(A)")) {
            if (acOnIngress == null) {
                return "";
            }
            return NodeRenderer.getNodeName(acOnIngress.getNode());
        } else if (field.equals("End port(A)")) {
            if (acOnIngress == null) {
                return "";
            }
            return PortRenderer.getIfName(acOnIngress);
        } else if (field.equals("End node(B)")) {
            if (acOnEgress == null) {
                return "";
            }
            return NodeRenderer.getNodeName(acOnEgress.getNode());
        } else if (field.equals("End port(B)")) {
            if (acOnEgress == null) {
                return "";
            }
            return PortRenderer.getIfName(acOnEgress);
        } else if (field.equals("Note")) {
            return PseudoWireRenderer.getNote(pw);
        } else if (field.equals("_LastEditor")) {
            return PseudoWireRenderer.getLastEditor(pw);
        } else if (field.equals("_LastEditTime")) {
            return PseudoWireRenderer.getLastEditTime(pw);
        }
        return "N/A";
    }

}