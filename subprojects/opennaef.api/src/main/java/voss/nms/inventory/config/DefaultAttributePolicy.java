package voss.nms.inventory.config;

import tef.skelton.dto.EntityDto;
import voss.core.server.config.AttributePolicy;
import voss.core.server.database.ATTR;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.Map;

public class DefaultAttributePolicy implements AttributePolicy {

    @Override
    public boolean isCollectionAttribute(String attr) {
        return false;
    }

    @Override
    public boolean isMapAttribute(String attr) {
        return false;
    }

    @Override
    public boolean isExcludeAttr(String attr) {
        if (attr == null) {
            return true;
        } else if (attr.equals("node")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isPersistentAttribute(String attr) {
        return false;
    }

    @Override
    public boolean isToBeInitializeState(EntityDto o, Map<String, String> map) {
        return false;
    }

    @Override
    public boolean isToBeInitializeState(String arg0) {
        return false;
    }

    @Override
    public String getPortModeAttributeName() {
        return MPLSNMS_ATTR.ATTR_PORT_MODE;
    }

    @Override
    public boolean isPortModeSwitch(String mode) {
        if (mode == null) {
            return false;
        }
        return MPLSNMS_ATTR.ATTR_PORT_MODE_VLAN.equals(mode);
    }

    @Override
    public boolean isPortModeRouter(String mode) {
        if (mode == null) {
            return false;
        }
        return MPLSNMS_ATTR.ATTR_PORT_MODE_IP.equals(mode);
    }

    @Override
    public String getSwitchPortModeAttributeName() {
        return MPLSNMS_ATTR.ATTR_SWITCHPORT_MODE;
    }

    @Override
    public boolean isSwitchPortTrunk(String mode) {
        if (mode == null) {
            return false;
        }
        return MPLSNMS_ATTR.ATTR_SWITCHPORT_MODE_TRUNK.equals(mode);
    }

    @Override
    public boolean isSwitchPortAccess(String mode) {
        if (mode == null) {
            return false;
        }
        return MPLSNMS_ATTR.ATTR_SWITCHPORT_MODE_ACCESS.equals(mode);
    }

    @Override
    public boolean isSwitchPortDot1qTunnel(String mode) {
        if (mode == null) {
            return false;
        }
        return MPLSNMS_ATTR.ATTR_SWITCHPORT_MODE_DOT1Q_TUNNEL.equals(mode);
    }

    @Override
    public String getIfNameAttributeName() {
        return ATTR.IFNAME;
    }
}