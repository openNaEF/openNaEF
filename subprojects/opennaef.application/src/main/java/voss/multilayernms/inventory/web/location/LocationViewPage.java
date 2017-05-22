package voss.multilayernms.inventory.web.location;

import naef.dto.LocationDto;
import naef.dto.NodeDto;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.LocationType;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.LocationRenderer;
import voss.multilayernms.inventory.web.location.LocationUtil.BreadCrumbLink;
import voss.multilayernms.inventory.web.node.NodeEditPage;
import voss.multilayernms.inventory.web.node.NodePageUtil;
import voss.multilayernms.inventory.web.search.NodeSearchPage;
import voss.nms.inventory.model.RenewableAbstractReadOnlyModel;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class LocationViewPage extends WebPage {
    public static final String OPERATION_NAME = "LocationView";
    private final LocationDto current;
    private final LocationNodeModel model;

    public LocationViewPage() {
        this(new PageParameters());
    }

    public LocationViewPage(PageParameters params) {
        Logger log = getLogger();
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.current = LocationUtil.getLocation(params);
            this.model = new LocationNodeModel(this.current);
            this.model.renew();
            log.debug("current location: " + current.getName());
            log.debug("param=" + params);
            ListView<BreadCrumbLink> breadcrumbList = LocationUtil.createBreadCrumbList(current);
            add(breadcrumbList);
            createHeader();
            populateLocation();
            populateNode();
            createAddLocationLink();
            createEditLocationLink();
            createDeleteLocationLink();
            createAddNodeLink();
            log.debug("end");
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private void createHeader() {
        ExternalLink topLink = UrlUtil.getTopLink("top");
        add(topLink);
        Link<Void> reloadLink = new BookmarkablePageLink<Void>("refresh",
                LocationViewPage.class, LocationUtil.getParameters(current));
        add(reloadLink);

        Label categoryLabel = new Label("category", Model.of(LocationRenderer.getLocationType(current)));
        add(categoryLabel);

        String buildingCode = LocationRenderer.getBuildingCode(current);
        if (buildingCode != null) {
            buildingCode = "Building Code: " + buildingCode;
        }
        Label addressLabel = new Label("buildingCode", Model.of(buildingCode));
        add(addressLabel);

        String popName = LocationRenderer.getPopName(current);
        if (popName != null) {
            popName = "POP Name: " + popName;
        }
        Label popNameLabel = new Label("popName", Model.of(popName));
        add(popNameLabel);
    }

    private void createAddLocationLink() {
        Link<Void> link = new Link<Void>("addLocation") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                LocationAddPage page = new LocationAddPage(LocationViewPage.this, current);
                setResponsePage(page);
            }
        };
        if (LocationUtil.isTrash(current)) {
            link.setEnabled(false);
            link.setVisible(false);
        }

        LocationType type = LocationType.getByCaption(LocationRenderer.getLocationType(current));
        if (LocationType.RACK.equals(type)) {
            link.add(new Label("label", ""));
        } else {
            link.add(new Label("label", "Add child location"));
        }
        add(link);
    }

    private void createEditLocationLink() {
        Link<Void> link = new Link<Void>("editLocation") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                LocationEditPage page = new LocationEditPage(LocationViewPage.this, current);
                setResponsePage(page);
            }
        };
        if (!LocationUtil.isEditable(current)) {
            link.setEnabled(false);
            link.setVisible(false);
        }
        add(link);
    }

    private void createDeleteLocationLink() {
        Link<Void> link = new Link<Void>("deleteLocation") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                LocationDeletePage page = new LocationDeletePage(LocationViewPage.this, current);
                setResponsePage(page);
            }
        };
        if (!LocationUtil.isEditable(current)) {
            link.setEnabled(false);
            link.setVisible(false);
        }
        add(link);
    }

    private void createAddNodeLink() {
        final WebPage page;
        if (LocationUtil.isNodeAssignable(current)) {
            page = new NodeEditPage(LocationViewPage.this, null, current);
        } else {
            page = null;
        }
        Link<Void> link = new Link<Void>("addNode") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setResponsePage(page);
            }
        };
        boolean editable = LocationUtil.isNodeAssignable(current);
        link.setEnabled(editable);
        link.setVisible(editable);
        add(link);
    }

    private void populateLocation() throws ExternalServiceException, IOException, RemoteException {
        List<LocationDto> targets = new ArrayList<LocationDto>();
        MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        for (LocationDto location : conn.getActiveLocationDtos()) {
            if (LocationUtil.isTrash(location)) {
                continue;
            }
            if (location.getParent() != null && DtoUtil.isSameMvoEntity(location.getParent(), current)) {
                targets.add(location);
            }
        }
        ListView<LocationDto> list = new ListView<LocationDto>("locationList", targets) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<LocationDto> item) {
                LocationDto location = item.getModelObject();
                PageParameters param = LocationUtil.getParameters(location);
                BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>("locationLink", LocationViewPage.class, param);
                item.add(link);
                Label nodeName = new Label("locationName", LocationUtil.getCaption(location));
                link.add(nodeName);
            }
        };
        add(list);
        WebMarkupContainer container = new WebMarkupContainer("locationBlock");
        add(container);
        container.setVisible(targets.size() > 0);
    }

    private void populateNode() throws ExternalServiceException, IOException, RemoteException {
        ListView<NodeDto> list = new ListView<NodeDto>("nodeList", this.model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<NodeDto> item) {
                NodeDto node = item.getModelObject();
                BookmarkablePageLink<Void> link = NodePageUtil.createNodeLink("nodeLink", node);
                item.add(link);
            }
        };
        add(list);
        WebMarkupContainer container = new WebMarkupContainer("nodeBlock");
        add(container);
        container.setVisible(this.model.isVisible());
    }

    public Logger getLogger() {
        return LoggerFactory.getLogger(LocationViewPage.class);
    }

    protected void onModelChanged() {
        super.onModelChanged();
        this.model.renew();
    }

    private static class LocationNodeModel extends RenewableAbstractReadOnlyModel<List<NodeDto>> {
        private static final long serialVersionUID = 1L;
        private final LocationDto location;
        private final List<NodeDto> nodes = new ArrayList<NodeDto>();

        public LocationNodeModel(LocationDto location) {
            this.location = location;
        }

        @Override
        public List<NodeDto> getObject() {
            return nodes;
        }

        public boolean isVisible() {
            return this.nodes.size() > 0;
        }

        @Override
        public void renew() {
            this.location.renew();
            this.nodes.clear();
            try {
                MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
                Logger log = LoggerFactory.getLogger(LocationViewPage.class);
                for (NodeDto node : conn.getNodes()) {
                    if (log.isTraceEnabled()) {
                        log.trace("target: " + node.getName() + ":"
                                + (LocationUtil.getLocation(node) == null ?
                                "no-location" : LocationUtil.getLocation(node).getName()));
                    }
                    LocationDto loc = LocationUtil.getLocation(node);
                    if (loc == null) {
                        continue;
                    } else if (DtoUtil.mvoEquals(this.location, loc)) {
                        nodes.add(node);
                    }
                }
            } catch (Exception e) {
                throw ExceptionUtils.throwAsRuntime(e);
            }
        }

    }
}
