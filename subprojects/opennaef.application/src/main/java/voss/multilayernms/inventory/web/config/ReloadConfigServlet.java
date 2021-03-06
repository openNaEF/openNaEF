package voss.multilayernms.inventory.web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.config.ServiceConfigRegistry;
import voss.core.server.database.CoreConnector;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.web.menu.MenuPage;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static java.lang.Thread.sleep;

public class ReloadConfigServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ReloadConfigServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("ReloadConfigServlet start.");
        try {
            ServiceConfigRegistry configRegistry = ServiceConfigRegistry.getInstance();
            configRegistry.reloadAll();
            CoreConnector.renew();
            InventoryConnector.renew();
            MplsNmsInventoryConnector.renew();
            resp.setStatus(HttpServletResponse.SC_OK);
            PrintWriter writer = resp.getWriter();
            writer.write("reload success.");
        } catch (Exception e) {
            ServletUtil.handleException(resp, e);
        }
        log.info("ReloadConfigServlet end.");
    }
}