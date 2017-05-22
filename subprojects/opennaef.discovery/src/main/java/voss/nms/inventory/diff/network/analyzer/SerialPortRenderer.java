package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.ConfigModelUtil;
import voss.model.SerialPortImpl;
import voss.model.value.PortSpeedValue;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SerialPortRenderer extends AbstractHardPortRenderer<SerialPortImpl> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public SerialPortRenderer(SerialPortImpl port, String parentID, int depth, Map<String, String> map) {
        super(port, parentID, depth, map);
        if (parentID == null) {
            throw new IllegalArgumentException("parentID is null.");
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        return AbsoluteNameFactory.getSerialPortAbsoluteName(getParentAbsoluteName(), getModel());
    }

    @Override
    protected String setInventoryID(String parentID) {
        return InventoryIdCalculator.getId(getModel());
    }

    public boolean isRoot() {
        return false;
    }

    public List<String> getAttributeNames() {
        List<String> result = new ArrayList<String>();
        for (Attr attr : Attr.values()) {
            result.add(attr.attributeName);
        }
        return result;
    }

    public String getValue(String naefAttributeName) {
        Attr attr = Attr.getByAttributeName(naefAttributeName);
        String value = getValue(attr);
        log.trace("result=" + attr.getAttributeName() + ":" + value);
        return value;
    }

    public String getValue(Enum<?> attr_) {
        Attr attr = (Attr) attr_;
        SerialPortImpl port = getModel();
        switch (attr) {
            case PORT_ID:
                return String.valueOf(port.getPortIndex());
            case IFNAME:
                return ConfigModelUtil.getIfName(port);
            case CONFIGNAME:
                return port.getConfigName();
            case IPIFNAME:
                return ConfigModelUtil.getIpIfName(port);
            case PORT_TYPE:
                return port.getPortTypeName();
            case MEDIA_TYPE:
                return port.getConnectorTypeName();
            case PORT_SPEED:
                PortSpeedValue.Oper speed = port.getPortOperationalSpeed();
                if (speed != null) {
                    Long speedValue = speed.getValue();
                    if (speedValue != null) {
                        return speedValue.toString();
                    }
                }
                return null;
            case IP_ADDRESS:
                return ConfigModelUtil.getIpAddress(port, true);
            case SUBNETMASK:
                return ConfigModelUtil.getSubnetMask(port, true);
            case DESCRIPTION:
                return port.getUserDescription();
            case OSPF_AREA_ID:
                return port.getOspfAreaID();
            case IGP_COST:
                return String.valueOf(port.getIgpCost());
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    public static enum Attr {
        IFNAME(MPLSNMS_ATTR.IFNAME),
        CONFIGNAME(ATTR.CONFIG_NAME),
        IPIFNAME(MPLSNMS_ATTR.IFNAME),
        PORT_TYPE(MPLSNMS_ATTR.PORT_TYPE),
        MEDIA_TYPE("media-type"),
        IP_ADDRESS(MPLSNMS_ATTR.IP_ADDRESS),
        SUBNETMASK(MPLSNMS_ATTR.MASK_LENGTH),
        DESCRIPTION(MPLSNMS_ATTR.DESCRIPTION),
        PORT_SPEED(MPLSNMS_ATTR.PORTSPEED_ADMIN),
        PORT_ID(MPLSNMS_ATTR.NAME),
        OSPF_AREA_ID(MPLSNMS_ATTR.OSPF_AREA_ID),
        IGP_COST(MPLSNMS_ATTR.IGP_COST),;

        private final String attributeName;

        private Attr(String name) {
            this.attributeName = name;
        }

        public String getAttributeName() {
            return this.attributeName;
        }

        public static Attr getByAttributeName(String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }
            for (Attr attr : Attr.values()) {
                if (attr.attributeName.equals(name)) {
                    return attr;
                }
            }
            throw new IllegalArgumentException();
        }
    }

}