package voss.multilayernms.inventory.util;

import naef.dto.PortDto;
import voss.nms.inventory.util.NameUtil;

import java.util.Comparator;

public class PortIfNameComparator implements Comparator<PortDto> {

    @Override
    public int compare(PortDto o1, PortDto o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return 1;
        } else if (o2 == null) {
            return -1;
        }
        String ifName1 = NameUtil.getNodeIfName(o1);
        String ifName2 = NameUtil.getNodeIfName(o2);
        return ifName1.compareTo(ifName2);
    }

}