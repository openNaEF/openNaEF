package voss.multilayernms.inventory.redirector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.aaa.AAAUtil;
import voss.core.server.config.ServiceConfigRegistry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class RedirectServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(RedirectServlet.class);

    private Distributer distributer;

    @Override
    public void init() {
        try {
            String keyRootDir = ServiceConfigRegistry.KEY_ROOT_DIR;
            String defaultRootDirName = ServiceConfigRegistry.DEFAULT_ROOT_DIR_NAME;
            String configDirName = ServiceConfigRegistry.CONFIG_DIR_NAME;
            String dirName = System.getProperty(keyRootDir, defaultRootDirName);
            String fileName = "redirect.config";
            distributer = new Distributer(dirName + "/" + configDirName + "/" + fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            String userName = AAAUtil.checkAAA(req.getRemoteHost(), req.getParameter("cmd"));
            req.setAttribute("userName", userName);
            log.info("redirect to [" + distributer.getRedirectUrl(req) + "]");
            res.sendRedirect(distributer.getRedirectUrl(req));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServletException(e.getMessage(), e);
        }
    }
}