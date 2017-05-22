package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.ConfigModelUtil;
import voss.model.Channel;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.database.MPLSNMS_ATTR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TdmSerialIfRenderer extends AbstractRenderer<Channel> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public TdmSerialIfRenderer(Channel port, String parentID, int depth, Map<String, String> map) {
        super(port, parentID, depth, map);
        if (parentID == null) {
            throw new IllegalArgumentException("parentID is null.");
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        return AbsoluteNameFactory.getTdmSerialIfAbsoluteName(getParentAbsoluteName(), getModel());
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

    @Override
    public String getValue(Enum<?> attr_) {
        Attr attr = (Attr) attr_;
        Channel port = getModel();
        switch (attr) {
            case BANDWIDTH:
                Long bandwidth = port.getBandwidth();
                if (bandwidth != null) {
                    return bandwidth.toString();
                }
                return null;
            case TIMESLOT:
                return String.valueOf(port.getTimeslotRange());
            case CHANNEL_GROUP:
                return port.getChannelGroupId();
            case IFNAME:
                return AbsoluteNameFactory.getTdmSerialIfName(port);
            case CONFIGNAME:
                return port.getConfigName();
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
        BANDWIDTH(MPLSNMS_ATTR.BANDWIDTH),
        TIMESLOT(MPLSNMS_ATTR.TIMESLOT),
        CHANNEL_GROUP(MPLSNMS_ATTR.CHANNEL_GROUP),
        IFNAME(MPLSNMS_ATTR.IFNAME),
        CONFIGNAME(ATTR.CONFIG_NAME),
        IP_ADDRESS(MPLSNMS_ATTR.IP_ADDRESS),
        SUBNETMASK(MPLSNMS_ATTR.MASK_LENGTH),
        DESCRIPTION(MPLSNMS_ATTR.DESCRIPTION),
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