package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.mpls.RsvpLspDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.HopUnit;
import voss.multilayernms.inventory.renderer.RsvpLspRenderer;

import java.util.List;

public class RsvpLspRenderingUtil extends RenderingUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(RsvpLspRenderingUtil.class);

    public static String rendering(RsvpLspDto lsp, String field) {
        if (field.equals("Primary Hops IP Address")
                || field.equals("Secondary Hops IP Address")
                ) {
            return renderingDynamicLengthField(lsp, field);
        } else {
            return convertNull2ZeroString(renderingStaticLengthField(lsp, field));
        }
    }

    public static String renderingStaticLengthField(RsvpLspDto lsp, String field) {
        if (field.equals("LSP Name")) {
            return RsvpLspRenderer.getLspName(lsp);
        } else if (field.equals("IngressNode")) {
            return RsvpLspRenderer.getIngressNodeName(lsp);
        } else if (field.equals("EgressNode")) {
            return RsvpLspRenderer.getEgressNodeName(lsp);
        } else if (field.equals("Operational Status")) {
            return RsvpLspRenderer.getOperStatus(lsp);
        } else if (field.equals("Facility Status")) {
            return RsvpLspRenderer.getFacilityStatus(lsp);
        } else if (field.equals("Primary Path Name")) {
            return RsvpLspRenderer.getMainPathName(lsp);
        } else if (field.equals("Secondary Path Name")) {
            return RsvpLspRenderer.getBackupPathName(lsp);
        } else if (field.equals("Active Path Name")) {
            return RsvpLspRenderer.getActivePathName(lsp);
        }

        return "N/A";
    }


    public static String renderingDynamicLengthField(RsvpLspDto lsp, String field) {
        if (field.equals("Primary Hops IP Address")) {
            return getHopIpAddressString("Primary Path", RsvpLspRenderer.getPrimaryHops(lsp));
        } else if (field.equals("Secondary Hops IP Address")) {
            return getHopIpAddressString("Secondary Path", RsvpLspRenderer.getSecondaryHops(lsp));
        }

        return "N/A";
    }

    private static String getHopIpAddressString(String prefix, List<HopUnit> hopUnits) {
        StringBuilder value = new StringBuilder();
        value.append("HopSize=").append(hopUnits.size());
        value.append("\n");

        String headerWithoutCount = prefix + ":" + " Hop ";
        value.append("HeaderWithoutCount=").append(headerWithoutCount);

        if (hopUnits.size() == 0) {
            value.append("\n");
            value.append(prefix + "|N/A");
        } else {
            int count = 0;
            for (HopUnit hopUnit : hopUnits) {
                count++;
                value.append("\n");
                value.append(headerWithoutCount + count);
                value.append("|");
                value.append(hopUnit.getHopSourceIP());
                value.append("(").append(hopUnit.getHopSourceName());
                value.append(") -> ");
                value.append(hopUnit.getHopDestIP());
                value.append("(").append(hopUnit.getHopDestName()).append(")");
            }
        }
        return value.toString();
    }

}