package voss.multilayernms.inventory.external;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.constants.ViewConstants;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.database.InventoryConnector;
import voss.nms.inventory.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WholePhysicalDiagramPositionExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(WholePhysicalDiagramPositionExportServlet.class);

    public static final String OPERATION_NAME = "LockServlet";
    public static final String PARAM_LOCK_TARGET = "L";
    public static final String PARAM_UNLOCK_TARGET = "U";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        lock(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        lock(req, resp);
    }

    private void lock(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        OutputStreamWriter fileWriter = null;
        List<String> positionCmds = new ArrayList<String>();
        InventoryConnector conn = MplsNmsInventoryConnector.getInstance();
        try {
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            log.debug("start topology position export...");
            long time = System.currentTimeMillis();
            long lapse = System.currentTimeMillis() - time;
            log.debug("end building: " + lapse + " ms.");

            String title = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date());
            File file = File.createTempFile("whole-physical-diagram-position-" + title, ".txt");
            log.debug("saving to " + file.getAbsolutePath());
            fileWriter = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));

            positionCmds.add("# This file created at " + title);
            positionCmds.add("process-batch");
            positionCmds.add("\n");

            for (NodeDto node : conn.getActiveNodes()) {
                String nodePosition = DtoUtil.getStringOrNull(node, ViewConstants.ATTR_POSITION);
                if (nodePosition != null) {
                    positionCmds.add("\n");
                    positionCmds.add("## " + node.getName() + " Posiotion Command ");
                    positionCmds.add("context \"" + node.getName() + "\"");
                    positionCmds.add("attribute set position \"" + nodePosition + "\"");

                    for (PortDto p : node.getPorts()) {
                        String portPosition = DtoUtil.getStringOrNull(p, ViewConstants.ATTR_POSITION);
                        if (portPosition != null) {
                            positionCmds.add("context \"" + p.getAbsoluteName() + "\"");
                            positionCmds.add("attribute set position \"" + portPosition + "\"");
                        }
                    }
                }
            }
            positionCmds.add("\n");
            positionCmds.add("end");

            for (String cmd : positionCmds) {
                fileWriter.append(cmd);
                fileWriter.append("\n");
            }
            fileWriter.flush();

            resp.setStatus(HttpServletResponse.SC_OK);
            PrintWriter writer = resp.getWriter();
            writer.println("whole-physical-diagram-position file saved to " + file.getAbsolutePath());
            writer.flush();
            writer.close();
            return;
        } catch (Exception e) {
            ServletUtil.handleException(resp, e);
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

}