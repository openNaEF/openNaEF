package voss.multilayernms.inventory.diff.web.flow;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class FlowContext {

    @SuppressWarnings("unused")
    private final Flow flow;
    private final ServletContext context;
    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private final Map<String, String[]> overrideParameters = new HashMap<String, String[]>();
    @SuppressWarnings("unchecked")
    public FlowContext(Flow flow, ServletContext context, HttpServletRequest req, HttpServletResponse resp) {
        this.flow = flow;
        this.context = context;
        this.req = req;
        this.resp = resp;
        this.overrideParameters.putAll(req.getParameterMap());
    }

    public String getUserName() {
        return req.getHeader("X-VOSS-REQUESTED-USER");
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
}