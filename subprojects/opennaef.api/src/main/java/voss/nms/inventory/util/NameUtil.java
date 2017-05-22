package voss.nms.inventory.util;

import naef.dto.PortDto;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.Util;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.database.MPLSNMS_ATTR;

public class NameUtil extends voss.core.server.util.NameUtil {

    public static String getPortTypeName(PortDto port) {
        String portTypeName = DtoUtil.getStringOrNull(port, MPLSNMS_ATTR.PORT_TYPE);
        if (Util.stringToNull(portTypeName) != null) {
            return portTypeName;
        }
        PortType type = PortType.getByType(port.getObjectTypeName());
        if (type != null) {
            return type.getCaption();
        }
        return "-";
    }
}