package voss.multilayernms.inventory.web.config;


import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import voss.nms.inventory.web.InventoryRequestCycle;

public class MplsnmsConfigurationApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return MplsNmsConfigurationEditPage.class;
    }

    @Override
    public void init() {
        getMarkupSettings().setDefaultMarkupEncoding("UTF-8");
    }

    @Override
    public final RequestCycle newRequestCycle(final Request request, final Response response) {
        return new InventoryRequestCycle(this, (WebRequest) request, (WebResponse) response);
    }

}