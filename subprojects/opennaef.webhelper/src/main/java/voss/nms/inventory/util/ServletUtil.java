package voss.nms.inventory.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.aaa.NotLoggedInException;
import voss.core.server.util.ExceptionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ServletUtil {
    public static final String RESULT = "X-result-status";
    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_FAILURE = "failure";

    private static final Logger log = LoggerFactory.getLogger(ServletUtil.class);

    public static void handleException(HttpServletResponse res, Exception e) throws IOException {
        log.error("can't proceed.", e);
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (e instanceof NotLoggedInException) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        res.setContentType("text/plain");
        res.addHeader("X-result-status", RESULT_FAILURE);
        res.addHeader("X-error-reason", rootCause.getMessage());
        PrintWriter writer = res.getWriter();
        writer.write("exception occured: root-cause is '" + rootCause.getMessage() + "'.");
    }

    public static void handleUnsupportedOperation(HttpServletResponse res) throws IOException {
        log.error("not supported operation.", new Throwable());
        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        res.setContentType("text/plain");
        res.addHeader(RESULT, RESULT_FAILURE);
        PrintWriter writer = res.getWriter();
        writer.write("not supported operation.");
    }

}