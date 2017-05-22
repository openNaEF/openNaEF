package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ATTR;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.naming.naef.AbsoluteNameFactory;
import voss.core.server.util.ConfigModelUtil;
import voss.core.server.util.Util;
import voss.discovery.agent.cisco.ext.StormControlActionRenderer;
import voss.discovery.agent.cisco.ext.StormControlBroadcastLevelRenderer;
import voss.discovery.agent.cisco.ext.StormControlNotificationActionRenderer;
import voss.model.EthernetPort;
import voss.model.EthernetPortsAggregator;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.constants.PortMode;
import voss.nms.inventory.constants.PortType;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.network.NetworkDiffUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EthernetLAGRenderer extends AbstractHardPortRenderer<EthernetPortsAggregator> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public EthernetLAGRenderer(EthernetPortsAggregator lag, String parentID, int depth, Map<String, String> map) {
        super(lag, parentID, depth, map);
        if (parentID == null) {
            throw new IllegalArgumentException("parentID is null.");
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        String lagID = AbsoluteNameFactory.getEthernetLAGName(getModel());
        return AbsoluteNameFactory.getEthernetLAGAbsoluteName(parentAbsoluteName, lagID);
    }

    @Override
    protected String setInventoryID(String parentID) {
        return InventoryIdCalculator.getId(getModel());
    }

    public Map<String, String> getMemberPorts() {
        Map<String, String> result = new HashMap<String, String>();
        EthernetPortsAggregator aps = getModel();
        for (EthernetPort member : aps.getPhysicalPorts()) {
            String memberID = InventoryIdCalculator.getId(member);
            String absoluteName = this.inventoryIDtoAbsoluteNameMap.get(memberID);
            result.put(memberID, absoluteName);
        }
        return result;
    }

    public boolean isRoot() {
        return false;
    }

    @Override
    public int getDepth() {
        return super.getDepth() + 50;
    }

    public List<String> getAttributeNames() {
        List<String> result = new ArrayList<String>();
        for (Attr attr : Attr.values()) {
            result.add(attr.attributeName);
        }
        return result;
    }

    @Override
    public String getValue(String naefAttributeName) {
        Attr attr = Attr.getByAttributeName(naefAttributeName);
        String value = getValue(attr);
        log.trace("result=" + attr.getAttributeName() + ":" + value);
        return value;
    }

    @Override
    public String getValue(Enum<?> attr_) {
        Attr attr = (Attr) attr_;
        EthernetPortsAggregator port = getModel();
        switch (attr) {
            case IFNAME:
                return ConfigModelUtil.getIfName(port);
            case CONFIGNAME:
                return port.getConfigName();
            case IPIFNAME:
                return ConfigModelUtil.getIpIfName(port);
            case PORT_TYPE:
                return PortType.LAG.getCaption();
            case PORT_MODE:
                PortMode portMode = NetworkDiffUtil.getPortMode(port);
                if (portMode == null) {
                    return null;
                } else {
                    return portMode.name();
                }
            case SWITCH_PORT_MODE:
                SwitchPortMode switchPortMode = NetworkDiffUtil.getSwitchPortMode(port);
                if (switchPortMode == null) {
                    return null;
                } else {
                    return switchPortMode.name();
                }
            case IP_ADDRESS:
                return ConfigModelUtil.getIpAddress(port, true);
            case SUBNETMASK:
                return ConfigModelUtil.getSubnetMask(port, true);
            case DESCRIPTION:
                return port.getUserDescription();
            case STATUS_ADMIN:
                String adminStatus = port.getAdminStatus();
                if (adminStatus == null) {
                    return null;
                }
                adminStatus = adminStatus.toLowerCase();
                if (Util.isOneOf(adminStatus, "有効", "enable", "enabled", "true")) {
                    return MPLSNMS_ATTR.ENABLED;
                } else {
                    return MPLSNMS_ATTR.DISABLED;
                }
            case OSPF_AREA_ID:
                return port.getOspfAreaID();
            case IGP_COST:
                return String.valueOf(port.getIgpCost());
            case STORMCONTROL_BROADCAST_LEVEL:
                StormControlBroadcastLevelRenderer blr = new StormControlBroadcastLevelRenderer(port);
                Integer _level = blr.get();
                if (_level == null) {
                    return null;
                } else {
                    int __level = _level.intValue();
                    float level = ((float) __level) / 100f;
                    return String.valueOf(level);
                }
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    @Override
    public List<String> getValues(Enum<?> attr_) {
        Attr attr = (Attr) attr_;
        EthernetPortsAggregator port = getModel();
        switch (attr) {
            case STORMCONTROL_ACTION:
                List<String> result = new ArrayList<String>();
                StormControlActionRenderer ar = new StormControlActionRenderer(port);
                if (ar.isShutdownEnable()) {
                    result.add(MPLSNMS_ATTR.STORMCONTROL_ACTION_VALUE_SHUTDOWN);
                }
                StormControlNotificationActionRenderer nr = new StormControlNotificationActionRenderer(port);
                if (nr.isTrapEnable()) {
                    result.add(MPLSNMS_ATTR.STORMCONTROL_ACTION_VALUE_TRAP);
                }
                return result;
        }
        throw new IllegalStateException("unknown naef attribute: " + attr.getAttributeName());
    }

    public static enum Attr {
        IFNAME(MPLSNMS_ATTR.IFNAME),
        CONFIGNAME(ATTR.CONFIG_NAME),
        IPIFNAME(MPLSNMS_ATTR.IFNAME),
        PORT_TYPE(MPLSNMS_ATTR.PORT_TYPE),
        PORT_MODE("port-mode"),
        SWITCH_PORT_MODE("switch-port-mode"),
        IP_ADDRESS(MPLSNMS_ATTR.IP_ADDRESS),
        SUBNETMASK(MPLSNMS_ATTR.MASK_LENGTH),
        DESCRIPTION(MPLSNMS_ATTR.DESCRIPTION),
        STATUS_ADMIN(MPLSNMS_ATTR.ADMIN_STATUS),
        OSPF_AREA_ID(MPLSNMS_ATTR.OSPF_AREA_ID),
        IGP_COST(MPLSNMS_ATTR.IGP_COST),
        STORMCONTROL_BROADCAST_LEVEL(MPLSNMS_ATTR.STORMCONTROL_BROADCAST_LEVEL),
        STORMCONTROL_ACTION(MPLSNMS_ATTR.STORMCONTROL_ACTION),;

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