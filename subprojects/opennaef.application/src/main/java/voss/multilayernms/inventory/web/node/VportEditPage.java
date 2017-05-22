package voss.multilayernms.inventory.web.node;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.util.AAAWebUtil;

public class VportEditPage extends WebPage {
    public static final String OPERATION_NAME = "VportEdit";
    private final WebPage backPage;

    public VportEditPage(final WebPage backPage) {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            this.backPage = backPage;

            Link<Void> backLink = new Link<Void>("back") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick() {
                    setResponsePage(getBackPage());
                }
            };
            add(backLink);
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public WebPage getBackPage() {
        return backPage;
    }


}