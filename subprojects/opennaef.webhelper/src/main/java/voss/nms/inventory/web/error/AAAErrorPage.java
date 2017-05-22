package voss.nms.inventory.web.error;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

public class AAAErrorPage extends WebPage {

    public AAAErrorPage(String errorMessage) {
        Label label = new Label("errorMessage", Model.of(errorMessage));
        add(label);
    }
}