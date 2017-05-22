package voss.core.server.util;

import naef.dto.PortDto;

import java.util.Comparator;

public class IfNameBasedPortComparator implements Comparator<PortDto> {
    private final IfNameComparator comparator = new IfNameComparator();

    @Override
    public int compare(PortDto port1, PortDto port2) {
        long time = System.currentTimeMillis();
        int r = compareIfName(port1, port2);
        if (r != 0) {
            PerfLog.debug(time, System.currentTimeMillis(), "- node element");
            return r;
        }
        PerfLog.debug(time, System.currentTimeMillis(), "- port");
        return port1.getName().compareTo(port2.getName());
    }

    private int compareIfName(PortDto p1, PortDto p2) {
        String ifName1 = DtoUtil.getIfName(p1);
        String ifName2 = DtoUtil.getIfName(p2);
        return this.comparator.compare(ifName1, ifName2);
    }
}