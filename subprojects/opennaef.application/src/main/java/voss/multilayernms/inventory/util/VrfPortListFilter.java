package voss.multilayernms.inventory.util;

import naef.dto.HardPortDto;
import naef.dto.PortDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.vlan.VlanIfDto;
import naef.dto.vrf.VrfIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.PortFilter;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.VlanUtil;


public class VrfPortListFilter implements PortFilter {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(VrfPortListFilter.class);

    public boolean match(PortDto port) {
        if (port instanceof VrfIfDto) {
            return false;
        } else if (port instanceof VrfIfDto) {
            return false;
        } else if (port instanceof VlanIfDto) {
            if (NodeUtil.isSubInterface(port)) {
                return true;
            }
            return false;
        } else if (port instanceof AtmPvcIfDto) {
            return true;
        } else if (port instanceof FrPvcIfDto) {
            return true;
        } else if (port instanceof EthLagIfDto) {
            return true;
        } else if (port instanceof IpIfDto) {
            return true;
        } else if (port instanceof HardPortDto) {
            if (VlanUtil.isBridgePort(port)) {
                return false;
            }
            return true;
        }
        log.warn("unknown type: " + port.getAbsoluteName());
        return false;
    }
}