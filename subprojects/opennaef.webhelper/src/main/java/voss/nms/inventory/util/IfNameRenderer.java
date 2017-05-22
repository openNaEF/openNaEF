package voss.nms.inventory.util;

import naef.dto.PortDto;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import voss.core.server.util.DtoUtil;
import voss.nms.inventory.database.MPLSNMS_ATTR;

public class IfNameRenderer extends ChoiceRenderer<PortDto> {
    private static final long serialVersionUID = 1L;

    @Override
    public Object getDisplayValue(PortDto port) {
        if (port == null) {
            return null;
        }
        String ifName = DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.IFNAME);
        if (ifName != null) {
            return ifName;
        }
        return NameUtil.getIfName(port);
    }
}