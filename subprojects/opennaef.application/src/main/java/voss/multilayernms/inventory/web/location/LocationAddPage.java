package voss.multilayernms.inventory.web.location;

import naef.dto.LocationDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.builder.MplsNmsLocationCommandBuilder;
import voss.multilayernms.inventory.database.LocationType;
import voss.multilayernms.inventory.renderer.LocationRenderer;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.util.AAAWebUtil;

import java.util.Set;

public class LocationAddPage extends WebPage {
    public static final String OPERATION_NAME = "LocationAdd";
    public static final String KEY_LOCATION_ID = "loc";
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(LocationAddPage.class);
    private final LocationDto parentLocation;
    private final LocationType type;
    private final String editorName;
    private final WebPage backPage;
    private String caption = null;

    public LocationAddPage(WebPage backPage, LocationDto parent) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.parentLocation = parent;
            this.type = selectChildType(parent);
            this.backPage = backPage;

            add(new FeedbackPanel("feedback"));

            String parentLocationName = LocationUtil.getCaption(parent);
            final Label parentLocationNameLabel = new Label("parentLocationName", new Model<String>(parentLocationName));
            add(parentLocationNameLabel);

            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            add(backLink);

            final TextField<String> nameField = new TextField<String>("name",
                    new PropertyModel<String>(this, "caption"), String.class);
            nameField.setRequired(true);
            Form<Void> addLocationForm = new Form<Void>("addLocation") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit() {
                    try {
                        String name = getCaption();
                        LocationDto parent = getParentLocation();
                        LocationType type = getType();
                        for (LocationDto child : parent.getChildren()) {
                            String locName = LocationUtil.getCaption(child);
                            if (locName != null && locName.equals(name)) {
                                throw new IllegalStateException("duplicated location name: " + name);
                            }
                        }
                        Set<LocationDto> locations = parent.getChildren();
                        for (LocationDto loc : locations) {
                            if (LocationUtil.getCaption(loc).equals(name)) {
                                throw new IllegalArgumentException("This is a duplicate location names.");
                            }
                        }

                        MplsNmsLocationCommandBuilder builder = new MplsNmsLocationCommandBuilder(parent, type, name, editorName);
                        builder.setSource(DiffCategory.INVENTORY.name());
                        builder.buildCommand();
                        ShellConnector.getInstance().execute(builder);
                        String path = builder.getPath();
                        LocationDto newLocation = LocationUtil.getLocation(path);
                        setResponsePage(new LocationEditPage(getBackPage(), newLocation));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            addLocationForm.add(nameField);
            add(addLocationForm);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getCaption() {
        return this.caption;
    }

    public void setCaption(String s) {
        this.caption = s;
    }

    public LocationDto getParentLocation() {
        return this.parentLocation;
    }

    public LocationType getType() {
        return this.type;
    }

    private LocationType selectChildType(LocationDto location) {
        LocationType t = LocationType.getByCaption(LocationRenderer.getLocationType(parentLocation));
        switch (t) {
            case ROOT:
                return LocationType.AREA;
            case AREA:
                return LocationType.COUNTRY;
            case COUNTRY:
                return LocationType.CITY;
            case CITY:
                return LocationType.BUILDING;
            case BUILDING:
                return LocationType.FLOOR;
            case FLOOR:
                return LocationType.RACK;
        }
        throw new IllegalStateException("unexpected location-type:" + t.name());
    }
}