package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.vlan.VlanDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.renderer.VlanRenderer;

public class VlanRenderingUtil extends RenderingUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(VlanRenderingUtil.class);

    public static String rendering(VlanDto vlan, String field) {
        return convertNull2ZeroString(renderingStaticLengthField(vlan, field));
    }

    public static String renderingStaticLengthField(VlanDto vlan, String field) {

        if (field.equals("VLAN ID")) {
            return VlanRenderer.getVlanId(vlan);
        } else if (field.equals("SubnetAddress")) {
            return VlanRenderer.getSubnetAddress(vlan);
        } else if (field.equals("Subnet")) {
            return VlanRenderer.getSubnetName(vlan);
        } else if (field.equals("User")) {
            return VlanRenderer.getUser(vlan);
        } else if (field.equals("Notice")) {
            return VlanRenderer.getNotice(vlan);
        } else if (field.equals("Purpose")) {
            return VlanRenderer.getPurpose(vlan);
        } else if (field.equals("AreaCode")) {
            return VlanRenderer.getAreaCode(vlan);
        } else if (field.equals("UserCode")) {
            return VlanRenderer.getUserCode(vlan);
        } else if (field.equals("VLAN Pool")) {
            return VlanRenderer.getVlanIdPoolName(vlan);
        }

        return "N/A";
    }
}