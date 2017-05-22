package voss.multilayernms.inventory.external;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import jp.iiga.nmt.core.model.IDiagram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.view.topology.WholeRsvpLspTopologyViewMaker;
import voss.nms.inventory.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WholeLspDiagramBuilderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(WholeLspDiagramBuilderServlet.class);

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
        try {
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            log.debug("start building topology...");
            long time = System.currentTimeMillis();
            IDiagram diagram = new WholeRsvpLspTopologyViewMaker().makeTopologyView();
            long lapse = System.currentTimeMillis() - time;
            log.debug("end building: " + lapse + " ms.");

            String title = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date());
            File file = File.createTempFile("whole-lsp-diagram-" + title, ".xml");
            log.debug("saving to " + file.getAbsolutePath());
            fileWriter = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));

            XStream stream = new XStream(new DomDriver("UTF-8"));
            stream.toXML(diagram, fileWriter);

            resp.setStatus(HttpServletResponse.SC_OK);
            PrintWriter writer = resp.getWriter();
            writer.println("xml saved to " + file.getAbsolutePath());
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