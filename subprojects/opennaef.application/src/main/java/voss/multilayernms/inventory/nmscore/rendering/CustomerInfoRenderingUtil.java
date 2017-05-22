package voss.multilayernms.inventory.nmscore.rendering;

import naef.dto.CustomerInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.constants.CustomerConstants;
import voss.multilayernms.inventory.renderer.CustomerInfoRenderer;

public class CustomerInfoRenderingUtil extends RenderingUtil {
    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(CustomerInfoRenderingUtil.class);

    public static String rendering(CustomerInfoDto dto, String field) {
        return convertNull2ZeroString(renderingStaticLengthField(dto, field));
    }

    public static String renderingStaticLengthField(CustomerInfoDto dto, String field) {
        if (field.equals(CustomerConstants.CUSTOMER_NAME)) {
            return CustomerInfoRenderer.getCustomerName(dto);
        }
        if (field.equals(CustomerConstants.TARGET_NODE)) {
            return CustomerInfoRenderer.getNodesStringByCustomerInfoDto(dto);
        }
        if (field.equals(CustomerConstants.MAX_SSL)) {
            return "" + CustomerInfoRenderer.getMaxSsl(dto);
        }
        if (field.equals(CustomerConstants.MAX_MEMBER)) {
            return "" + CustomerInfoRenderer.getMaxMember(dto);
        }
        if (field.equals(CustomerConstants.CUSTOMER_LOGIN_ID)) {
            return CustomerInfoRenderer.getSystemUserName(dto);
        }
        if (field.equals(CustomerConstants.FW_POLICY_ZONE_MATRIX)) {
            return CustomerInfoRenderer.getFWPoliciesZoneMatrix(dto).size() > 0 ? "Set" : "No Set";
        } else if (field.equals("vlans")) {
            return CustomerInfoRenderer.getMemberVlansString(dto);
        } else if (field.equals("ip subnets")) {
            return CustomerInfoRenderer.getMemberIpSubnetsString(dto);
        }
        return "N/A";
    }
}