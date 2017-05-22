package voss.nms.inventory.util;

import naef.dto.NaefDto;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import voss.core.server.config.CoreConfiguration;
import voss.core.server.util.DtoUtil;
import voss.core.server.util.ExceptionUtils;

public class HistoryUtil {
    private HistoryUtil() {
    }

    public static ExternalLink createHistoryLink(NaefDto dto) {
        try {
            CoreConfiguration config = CoreConfiguration.getInstance();
            String url = config.getInventoryHistoryUrl() + (dto != null ? DtoUtil.getMvoId(dto).toString() : "");
            ExternalLink link = new ExternalLink("history", Model.of(url), Model.of("History"));
            link.setEnabled(dto != null);
            link.setVisible(dto != null);
            return link;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static ExternalLink createHistoryLink(NaefDto dto, String label) {
        try {
            CoreConfiguration config = CoreConfiguration.getInstance();
            String url = config.getInventoryHistoryUrl() + (dto != null ? DtoUtil.getMvoId(dto).toString() : "");
            ExternalLink link = new ExternalLink("history", Model.of(url), Model.of(label));
            link.setEnabled(dto != null);
            link.setVisible(dto != null);
            return link;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }
}