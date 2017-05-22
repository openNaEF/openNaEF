package voss.nms.inventory.web;

import naef.ui.NaefShellFacade.ShellException;
import org.apache.wicket.Page;
import org.apache.wicket.Response;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.aaa.NotLoggedInException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.ExceptionUtils;
import voss.nms.inventory.util.AAAWebUtil;
import voss.nms.inventory.web.error.ErrorItem;
import voss.nms.inventory.web.error.ErrorPage;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InventoryRequestCycle extends WebRequestCycle {
    private final Logger log = LoggerFactory.getLogger(InventoryRequestCycle.class);
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS");

    public InventoryRequestCycle(WebApplication application, WebRequest request, Response response) {
        super(application, request, response);
    }

    @Override
    public Page onRuntimeException(Page page, RuntimeException e) {
        String errorCode = AAAWebUtil.getIpAddress((WebPage) page) + "-" + df.format(new Date());
        log.info("Exception: page: " + page + " errorCode: " + errorCode, e);
        Throwable cause = getCause(e);
        if (cause instanceof WicketRuntimeException) {
            if (cause instanceof PageExpiredException) {
                List<ErrorItem> items = new ArrayList<ErrorItem>();
                ErrorItem item1 = new ErrorItem("Symptom",
                        "Page expired. ");
                items.add(item1);
                ErrorItem item2 = new ErrorItem("Action",
                        "Please back to previous page using browser's back button or [back] button below, and reload page.");
                items.add(item2);
                ErrorPage errorPage = new ErrorPage((WebPage) page,
                        "Page expired", "Page expired",
                        items);
                return errorPage;
            }
            cause = getCause(cause);
        }
        Throwable rootCause = ExceptionUtils.getRootCause(cause);
        if (rootCause instanceof NotLoggedInException) {
            List<ErrorItem> items = new ArrayList<ErrorItem>();
            ErrorItem item1 = new ErrorItem("Symptom",
                    "You are not logged in. ");
            items.add(item1);
            ErrorItem item2 = new ErrorItem("Action",
                    "Login first, then reload this page using browser's reload button.");
            items.add(item2);
            ErrorPage errorPage = new ErrorPage((WebPage) page,
                    "Authentication failed", "Authentication failed",
                    items);
            return errorPage;
        }
        if (rootCause instanceof ShellException) {
            String msg = rootCause.getMessage();
            if (msg == null) {
                msg = "";
            }
            List<ErrorItem> items = new ArrayList<ErrorItem>();
            ErrorItem item1;
            if (msg.contains("Inventory obsoleted.")) {
                item1 = new ErrorItem("Symptom",
                        "Target inventory object is obsoleted because updated by other.");
            } else if (rootCause.getMessage().contains("更新されています")) {
                item1 = new ErrorItem("Symptom",
                        "An error occured while updating inventory. [Has been updated by other users.]");
            } else {
                item1 = new ErrorItem("Symptom",
                        "An error occured while updating inventory. [" + rootCause.getMessage() + "]");
            }
            items.add(item1);
            ErrorItem item2 = new ErrorItem("Action",
                    "Please go back to previous page of edit page, and refresh and go edit page again.");
            items.add(item2);
            ErrorPage errorPage = new ErrorPage((WebPage) page,
                    "Update failed", "Update failed",
                    items);
            return errorPage;
        }
        if (cause instanceof InvocationTargetException) {
            cause = getCause(cause);
        }
        String title = "Error [" + getTitle(cause) + "]";
        ErrorPage errorPage = new ErrorPage((WebPage) page, "Error", title,
                getMessage(cause, page, errorCode));
        return errorPage;
    }

    private String getTitle(Throwable cause) {
        if (cause instanceof InventoryException) {
            return cause.getMessage();
        }
        Throwable root = ExceptionUtils.getRootCause(cause);
        return root.getMessage();
    }

    private List<ErrorItem> getMessage(Throwable cause, Page page, String errorCode) {
        List<ErrorItem> items = new ArrayList<ErrorItem>();
        ErrorItem item1 = new ErrorItem("Symptom", "Your operation is failed with error.");
        items.add(item1);
        ErrorItem item2 = new ErrorItem("Cause", cause.getMessage());
        items.add(item2);
        if (cause instanceof InventoryException) {
            Throwable root = ExceptionUtils.getRootCause(cause);
            if (root != null || root != cause) {
                ErrorItem item3 = new ErrorItem("Root Cause", root.getMessage());
                items.add(item3);
            }
        }
        ErrorItem item4 = new ErrorItem("Action", "Please contact system administrator with additional information below.");
        item4.addLine(errorCode);
        items.add(item4);
        return items;
    }

    private Throwable getCause(Throwable th) {
        Throwable cause = th;
        if (th.getCause() != null) {
            cause = th.getCause();
        }
        return cause;
    }

}