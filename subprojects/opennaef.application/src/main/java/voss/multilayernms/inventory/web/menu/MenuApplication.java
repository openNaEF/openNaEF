package voss.multilayernms.inventory.web.menu;


import org.apache.wicket.*;
import org.apache.wicket.protocol.http.SecondLevelCacheSessionStore;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.protocol.http.pagestore.DiskPageStore;
import org.apache.wicket.session.ISessionStore;
import org.apache.wicket.util.lang.Bytes;
import voss.nms.inventory.session.InventoryWebSession;
import voss.nms.inventory.web.InventoryRequestCycle;

public class MenuApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return MenuPage.class;
    }

    @Override
    public void init() {
        getMarkupSettings().setDefaultMarkupEncoding("UTF-8");
    }

    @Override
    public final RequestCycle newRequestCycle(final Request request, final Response response) {
        return new InventoryRequestCycle(this, (WebRequest) request, (WebResponse) response);
    }

    @Override
    public Session newSession(Request request, Response response) {
        return new InventoryWebSession(request);
    }

    @Override
    protected ISessionStore newSessionStore() {
        DiskPageStore page = new DiskPageStore((int) Bytes.megabytes(100).bytes(), (int) Bytes.gigabytes(1.0).bytes(), 50);
        SecondLevelCacheSessionStore store = new SecondLevelCacheSessionStore(this, page);
        return store;
    }

}