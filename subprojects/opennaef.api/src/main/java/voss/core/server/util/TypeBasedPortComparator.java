package voss.core.server.util;

import naef.dto.HardPortDto;
import naef.dto.NodeElementDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.pos.PosApsIfDto;
import voss.core.server.database.ATTR;
import voss.core.server.database.CONSTANTS;

import java.util.Collection;
import java.util.Comparator;

public class TypeBasedPortComparator implements Comparator<PortDto> {
    private static final int loopbackType = 100;
    private static final int regularType = 200;
    private static final int aggregatorType = 300;
    private static final int logicalType = 400;

    @Override
    public int compare(PortDto port1, PortDto port2) {
        long time = System.currentTimeMillis();
        int type1 = getType(port1);
        int type2 = getType(port2);
        int diff = type1 - type2;
        if (diff != 0) {
            PerfLog.debug(time, System.currentTimeMillis(), "- type diff");
            return diff;
        }
        PerfLog.debug(time, System.currentTimeMillis(), "- no type diff");
        int r = comparePort(port1, port2);
        if (r != 0) {
            PerfLog.debug(time, System.currentTimeMillis(), "- node element");
            return r;
        }
        PerfLog.debug(time, System.currentTimeMillis(), "- port");
        return port1.getName().compareTo(port2.getName());
    }

    private int getType(PortDto port) {
        NodeElementDto owner = port.getOwner();
        if (port instanceof IpIfDto) {
            String portType = DtoUtil.getStringOrNull(port, ATTR.PORT_TYPE);
            if (portType == null) {
                return logicalType;
            } else if (portType.equals(CONSTANTS.INTERFACE_TYPE_LOOPBACK)) {
                return loopbackType;
            } else if (portType.equals(CONSTANTS.INTERFACE_TYPE_INDEPENDENT_IP)) {
                Collection<PortDto> associated = ((IpIfDto) port).getAssociatedPorts();
                if (associated == null || associated.size() == 0) {
                    return loopbackType;
                } else if (associated.size() > 1) {
                    throw new IllegalStateException("too meny associated-port.");
                } else {
                    return getType(associated.iterator().next());
                }
            } else {
                return logicalType;
            }
        } else if (port instanceof HardPortDto) {
            return regularType;
        } else if (port instanceof EthLagIfDto) {
            return aggregatorType;
        } else if (port instanceof AtmApsIfDto) {
            return aggregatorType;
        } else if (port instanceof PosApsIfDto) {
            return aggregatorType;
        } else if (port instanceof FrPvcIfDto) {
            FrPvcIfDto dlc = (FrPvcIfDto) port;
            return getType((PortDto) dlc.getOwner());
        } else if (port instanceof AtmPvpIfDto) {
            AtmPvpIfDto vp = (AtmPvpIfDto) port;
            return getType((PortDto) vp.getOwner());
        } else if (port instanceof AtmPvcIfDto) {
            AtmPvcIfDto vc = (AtmPvcIfDto) port;
            return getType((PortDto) vc.getOwner());
        } else if (owner != null && owner instanceof PortDto) {
            return getType((PortDto) owner);
        } else {
            return logicalType;
        }
    }

    private int comparePort(PortDto p1, PortDto p2) {
        String ifName1 = NameUtil.getIfNameForSort(p1);
        String ifName2 = NameUtil.getIfNameForSort(p2);
        if (Util.isNull(ifName1, ifName2)) {
            throw new IllegalStateException("IfName is null: ifName1=" + ifName1 + ", ifName2=" + ifName2);
        }
        String[] ifNames1 = ifName1.split("[/.]");
        String[] ifNames2 = ifName2.split("[/.]");
        int depth = Math.min(ifNames1.length, ifNames2.length);
        for (int i = 0; i < depth; i++) {
            long _1 = Util.parseLong(ifNames1[i], 0);
            long _2 = Util.parseLong(ifNames2[i], 0);
            if (_1 != _2) {
                if (_1 > _2) {
                    return 1;
                } else if (_1 < _2) {
                    return -1;
                }
            }
            String name1 = ifNames1[i];
            String name2 = ifNames2[i];
            int r = name1.compareTo(name2);
            if (r != 0) {
                return r;
            }
        }
        return ifNames1.length - ifNames2.length;
    }
}