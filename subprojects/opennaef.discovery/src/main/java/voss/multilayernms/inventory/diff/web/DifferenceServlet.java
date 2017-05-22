package voss.multilayernms.inventory.diff.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.aaa.AAAConfiguration;
import voss.multilayernms.inventory.diff.service.RegularExecution;
import voss.multilayernms.inventory.diff.util.ConfigUtil;
import voss.multilayernms.inventory.diff.web.flow.Flow;
import voss.nms.inventory.constants.LogConstants;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class DifferenceServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            ConfigUtil conf = ConfigUtil.getInstance();
            if (!conf.reload())
                throw new ServletException("Config load error.");
            checkAndCreateDirs(conf.getDebugDumpDir());
            checkAndCreateDirs(conf.getDifferenceSetDir());
            RegularExecution.getInstance().reScheduleAll();
            AAAConfiguration.getInstance().reloadConfiguration();
        } catch (Exception e) {
            log.error("got exception.", e);
        }
    }

    private void checkAndCreateDirs(String dir) {
        File file = new File(dir);
        if (!file.exists())
            file.mkdirs();
    }

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LogConstants.DIFF_SERVICE);
    private Flow flow = new Flow();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("ip[" + req.getRemoteAddr() + "] requestURL[" + req.getRequestURL() + "] Query["
                + req.getQueryString() + "]");

        try {
            flow.execute(getServletContext(), req, resp);
        } catch (Exception e) {
            log.error("flow execution error", e);
            resp.setContentType("text/plane; charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            PrintWriter pw = resp.getWriter();
            pw.println(e.getMessage());
            pw.close();
        }
    }
}