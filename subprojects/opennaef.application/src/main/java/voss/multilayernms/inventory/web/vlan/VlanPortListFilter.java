package voss.multilayernms.inventory.web.vlan;

import naef.dto.PortDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import voss.core.server.util.PortFilter;


public class VlanPortListFilter implements PortFilter {
    private static final long serialVersionUID = 1L;

    public boolean match(PortDto port) {
        if (port == null) {
            return false;
        } else if (port instanceof EthPortDto) {
            return true;
        } else if (port instanceof EthLagIfDto) {
            return true;
        } else {
            return false;
        }
    }
}