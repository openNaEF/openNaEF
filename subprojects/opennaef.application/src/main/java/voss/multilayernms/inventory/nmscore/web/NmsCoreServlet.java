package voss.multilayernms.inventory.nmscore.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.web.flow.Flow;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class NmsCoreServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(NmsCoreServlet.class);

    private Flow flow = new Flow();

    @Override
    public void init() throws ServletException {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        } catch (Throwable e) {
            log.error("Servlet boot error: " + e.getMessage(), e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("requestURL : " + req.getRequestURL());
        log.info("queryString : " + req.getQueryString());
        log.info("remoteAddr : " + req.getRemoteAddr());
        log.info("request method:" + req.getMethod());

        try {
            log.debug("start processing.");
            long current = System.currentTimeMillis();
            flow.execute(new FlowContext(getServletContext(), req, resp));
            log.debug("end processing. lapse=" + (System.currentTimeMillis() - current));
        } catch (ResponseCodeException e) {
            log.error(e.getStatusCode() + " " + e.getMessage(), e);
            sendError(resp, e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            log.error("flow execution error", e);
            sendError(resp, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void sendError(HttpServletResponse resp, String message, int status) {
        try {
            resp.setContentType("text/plane; charset=UTF-8");
            try {
                log.debug(message + " / length:" + message.length() + " / bytes:" + message.getBytes("UTF-8").length);
                resp.setContentLength(message.getBytes("UTF-8").length);
            } catch (UnsupportedEncodingException e) {
                log.error(e.toString());
            }
            resp.setStatus(status);
            PrintWriter writer = resp.getWriter();
            writer.println(message);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error("Error response write error.", e);
        }
    }

    private class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            log.error("Thread: " + t + "  Throwable: " + e);
            e.printStackTrace();
        }
    }

}