package voss.nms.inventory.util;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.PopupSettings;

public class WicketUtil {

    public static PopupSettings getPopupSettingsForWindowOpen() {
        PopupSettings popupSettings = new PopupSettings(PopupSettings.LOCATION_BAR |
                PopupSettings.MENU_BAR | PopupSettings.RESIZABLE |
                PopupSettings.SCROLLBARS | PopupSettings.STATUS_BAR |
                PopupSettings.TOOL_BAR);
        return popupSettings;
    }

    public static void toPopup(Link<?> link) {
        if (link != null) {
            link.setPopupSettings(getPopupSettingsForWindowOpen());
        }
    }

}