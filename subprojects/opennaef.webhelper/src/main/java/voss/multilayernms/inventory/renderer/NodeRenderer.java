package voss.multilayernms.inventory.renderer;

import naef.dto.LocationDto;
import naef.dto.NodeDto;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.core.server.util.LocationUtil;
import voss.multilayernms.inventory.database.LocationType;
import voss.nms.inventory.builder.NodeCommandBuilder;
import voss.nms.inventory.config.InventoryConfiguration;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.NodeUtil;
import voss.nms.inventory.util.VlanUtil;

import java.util.List;

public class NodeRenderer extends GenericRenderer {

    public static String getNodeName(NodeDto node) {
        if (node == null) {
            return null;
        }
        return node.getName();
    }

    public static String getNodeType(NodeDto node) {
        if (node == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.NODE_TYPE);
    }

    public static String getVendorName(NodeDto node) {
        if (node == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.VENDOR_NAME);
    }

    public static String getOsType(NodeDto node) {
        if (node == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.OS_TYPE);
    }

    public static String getOsVersion(NodeDto node) {
        if (node == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.OS_VERSION);
    }

    public static String getManagementIpAddress(NodeDto node) {
        if (node == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.MANAGEMENT_IP);
    }

    public static LocationDto getArea(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto area = LocationUtil.getLocation(node, LocationType.AREA);
        return area;
    }

    public static String getAreaName(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto loc = getArea(node);
        return LocationRenderer.getName(loc);
    }

    public static LocationDto getCountry(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto country = LocationUtil.getLocation(node, LocationType.COUNTRY);
        return country;
    }

    public static String getCountryName(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto loc = getCountry(node);
        return LocationRenderer.getName(loc);
    }

    public static LocationDto getCity(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto city = LocationUtil.getLocation(node, LocationType.CITY);
        return city;
    }

    public static String getCityName(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto city = getCity(node);
        return LocationRenderer.getName(city);
    }

    public static LocationDto getBuilding(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto building = LocationUtil.getLocation(node, LocationType.BUILDING);
        return building;
    }

    public static String getBuildingName(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto loc = getBuilding(node);
        return LocationRenderer.getName(loc);
    }

    public static LocationDto getFloor(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto floor = LocationUtil.getLocation(node, LocationType.FLOOR);
        return floor;
    }

    public static String getFloorName(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto floor = getFloor(node);
        return LocationRenderer.getName(floor);
    }

    public static LocationDto getRack(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto loc = LocationUtil.getLocation(node);
        return loc;
    }

    public static String getRackName(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto loc = getRack(node);
        return LocationRenderer.getName(loc);
    }

    public static String getBuildingCode(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto loc = getBuilding(node);
        return DtoUtil.getStringOrNull(loc, MPLSNMS_ATTR.BUILDING_CODE);
    }

    public static String getPopName(NodeDto node) {
        LocationDto floor = getPop(node);
        if (floor == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(floor, MPLSNMS_ATTR.POP_NAME);
    }

    public static LocationDto getPop(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto floor = LocationUtil.getLocation(node, LocationType.FLOOR);
        return floor;
    }

    public static String getExternalInventoryDBID(NodeDto node) {
        if (node == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.EXTERNAL_INVENTORY_DB_ID);
    }

    public static LocationDto getLocation(NodeDto node) {
        if (node == null) {
            return null;
        }
        LocationDto loc = LocationUtil.getLocation(node);
        return loc;
    }

    public static boolean isNodeInTrash(NodeDto node) {
        if (node == null) {
            return true;
        }
        LocationDto loc = getLocation(node);
        return LocationUtil.isTrash(loc);
    }

    public static String getSnmpCommunity(NodeDto node) {
        if (node == null) {
            return null;
        }
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.SNMP_COMMUNITY);
    }

    public static int getSnmpVersionForSnmp4j(NodeDto node) {
        if (node == null) {
            return 1;
        }
        String mode = DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.SNMP_MODE);
        if (mode == null) {
            try {
                mode = InventoryConfiguration.getInstance().getDefaultSnmpMethod();
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }
        if (mode == null) {
            return -1;
        } else if (mode.toLowerCase().contains("v2")) {
            return 1;
        } else if (mode.toLowerCase().contains("v1")) {
            return 0;
        } else if (mode.toLowerCase().contains("v3")) {
            return 3;
        }
        return 1;
    }

    public static String getSnmpMode(NodeDto node) {
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.SNMP_MODE);
    }

    public static String getConsoleLoginPassword(NodeDto node) {
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.TELNET_PASSWORD);
    }

    public static String getConsoleLoginUserName(NodeDto node) {
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.TELNET_ACCOUNT);
    }

    public static String getPrivilegedLoginPassword(NodeDto node) {
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.ADMIN_PASSWORD);
    }

    public static String getPrivilegedUserName(NodeDto node) {
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.ADMIN_ACCOUNT);
    }

    public static String getLocationName(NodeDto node) {
        LocationDto loc = getLocation(node);
        if (loc == null) {
            return "N/A";
        } else {
            return LocationRenderer.getName(loc);
        }
    }

    public static List<String> getZones(NodeDto node) {
        String zones = DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.ZONE_LIST);
        return NodeCommandBuilder.toZoneList(zones);
    }

    public static String getCliMode(NodeDto node) {
        return DtoUtil.getStringOrNull(node, MPLSNMS_ATTR.CLI_MODE);
    }

    public static boolean isVmHostingEnable(NodeDto node) {
        return NodeUtil.isVirtualizationHostingEnable(node);
    }

    public static boolean isVirtualNode(NodeDto node) {
        return NodeUtil.isVirtualNode(node);
    }

    public static String getFeature(NodeDto node) {
        StringBuilder sb = new StringBuilder();
        if (VlanUtil.isVlanEnabled(node)) {
            sb.append("VLAN ");
        }
        if (NodeUtil.isVirtualizationHostingEnable(node)) {
            sb.append("Virtual Host ");
        }
        if (NodeUtil.isVirtualNode(node)) {
            sb.append("Virtual Guest ");
        }
        return sb.toString();
    }

    public static String getPower(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "電源");
    }

    public static String getCPUs(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "CPUs");
    }
    public static String getCPUProduct(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "CPUProduct");
    }

    public static String getMemory(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "Memory");
    }
    public static String getStorage(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "Storage");
    }

    public static String getNICProduct(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "NICProduct");
    }
    public static String getSFPPlusProduct(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "SFP+Product");
    }

    public static String getApplianceType(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "appliance_type");
    }

    public static String getResourcePermission(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "resource_permission");
    }

    public static String getPortSummary(NodeDto node) {
        return DtoUtil.getStringOrNull(node, "PortSummary");
    }
}