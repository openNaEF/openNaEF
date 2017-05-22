package voss.nms.inventory.util;

import org.apache.wicket.Request;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.aaa.*;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;

import java.io.IOException;

public class AAAWebUtil extends AAAUtil {

    public static String checkAAA(WebPage page, String operationName) throws InventoryException, ExternalServiceException, NotLoggedInException {
        try {
            String ipAddress = getIpAddress(page);
            log("auth request: [" + operationName + "] from [" + ipAddress + "]");
            AAAConfiguration aaa = AAAConfiguration.getInstance();
            if (!aaa.isInitialized()) {
                aaa.reloadConfiguration();
            }
            boolean success = aaa.checkAAA(ipAddress, operationName);
            if (!success) {
                throw new InventoryException("Not authorized.");
            }
            AAAUser user = aaa.authenticate(ipAddress);
            return user.getName();
        } catch (AuthorizationException e) {
            throw new InventoryException("Rights violations occurred in the engine. Please check server configuration.", e);
        } catch (IOException e) {
            throw new InventoryException("I/O exception occurred.", e);
        }
    }

    public static String getIpAddress(WebPage page) {
        if (page == null) {
            return "null-page";
        }
        Request request = page.getRequest();
        if (request instanceof WebRequest) {
            WebRequest webreq = (WebRequest) request;
            return webreq.getHttpServletRequest().getRemoteAddr();
        }
        return null;
    }

    private static void log(String msg) {
        Logger authLog = LoggerFactory.getLogger(AAAConfiguration.AAA_LOG_NAME);
        long id = Thread.currentThread().getId();
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(id).append("]");
        sb.append(msg);
        authLog.info(sb.toString());
    }

}