package voss.multilayernms.inventory.web.location;

import naef.dto.LocationDto;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.database.ShellConnector;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.builder.LocationCommandBuilder;
import voss.nms.inventory.util.AAAWebUtil;

public class LocationDeletePage extends WebPage {
    public static final String OPERATION_NAME = "LocationDelete";
    private final String editorName;

    public LocationDeletePage(final WebPage backPage, final LocationDto location) {
        try {
            this.editorName = AAAWebUtil.checkAAA(this, OPERATION_NAME);
            if (location == null) {
                throw new IllegalStateException("location is null.");
            }
            Label confirmLabel = new Label("locationName", Model.of(location.getName()));
            add(confirmLabel);

            Form<Void> form = new Form<Void>("locationDeleteConfirmForm");
            add(form);

            Button proceed = new Button("proceed") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    try {
                        try {
                            LocationUtil.checkDeletable(location);
                        } catch (InventoryException e) {
                            throw new RuntimeException(e);
                        }
                        LocationCommandBuilder builder = new LocationCommandBuilder(location, editorName);
                        builder.buildDeleteCommand();
                        ShellConnector.getInstance().execute(builder);
                        LocationDto parent = location.getParent();
                        setResponsePage(LocationViewPage.class, LocationUtil.getParameters(parent));
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            form.add(proceed);

            Button back = new Button("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit() {
                    setResponsePage(backPage);
                }
            };
            form.add(back);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    @SuppressWarnings("unused")
    private Logger log() {
        return LoggerFactory.getLogger(LocationDeletePage.class);
    }
}