package voss.nms.inventory.model;

import naef.dto.*;
import naef.dto.atm.AtmApsIfDto;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.atm.AtmPvpIfDto;
import naef.dto.eth.EthLagIfDto;
import naef.dto.eth.EthPortDto;
import naef.dto.fr.FrPvcIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.serial.TdmSerialIfDto;
import naef.dto.vlan.VlanIfDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.NodeUtil;
import voss.core.server.util.PortFilter;
import voss.nms.inventory.database.MPLSNMS_ATTR;

public class NodeDetailPortFilter implements PortFilter {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean match(PortDto port) {
        Logger log = LoggerFactory.getLogger(PortsModel.class);
        if (port == null) {
            return false;
        } else if (port.getIfname() == null) {
                return false;
        } else if (port.isAlias()) {
            return true;
        }
        if (port instanceof EthPortDto) {
            if (port.getOwner() instanceof HardwareDto) {
                return true;
            } else if (port.getOwner() instanceof NodeDto) {
                return true;
            } else {
                return false;
            }
        } else if (port instanceof HardPortDto) {
            return true;
        } else if (port instanceof AtmApsIfDto) {
            return true;
        } else if (port instanceof PosApsIfDto) {
            return true;
        } else if (port instanceof EthLagIfDto) {
            return true;
        } else if (port instanceof TdmSerialIfDto) {
            return true;
        } else if (port instanceof IpIfDto) {
            IpIfDto ip = (IpIfDto) port;
            if (NodeUtil.isLoopback(ip)) {
                return true;
            } else if (DtoUtil.getStringOrNull(ip, ATTR.PORT_TYPE) != null) {
                return true;
            }
        } else if (port instanceof AtmPvcIfDto) {
            return true;
        } else if (port instanceof AtmPvpIfDto) {
            return !NodeUtil.isImplicit(port);
        } else if (port instanceof FrPvcIfDto) {
            return true;
        } else if (port instanceof VlanIfDto) {
            NodeElementDto owner = port.getOwner();
            if (owner != null && owner instanceof PortDto) {
                return true;
            }
            return DtoUtil.getBoolean(port, MPLSNMS_ATTR.SVI_ENABLED);
        }
        log.debug("filtered:(" + port.getClass().getSimpleName() + ")" + port.getAbsoluteName());
        return false;
    }

}