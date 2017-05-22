package voss.nms.inventory.util;

import org.apache.wicket.markup.html.WebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PageUtil {
    private static final Logger log = LoggerFactory.getLogger(PageUtil.class);

    public static void setModelChanged(WebPage page) {
        try {
            Method modelChangedMethod = page.getClass().getMethod("modelChanged", new Class<?>[0]);
            if (modelChangedMethod != null) {
                try {
                    log.debug("called: " + page.getClass().getName() + "#modelChanged");
                    modelChangedMethod.setAccessible(true);
                    modelChangedMethod.invoke(page, new Object[0]);
                } catch (InvocationTargetException e) {
                    log.warn("page.modelChanged() failed.", e);
                } catch (IllegalAccessException e) {
                    log.warn("page.modelChanged() could not be accessed.", e);
                }
            }
        } catch (NoSuchMethodException e) {
            log.debug("no page.modelChanged() method: " + page.getClass().getName());
        }
        WebPage backPage = getBackPage(page);
        if (backPage != null) {
            setModelChanged(backPage);
        }
    }

    public static WebPage getBackPage(WebPage page) {
        try {
            Method getBackPageMethod = page.getClass().getDeclaredMethod("getBackPage", new Class<?>[0]);
            if (getBackPageMethod != null) {
                try {
                    log.debug("called: " + page.getClass().getName() + "#getBackPage");
                    getBackPageMethod.setAccessible(true);
                    Object backedPage = getBackPageMethod.invoke(page, new Object[0]);
                    if (backedPage != null && backedPage instanceof WebPage) {
                        return (WebPage) backedPage;
                    }
                } catch (InvocationTargetException e) {
                    log.warn("page.getBackPage() failed.", e);
                } catch (IllegalAccessException e) {
                    log.warn("page.getBackPage() could not be accessed.", e);
                }
            }
        } catch (NoSuchMethodException e) {
            log.debug("no getBackPage() method: " + page.getClass().getName());
        }
        return null;
    }
}