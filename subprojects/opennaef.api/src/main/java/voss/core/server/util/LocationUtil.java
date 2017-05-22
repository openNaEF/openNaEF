package voss.core.server.util;

import naef.dto.LocationDto;
import naef.dto.NodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.dto.EntityDto.Desc;
import voss.core.server.builder.InventoryBuilder;
import voss.core.server.constant.ModelConstant;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.mplsnms.MplsnmsAttrs;
import voss.multilayernms.inventory.database.LocationType;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.database.MPLSNMS_ATTR;
import voss.nms.inventory.util.LocationComparator;

import java.util.*;

public class LocationUtil {
    public static final String KEY_LOCATION_ID = "loc";
    public static final String KEY_TOP_CAPTION = "Location";

    private static Logger log() {
        return LoggerFactory.getLogger(LocationUtil.class);
    }

    public static LocationDto getLocationFromAll(String name) {
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            Set<LocationDto> dtos = conn.getAllLocationDtos();
            for (LocationDto dto : dtos) {
                if (dto.getName().equals(name)) {
                    return dto;
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("no location found with name:" + name, e);
        }
    }

    public static Comparator<LocationDto> getComparator() {
        return new LocationComparator();
    }

    public static List<LocationDto> getRootLocations() {
        List<LocationDto> result = new ArrayList<LocationDto>();
        try {
            for (LocationDto loc : InventoryConnector.getInstance().getAllLocationDtos()) {
                if (isRootLocation(loc)) {
                    result.add(loc);
                }
            }
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static LocationDto getRootLocation() {
        List<LocationDto> rootLocations = getRootLocations();
        if (rootLocations.size() == 1) {
            return rootLocations.get(0);
        } else if (rootLocations.size() == 0) {
            throw new IllegalStateException("no root location.");
        } else {
            throw new IllegalStateException("2 or more root location found.");
        }
    }

    public static LocationDto getLocation(NodeDto node) {
        if (node == null) {
            return null;
        }
        Desc<LocationDto> desc = node.get(MplsnmsAttrs.NodeDtoAttr.SHUUYOU_FLOOR);
        if (desc == null) {
            return null;
        }
        return node.toDto(desc);
    }

    public static void checkDeletable(LocationDto location) throws InventoryException, ExternalServiceException {
        if (location.getChildren() != null && location.getChildren().size() > 0) {
            throw new InventoryException("One or more sub-location found. Please delete the location after all associated sub-locations are deleted/moved.");
        }
        try {
            InventoryConnector conn = InventoryConnector.getInstance();
            for (NodeDto node : conn.getNodes()) {
                LocationDto loc = getLocation(node);
                if (loc == null) {
                    continue;
                }
                if (loc.getName().equals(location.getName())) {
                    throw new InventoryException("Node may belong to the location . " + node.getName());
                }
            }
        } catch (Exception e) {
            if (e instanceof InventoryException) {
                throw (InventoryException) e;
            }
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static boolean isRootLocation(LocationDto target) {
        LocationDto parent = target.getParent();
        log().debug("parent: " + (parent == null ? "N/A" : parent.getName()));
        return parent == null;
    }

    public static boolean isEditable(LocationDto location) {
        if (isTrash(location)) {
            return false;
        } else if (isRootLocation(location)) {
            return false;
        }
        return true;
    }

    public static boolean isTrash(LocationDto location) {
        if (location == null) {
            return false;
        }
        return location.getName().equals(ModelConstant.LOCATION_TRASH);
    }

    public static String getTrashAbsoluteName() {
        return InventoryBuilder.getRelativeName(ATTR.TYPE_LOCATION, ModelConstant.LOCATION_TRASH);
    }

    public static String getCaption(LocationDto location) {
        if (location == null) {
            return null;
        }
        return DtoUtil.getString(location, MPLSNMS_ATTR.CAPTION);
    }

    public static boolean isAlive(LocationDto location) {
        Object flag = location.getValue(ATTR.VISIBLE_FLAG);
        if (flag == null) {
            return true;
        }
        String flagString = (flag instanceof String ? (String) flag : flag.toString());
        return Boolean.parseBoolean(flagString);
    }

    public static boolean hasChild(LocationDto location, String childName) {
        if (location == null) {
            return false;
        }
        for (LocationDto child : location.getChildren()) {
            String caption = getCaption(child);
            if (caption != null && caption.equals(childName)) {
                return true;
            }
            if (child.getName().equals(childName)) {
                return true;
            }
        }
        return false;
    }

    public static LocationDto getLocation(String name) {
        try {
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            Set<LocationDto> dtos = conn.getActiveLocationDtos();
            for (LocationDto dto : dtos) {
                if (dto.getName().equals(name)) {
                    return dto;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }


    public static LocationDto getLocation(NodeDto node, LocationType type) {
        Desc<LocationDto> desc = node.get(MplsnmsAttrs.NodeDtoAttr.SHUUYOU_FLOOR);
        if (desc == null) {
            return null;
        }
        LocationDto loc = node.toDto(desc);
        if (loc == null) {
            return null;
        }
        LocationDto parent = loc;
        while (parent != null) {
            if (getLocationType(parent) == type) {
                break;
            }
            parent = parent.getParent();
        }
        return parent;
    }

    public static List<LocationDto> getChildren(LocationDto location) {
        List<LocationDto> result = new ArrayList<LocationDto>();
        for (LocationDto loc : location.getChildren()) {
            if (isAlive(loc)) {
                result.add(loc);
            }
        }
        return result;
    }

    public static LocationType getLocationType(LocationDto location) {
        if (location == null) {
            return null;
        }
        String type = DtoUtil.getStringOrNull(location, MPLSNMS_ATTR.LOCATION_TYPE);
        if (type == null) {
            return null;
        }
        return LocationType.getByCaption(type);
    }

    public static List<LocationDto> getLocationsByType(LocationType requestedType) throws ExternalServiceException {
        try {
            List<LocationDto> result = new ArrayList<LocationDto>();
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            for (LocationDto loc : conn.getActiveLocationDtos()) {
                if (!isAlive(loc)) {
                    continue;
                }
                LocationType type = getLocationType(loc);
                if (type != null && type == requestedType) {
                    result.add(loc);
                }
            }
            Collections.sort(result, new LocationComparator());
            return result;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static LocationDto getTrash() throws InventoryException {
        return getLocation(ModelConstant.LOCATION_TRASH);
    }
}