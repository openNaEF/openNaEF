package voss.multilayernms.inventory.web.location;

import naef.dto.LocationDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import voss.core.server.builder.BuildResult;
import voss.core.server.database.ShellConnector;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.database.LocationType;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.renderer.LocationRenderer;
import voss.nms.inventory.builder.LocationCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LocationEditPage extends WebPage {
    public static final String OPERATION_NAME = "LocationEdit";
    private final LocationDto location;
    private final LocationType type;
    private String name;
    private String note;
    private LocationDto parentLocation;
    private final String editorName;
    private final WebPage backPage;

    public LocationEditPage(WebPage backPage, LocationDto location) {
        if (location == null) {
            throw new IllegalArgumentException("target location is null.");
        }
        this.location = location;
        this.type = LocationType.getByCaption(LocationRenderer.getLocationType(location));
        this.backPage = backPage;
        this.name = LocationRenderer.getName(location);
        this.parentLocation = location.getParent();
        this.note = LocationRenderer.getName(location);

        Label locationName = new Label("locationName", new Model<String>(this.name));
        add(locationName);

        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);

            add(new FeedbackPanel("feedback"));

            Form<Void> editLocationForm = new Form<Void>("editLocationForm");
            add(editLocationForm);

            Button proceedButton = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        LocationDto parent = getLocation().getParent();
                        Set<LocationDto> locations = parent.getChildren();
                        for (LocationDto loc : locations) {
                            if (LocationUtil.getCaption(getLocation()).equals(name)) {
                                continue;
                            }
                            if (LocationUtil.getCaption(loc).equals(name)) {
                                throw new IllegalArgumentException("This is a duplicate location names.");
                            }
                        }
                        LocationCommandBuilder builder = new LocationCommandBuilder(getLocation(), editorName);
                        builder.setCaption(getName());
                        builder.setParent(getParentLocation());
                        builder.setNote(getNote());
                        BuildResult result = builder.buildCommand();
                        if (BuildResult.SUCCESS.equals(result)) {
                            ShellConnector.getInstance().execute(builder);
                        }
                        setResponsePage(LocationViewPage.class, LocationUtil.getParameters(getLocation()));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            editLocationForm.add(proceedButton);

            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            editLocationForm.add(backLink);

            final TextField<String> nameField = new TextField<String>("newLocationName", new PropertyModel<String>(this, "name"));
            nameField.setRequired(true);
            editLocationForm.add(nameField);

            final TextField<String> buildingCodeField = new TextField<String>("buildingCode", new PropertyModel<String>(this, "buildingCode"));
            editLocationForm.add(buildingCodeField);
            buildingCodeField.setEnabled(type == LocationType.BUILDING);
            buildingCodeField.setVisible(type == LocationType.BUILDING);

            final TextField<String> popNameField = new TextField<String>("popName", new PropertyModel<String>(this, "popName"));
            editLocationForm.add(popNameField);
            popNameField.setEnabled(type == LocationType.FLOOR);
            popNameField.setVisible(type == LocationType.FLOOR);

            final TextField<String> noteField = new TextField<String>("note", new PropertyModel<String>(this, "note"));
            editLocationForm.add(noteField);

            final DropDownChoice<LocationDto> locations = new DropDownChoice<LocationDto>("newParentLocation",
                    new PropertyModel<LocationDto>(this, "parentLocation"),
                    populateLocationList(),
                    new LocationChoiceRenderer());
            locations.setRequired(true);
            editLocationForm.add(locations);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    private List<LocationDto> populateLocationList() {
        List<LocationDto> list = new ArrayList<LocationDto>();
        try {
            for (LocationDto location : MplsNmsInventoryConnector.getInstance().getActiveLocationDtos()) {
                if (DtoUtil.mvoEquals(this.location, location)) {
                    continue;
                } else if (LocationUtil.isTrash(location)) {
                    continue;
                }
                list.add(location);
            }
            Collections.sort(list, LocationUtil.getComparator());
            return list;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public LocationDto getLocation() {
        return this.location;
    }

    public WebPage getBackPage() {
        return this.backPage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocationDto getParentLocation() {
        return parentLocation;
    }

    public void setParentLocation(LocationDto parent) {
        this.parentLocation = parent;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

}