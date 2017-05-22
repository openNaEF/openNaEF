package voss.nms.inventory.util;

import org.apache.wicket.markup.html.link.ExternalLink;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.config.InventoryConfiguration;

public class UrlUtil {

    public static ExternalLink getTopLink(String id) {
        try {
            String url = InventoryConfiguration.getInstance().getUrlBase();
            ExternalLink link = new ExternalLink(id, url);
            return link;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static ExternalLink getTopLink() {
        return getTopLink("top");
    }

    public static ExternalLink getLink(String id, String suffix) {
        try {
            String url = InventoryConfiguration.getInstance().getUrlBase();
            if (url.endsWith("/")) {
                url = url + suffix;
            } else {
                url = url + "/" + suffix;
            }
            ExternalLink link = new ExternalLink(id, url);
            return link;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}