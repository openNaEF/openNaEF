package voss.nms.inventory.export;

import naef.dto.NodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.Util;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.util.LocationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeSuggestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final String KEY_QUERY = "q";
    public static final String ENCODING = "UTF-8";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    protected void process(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding(ENCODING);
        String fragment = req.getParameter(KEY_QUERY);
        if (Util.stringToNull(fragment) == null) {
            return;
        }
        if (fragment.length() < 2) {
            return;
        }

        res.setCharacterEncoding(ENCODING);
        res.setHeader("Cache-Control", "no-cache");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Expires", "Tue, 29 Feb 2000 12:00:00 GMT");

        try {
            Logger log = LoggerFactory.getLogger(NodeSuggestServlet.class);
            log.debug("request: " + fragment);
            PrintWriter writer = res.getWriter();
            InventoryConnector conn = InventoryConnector.getInstance();
            List<String> results = new ArrayList<String>();
            for (NodeDto node : conn.getNodes()) {
                if (LocationUtil.getLocation(node) == null) {
                    log.debug("no location; " + node.getName());
                    continue;
                }
                if (node.getName() != null
                        && node.getName().toLowerCase().contains(fragment.toLowerCase())) {
                    results.add(node.getName());
                    log.debug("match: " + node.getName());
                    continue;
                }
                log.debug("not match: " + node.getName());
            }
            Collections.sort(results);
            for (String result : results) {
                writer.write(result);
                writer.write("\r\n");
                writer.flush();
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

}