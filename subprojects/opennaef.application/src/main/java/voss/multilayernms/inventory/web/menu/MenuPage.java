package voss.multilayernms.inventory.web.menu;


import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import voss.core.server.util.ExceptionUtils;
import voss.multilayernms.inventory.web.link.EtherLinkListPage;
import voss.multilayernms.inventory.web.location.LocationUtil;
import voss.multilayernms.inventory.web.node.NodeEditPage;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.util.UrlUtil;

public class MenuPage extends WebPage {
    public static final String OPERATION_NAME = "Menu";

    public MenuPage() {
        try {
            AAAWebUtil.checkAAA(this, OPERATION_NAME);
            add(UrlUtil.getLink("node", "node"));
            add(UrlUtil.getLink("vmHost", "vm"));
            add(UrlUtil.getLink("location", "loc"));
            add(UrlUtil.getLink("search", "search"));
            Link<Void> link = new Link<Void>("nodeAddLink") {
                private static final long serialVersionUID = 1L;

                public void onClick() {
                    try {
                        WebPage page = new NodeEditPage(MenuPage.this, null, LocationUtil.getRootLocation());
                        setResponsePage(page);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            add(link);

            add(UrlUtil.getLink("vlan", "vlan"));
            add(UrlUtil.getLink("subnet", "subnet"));
            add(UrlUtil.getLink("rsvplsp", "rsvplsp"));
            add(UrlUtil.getLink("path", "path"));
            add(UrlUtil.getLink("pw", "pw"));

            add(UrlUtil.getLink("customer", "customer"));
            Link<Void> etherlink = new Link<Void>("etherlink") {
                private static final long serialVersionUID = 1L;
                public void onClick() {
                    try {
                        WebPage page = new EtherLinkListPage();
                        setResponsePage(page);
                    } catch (Exception e) {
                        throw ExceptionUtils.throwAsRuntime(e);
                    }
                }
            };
            add(etherlink);

            add(UrlUtil.getLink("user", "user"));
            add(UrlUtil.getLink("reloadAppConfig", "reloadAppConfig"));
            add(UrlUtil.getLink("lockmgr", "lockmgr"));
            add(UrlUtil.getLink("deploy", "deploy"));

        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}