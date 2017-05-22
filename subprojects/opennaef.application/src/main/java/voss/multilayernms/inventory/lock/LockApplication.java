package voss.multilayernms.inventory.lock;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class LockApplication extends WebApplication {

    @Override
    public Class<? extends Page> getHomePage() {
        return LockEditPage.class;
    }

}