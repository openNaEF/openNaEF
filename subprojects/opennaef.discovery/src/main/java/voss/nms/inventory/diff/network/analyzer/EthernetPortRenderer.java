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
import voss.model.EthernetPort.Duplex;
import voss.model.LogicalEthernetPort;
import voss.model.PhysicalPort;
import voss.model.VlanDevice;
import voss.model.value.PortSpeedValue;
import voss.nms.inventory.constants.LogConstants;
import voss.nms.inventory.constants.PortConstants;
import voss.nms.inventory.constants.PortMode;
import voss.nms.inventory.constants.SwitchPortMode;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.diff.network.NetworkDiffUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EthernetPortRenderer extends AbstractHardPortRenderer<EthernetPort> {
    protected static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);

    public EthernetPortRenderer(EthernetPort port, String parentID, int depth, Map<String, String> map) {
        super(port, parentID, depth, map);
        if (parentID == null) {
            throw new IllegalArgumentException("parentID is null.");
        }
    }

    @Override
    protected String buildAbsoluteName(String parentAbsoluteName) {
        return AbsoluteNameFactory.getEthernetPortAbsoluteName(getParentAbsoluteName(), getModel());
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
        EthernetPort port = getModel();
        LogicalEthernetPort le = null;
        if (port.getDevice() instanceof VlanDevice) {
            le = ((VlanDevice) port.getDevice()).getLogicalEthernetPort(port);
        }
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
            case PORT_MODE:
                PortMode portMode = NetworkDiffUtil.getPortMode(le);
                if (portMode == null) {
                    return null;
                } else {
                    return portMode.name();
                }
            case SWITCH_PORT_MODE:
                SwitchPortMode switchPortMode = NetworkDiffUtil.getSwitchPortMode(le);
                if (switchPortMode == null) {
                    return null;
                } else {
                    return switchPortMode.name();
                }
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
            case DUPLEX_ADMIN:
                EthernetPort eth = (EthernetPort) port;
                Duplex duplex = eth.getDuplex();
                if (duplex != null) {
                    if (duplex.isAuto()) {
                        return PortConstants.DUPLEX_AUTO;
                    } else if (duplex.equals(Duplex.FULL)) {
                        return PortConstants.DUPLEX_FULL;
                    } else if (duplex.equals(Duplex.HALF)) {
                        return PortConstants.DUPLEX_HALF;
                    }
                    throw new IllegalStateException("illegal duplex_admin value: " + duplex.getValue());
                }
                return null;
            case PORTSPEED_ADMIN:
                PortSpeedValue.Admin speed = port.getPortAdministrativeSpeed();
                if (speed == null) {
                    return null;
                } else if (speed.isAuto()) {
                    return "auto";
                }
                Long speedValue = speed.getValue();
                if (speedValue != null) {
                    return speedValue.toString();
                } else {
                    throw new IllegalStateException("illegal portspeed_admin value: " + speed.toString());
                }
            case PORTSPEED_OPER:
                PortSpeedValue.Oper operSpeed = port.getPortOperationalSpeed();
                if (operSpeed == null) {
                    return null;
                }
                Long operSpeedValue = operSpeed.getValue();
                if (operSpeedValue != null) {
                    return operSpeedValue.toString();
                } else {
                    throw new IllegalStateException("illegal portspeed_oper value: " + operSpeed.toString());
                }
            case IP_ADDRESS:
                return getIpAddress(port);
            case SUBNETMASK:
                return getSubnetMask(port);
            case DESCRIPTION:
                return port.getUserDescription();
            case OSPF_AREA_ID:
                String areaID = port.getOspfAreaID();
                if (areaID == null && le != null) {
                    areaID = le.getOspfAreaID();
                }
                return areaID;
            case IGP_COST:
                int cost = port.getIgpCost();
                if (cost < 1 && le != null) {
                    cost = le.getIgpCost();
                }
                return String.valueOf(cost);
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
        EthernetPort port = getModel();
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

    private String getIpAddress(PhysicalPort port) {
        String ipAddress = ConfigModelUtil.getIpAddress(port, true);
        if (ipAddress != null) {
            return ipAddress;
        }
        if (port instanceof EthernetPort && port.getDevice() instanceof VlanDevice) {
            VlanDevice vd = (VlanDevice) port.getDevice();
            LogicalEthernetPort le = vd.getLogicalEthernetPort((EthernetPort) port);
            if (le != null && le.isAggregated()) {
                return null;
            } else {
                return ConfigModelUtil.getIpAddress(le, true);
            }
        }
        return null;
    }

    private String getSubnetMask(PhysicalPort port) {
        String ipAddress = ConfigModelUtil.getSubnetMask(port, true);
        if (ipAddress != null) {
            return ipAddress;
        }
        if (port instanceof EthernetPort && port.getDevice() instanceof VlanDevice) {
            VlanDevice vd = (VlanDevice) port.getDevice();
            LogicalEthernetPort le = vd.getLogicalEthernetPort((EthernetPort) port);
            if (le != null && le.isAggregated()) {
                return null;
            } else {
                return ConfigModelUtil.getSubnetMask(le, true);
            }
        }
        return null;
    }

    public static enum Attr {
        IFNAME(MPLSNMS_ATTR.IFNAME),
        CONFIGNAME(ATTR.CONFIG_NAME),
        IPIFNAME(MPLSNMS_ATTR.IFNAME),
        PORT_TYPE(MPLSNMS_ATTR.PORT_TYPE),
        MEDIA_TYPE("media-type"),
        PORT_MODE("port-mode"),
        SWITCH_PORT_MODE("switch-port-mode"),
        IP_ADDRESS(MPLSNMS_ATTR.IP_ADDRESS),
        SUBNETMASK(MPLSNMS_ATTR.MASK_LENGTH),
        DESCRIPTION(MPLSNMS_ATTR.DESCRIPTION),
        STATUS_ADMIN(MPLSNMS_ATTR.ADMIN_STATUS),
        PORTSPEED_ADMIN(MPLSNMS_ATTR.PORTSPEED_ADMIN),
        PORTSPEED_OPER(MPLSNMS_ATTR.PORTSPEED_OPER),
        DUPLEX_ADMIN(MPLSNMS_ATTR.DUPLEX_ADMIN),
        PORT_ID(MPLSNMS_ATTR.NAME),
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