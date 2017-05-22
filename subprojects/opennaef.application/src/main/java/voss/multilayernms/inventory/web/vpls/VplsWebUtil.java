package voss.multilayernms.inventory.web.vpls;

import naef.dto.vpls.VplsDto;
import naef.dto.vpls.VplsIntegerIdPoolDto;
import org.apache.wicket.PageParameters;
import voss.core.server.util.Util;
import voss.nms.inventory.util.VplsUtil;

public class VplsWebUtil extends VplsUtil {

    public static PageParameters getParameters(VplsIntegerIdPoolDto pool) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, Util.encodeUTF8(pool.getName()));
        return param;
    }

    public static PageParameters getVplsParam(VplsIntegerIdPoolDto pool, VplsDto vpls) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, Util.encodeUTF8(pool.getName()));
        param.add(KEY_VPLS_ID, vpls.getStringId().toString());
        return param;
    }

    public static PageParameters getVplsParam(VplsIntegerIdPoolDto pool, Integer id) {
        PageParameters param = new PageParameters();
        param.add(KEY_POOL_ID, Util.encodeUTF8(pool.getName()));
        param.add(KEY_VPLS_ID, id.toString());
        return param;
    }
}