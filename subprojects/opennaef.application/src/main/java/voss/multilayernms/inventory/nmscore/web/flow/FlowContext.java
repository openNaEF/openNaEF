package voss.multilayernms.inventory.nmscore.web.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.inventory.constants.HTTP_REQUEST_HEADER;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


public class FlowContext {
    private final Logger log = LoggerFactory.getLogger(FlowContext.class);

    private final ServletContext context;
    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private final Map<String, String[]> overrideParameters = new HashMap<String, String[]>();

    @SuppressWarnings("unchecked")
    public FlowContext(ServletContext context, HttpServletRequest req, HttpServletResponse resp)
            throws UnsupportedEncodingException {
        req.setCharacterEncoding("UTF-8");
        this.context = context;
        this.req = req;
        this.resp = resp;
        this.overrideParameters.putAll(req.getParameterMap());
    }

    public String getUser() {
        return (String) req.getHeader(HTTP_REQUEST_HEADER.X_VOSS_REQUESTED_USER);
    }

    public ServletContext getServletContext() {
        return context;
    }

    public HttpServletRequest getHttpServletRequest() {
        return req;
    }

    public HttpServletResponse getHttpServletResponse() {
        return resp;
    }

    public String getParameter(String name) {
        if (overrideParameters.containsKey(name)) {
            return overrideParameters.get(name)[0];
        }
        return req.getParameter(name);
    }

    public Map<String, String[]> getParameterMap() {
        return overrideParameters;
    }

    public void setFileDownloadHeader(String fileName) {
        resp.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        resp.setContentType("application/octet-stream");
    }

    public void dumpParams() {
        @SuppressWarnings("unchecked")
        Enumeration<String> enumeration = req.getParameterNames();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            for (String value : req.getParameterValues(key)) {
                log.info("param[" + key + "] = " + value);
            }
        }
    }
}