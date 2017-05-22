package voss.nms.inventory.builder;

import voss.nms.inventory.constants.PortType;

public class CoreAppBuilderUtil {

    public static boolean isEthernetType(PortType type) {
        if (type == null) {
            return false;
        }
        if (type == PortType.ETHERNET) {
            return true;
        } else if (type == PortType.EPS) {
            return true;
        } else if (type == PortType.LAG) {
            return true;
        } else if (type == PortType.VM_ETHERNET) {
            return true;
        }
        return false;
    }
}