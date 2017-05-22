package voss.nms.inventory.util;

import naef.dto.*;
import naef.dto.atm.AtmPortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.isdn.IsdnPortDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoComparator;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.LinkCoreUtil;
import voss.core.server.util.NodeUtil;

import java.util.*;

public class LinkUtil extends LinkCoreUtil {
    private static final Logger log = LoggerFactory.getLogger(LinkUtil.class);

    private LinkUtil() {
    }

    public static boolean isL2LinkCapablePort(PortDto port) {
        if (port == null) {
            return false;
        }
        if (port instanceof HardPortDto) {
            String layerName = getL2LinkTypeName(port);
            return layerName != null;
        }
        return false;
    }

    public static boolean isL1LinkCapablePort(PortDto port) {
        if (port == null) {
            return false;
        }
        if (port instanceof HardPortDto) {
            return true;
        }
        return false;
    }

    public static String getL2LinkTypeName(PortDto port) {
        if (port == null) {
            return null;
        }
        if (port instanceof EthPortDto) {
            return ATTR.TYPE_ETH_LINK;
        } else if (port instanceof SerialPortDto) {
            return ATTR.TYPE_SERIAL_LINK;
        } else if (port instanceof AtmPortDto) {
            return ATTR.TYPE_ATM_LINK;
        } else if (port instanceof PosPortDto) {
            return ATTR.TYPE_POS_LINK;
        } else if (port instanceof IsdnPortDto) {
            return ATTR.TYPE_ISDN_LINK;
        } else if (port instanceof EthLagIfDto) {
            return ATTR.TYPE_LAG_LINK;
        }
        return null;
    }

    public static String getL2Usage(PortDto port) {
        LinkDto link = NodeUtil.getLayer2Link(port);
        if (link == null) {
            log.trace("link is null.");
            return null;
        }
        if (link.getUpperLayers() == null) {
            log.trace("no upper-layers");
            return null;
        }
        List<NaefDto> user = new ArrayList<NaefDto>();
        getUser(link, user);
        DtoUtil.removeDuplication(user);
        Collections.sort(user, new DtoComparator());
        StringBuilder sb = new StringBuilder();
        for (NaefDto dto : user) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            if (dto instanceof NodeElementDto) {
                sb.append(NameUtil.getIfName((NodeElementDto) dto));
            } else if (dto instanceof NetworkDto) {
                sb.append(NameUtil.getResourceId((NetworkDto) dto));
            } else {
                sb.append(dto.getAbsoluteName());
            }
        }
        return sb.toString();
    }

    private static void getUser(NetworkDto link, List<NaefDto> users) {
        log.trace("getUser(): link=" + link.getAbsoluteName());
        NaefDto owner = link.getOwner();
        if (owner != null) {
            log.trace("owner found: " + owner.getAbsoluteName());
            users.add(owner);
        }
        Set<NetworkDto> uppers = link.getUpperLayers();
        if (uppers == null) {
            return;
        }
        for (NetworkDto upper : uppers) {
            if (upper == null) {
                continue;
            }
            getUser(upper, users);
        }
    }

    public static boolean isUsedDemarcationLink(PortDto port) {
        if (port == null) {
            return false;
        }
        LinkDto link = NodeUtil.getLayer2Link(port);
        if (link == null) {
            return false;
        }
        if (!isEthDemarcationLink(link)) {
            return false;
        }
        return link.getUpperLayers().size() > 0;
    }

    public static boolean isUsedLink(PortDto port) {
        if (port == null) {
            return false;
        }
        LinkDto link = NodeUtil.getLayer2Link(port);
        if (link == null) {
            return false;
        }
        if (isEthDemarcationLink(link)) {
            return false;
        }
        return link.getUpperLayers().size() > 0;
    }

    public static boolean hasEthDemarcationLink(PortDto port) {
        if (port == null) {
            return false;
        }
        if (port instanceof EthPortDto) {
            return hasDemarcationLink((EthPortDto) port);
        } else if (port instanceof EthLagIfDto) {
            return hasDemarcationLink((EthLagIfDto) port);
        }
        return false;
    }

    public static boolean hasDemarcationLink(EthPortDto eth) {
        if (eth == null) {
            return false;
        }
        LinkDto link = NodeUtil.getLayer2Link(eth);
        return isEthDemarcationLink(link);
    }

    public static boolean hasDemarcationLink(EthLagIfDto lag) {
        if (lag == null) {
            return false;
        }
        Set<Boolean> result = new HashSet<Boolean>();
        for (EthPortDto eth : lag.getBundlePorts()) {
            result.add(hasDemarcationLink(eth));
        }
        if (result.size() == 1) {
            Boolean value = result.iterator().next();
            return value.booleanValue();
        }
        return false;
    }

    public static boolean isEthDemarcationLink(LinkDto link) {
        if (link == null) {
            return false;
        }
        if (!link.isDemarcationLink()) {
            return false;
        }
        return link.getShellClassId().equals("eth-link");
    }

    public static boolean isVlanDemarcationLink(LinkDto link) {
        if (link == null) {
            return false;
        }
        if (!link.isDemarcationLink()) {
            return false;
        }
        return link.getShellClassId().equals("vlan-link");
    }
}