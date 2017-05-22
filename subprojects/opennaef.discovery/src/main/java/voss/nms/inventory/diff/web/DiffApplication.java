package voss.nms.inventory.diff.web;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class DiffApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return DiffStatusPage.class;
    }

}