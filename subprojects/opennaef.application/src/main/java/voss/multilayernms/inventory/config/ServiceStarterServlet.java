package voss.multilayernms.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfigRegistry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ServiceStarterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ServiceStarterServlet.class);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            MplsNmsConfiguration.getInstance();
            startService();
        } catch (Exception e) {
            log.error("server error." + e.toString());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        startService();
    }

    private synchronized void startService() {
        log.info("ServiceStarterServlet start.");
        try {
            ServiceConfigRegistry configRegistry = ServiceConfigRegistry.getInstance();
            configRegistry.reloadAll();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.info("ServiceStarterServlet end.");
    }
}