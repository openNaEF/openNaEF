package voss.multilayernms.inventory.web.location;

import naef.dto.LocationDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.LocationType;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.util.LocationComparator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LocationUtil extends voss.nms.inventory.util.LocationUtil {
    private static Logger log() {
        return LoggerFactory.getLogger(LocationUtil.class);
    }

    public static LocationDto getLocation(PageParameters param) {
        String id = param.getString(KEY_LOCATION_ID);
        try {
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            Set<LocationDto> dtos = conn.getActiveLocationDtos();
            for (LocationDto dto : dtos) {
                if (DtoUtil.getMvoId(dto).toString().equals(id)) {
                    return dto;
                }
            }
            return getLocation(KEY_TOP_CAPTION);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static BookmarkablePageLink<Void> getLocationLink(String id, LocationDto dto) {
        PageParameters parameters = getParameters(dto);
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id, LocationViewPage.class, parameters);
        link.setEnabled(dto != null);
        link.setVisible(dto != null);
        return link;
    }

    public static ListView<BreadCrumbLink> createBreadCrumbList(LocationDto current) {
        final List<BreadCrumbLink> breadcrumbValues = new ArrayList<BreadCrumbLink>();
        List<LocationDto> hierarchy = new ArrayList<LocationDto>();
        LocationDto parent = current;
        while (parent != null) {
            hierarchy.add(parent);
            parent = parent.getParent();
        }
        Collections.reverse(hierarchy);

        for (int i = 0; i < hierarchy.size(); i++) {
            LocationDto location = hierarchy.get(i);
            String s = LocationUtil.getCaption(location);
            String dash = (hierarchy.size() - i > 1 ? " - " : "");
            PageParameters param = LocationUtil.getParameters(location);
            BreadCrumbLink bcl = new BreadCrumbLink(s, param, dash);
            breadcrumbValues.add(bcl);
        }
        ListView<BreadCrumbLink> breadcrumbList =
                new ListView<BreadCrumbLink>("breadcrumbList", breadcrumbValues) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<BreadCrumbLink> item) {
                        BreadCrumbLink bcl = item.getModelObject();
                        BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>("link", LocationViewPage.class, bcl.param);
                        item.add(link);
                        link.add(new Label("label", bcl.label));
                        item.add(new Label("dash", bcl.dash));
                    }
                };
        return breadcrumbList;
    }

    public static class BreadCrumbLink implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label;
        private final PageParameters param;
        private final String dash;

        BreadCrumbLink(String label, PageParameters param, String dash) {
            this.label = label;
            this.param = param;
            this.dash = dash;
        }

        public String getLabel() {
            return this.label;
        }

        public PageParameters getParameters() {
            return this.param;
        }

        public String getDash() {
            if (this.dash == null) {
                return "";
            }
            return this.dash;
        }
    }

    public static boolean isNodeAssignable(LocationDto loc) {
        if (loc == null) {
            return false;
        }
        LocationType type = getLocationType(loc);
        if (type == null) {
            return false;
        }
        switch (type) {
            case RACK:
                return true;
        }
        return false;
    }

    public static LocationDto getParentArea(LocationDto location) {
        if (location == null) {
            return null;
        }
        LocationDto parent = location.getParent();
        if (parent == null) {
            return location;
        }
        LocationType type = getLocationType(location);
        if (type == null) {
            return null;
        }
        switch (type) {
            case RACK:
                return getAreaLocation(parent);
            default:
                return null;
        }
    }

    private static LocationDto getAreaLocation(LocationDto location) {
        if (location == null) {
            throw new IllegalArgumentException();
        }
        log().debug("loc.name=" + location.getName());
        LocationType type = getLocationType(location);
        log().debug("loc.type=" + type);
        if (type == LocationType.AREA) {
            return location;
        }
        LocationDto parent = location.getParent();
        if (parent == null) {
            return location;
        }
        return getAreaLocation(parent);
    }

    public static List<LocationDto> getAreas() throws ExternalServiceException {
        try {
            List<LocationDto> areas = new ArrayList<LocationDto>();
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            for (LocationDto loc : conn.getActiveLocationDtos()) {
                if (!isAlive(loc)) {
                    continue;
                }
                LocationType type = getLocationType(loc);
                if (type != null && type == LocationType.AREA) {
                    areas.add(loc);
                }
            }
            Collections.sort(areas, new LocationComparator());
            return areas;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static List<LocationDto> getCountries(LocationDto area) throws ExternalServiceException {
        try {
            List<LocationDto> countries = new ArrayList<LocationDto>();
            if (area == null) {
                return Collections.emptyList();
            }
            for (LocationDto loc : area.getChildren()) {
                if (!isAlive(loc)) {
                    continue;
                }
                LocationType type = getLocationType(loc);
                if (type != null && type == LocationType.COUNTRY) {
                    countries.add(loc);
                }
            }
            Collections.sort(countries, new LocationComparator());
            return countries;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static List<LocationDto> getCities(LocationDto country) throws ExternalServiceException {
        try {
            if (country == null) {
                return Collections.emptyList();
            }
            List<LocationDto> countries = new ArrayList<LocationDto>();
            for (LocationDto loc : country.getChildren()) {
                if (!isAlive(loc)) {
                    continue;
                }
                LocationType type = getLocationType(loc);
                if (type != null && type == LocationType.CITY) {
                    countries.add(loc);
                }
            }
            Collections.sort(countries, new LocationComparator());
            return countries;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static List<LocationDto> getBuildings(LocationDto city) throws ExternalServiceException {
        try {
            if (city == null) {
                return Collections.emptyList();
            }
            List<LocationDto> countries = new ArrayList<LocationDto>();
            for (LocationDto loc : city.getChildren()) {
                if (!isAlive(loc)) {
                    continue;
                }
                LocationType type = getLocationType(loc);
                if (type != null && type == LocationType.BUILDING) {
                    countries.add(loc);
                }
            }
            Collections.sort(countries, new LocationComparator());
            return countries;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static List<LocationDto> getFloors(LocationDto building) throws ExternalServiceException {
        try {
            if (building == null) {
                return Collections.emptyList();
            }
            List<LocationDto> countries = new ArrayList<LocationDto>();
            for (LocationDto loc : building.getChildren()) {
                if (!isAlive(loc)) {
                    continue;
                }
                LocationType type = getLocationType(loc);
                if (type != null && type == LocationType.FLOOR) {
                    countries.add(loc);
                }
            }
            Collections.sort(countries, new LocationComparator());
            return countries;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static List<LocationDto> getRacks(LocationDto floor) throws ExternalServiceException {
        try {
            if (floor == null) {
                return Collections.emptyList();
            }
            List<LocationDto> countries = new ArrayList<LocationDto>();
            for (LocationDto loc : floor.getChildren()) {
                if (!isAlive(loc)) {
                    continue;
                }
                LocationType type = getLocationType(loc);
                if (type != null && type == LocationType.RACK) {
                    countries.add(loc);
                }
            }
            Collections.sort(countries, new LocationComparator());
            return countries;
        } catch (Exception e) {
            throw ExceptionUtils.getExternalServiceException(e);
        }
    }

    public static BookmarkablePageLink<Void> getTopLink(String id) {
        BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id, LocationViewPage.class);
        return link;
    }


    public static boolean checkLocation(PageParameters param) {
        boolean result = true;
        String id = param.getString(LocationUtil.KEY_LOCATION_ID);
        if (id == null || id.equals("")) {
            return false;
        }
        try {
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            Set<LocationDto> dtos = conn.getActiveLocationDtos();
            for (LocationDto dto : dtos) {
                if (DtoUtil.getMvoId(dto).toString().equals(id)) {
                    return false;
                }
            }
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
        return result;
    }
}