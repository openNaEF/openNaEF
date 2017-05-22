package voss.multilayernms.inventory.lock;

import naef.dto.NaefDto;
import naef.ui.NaefDtoFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.MVO.MvoId;
import tef.skelton.dto.EntityDto;
import voss.core.server.aaa.AAAUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.nms.inventory.util.NameUtil;
import voss.nms.inventory.util.ServletUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LockServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LockServlet.class);

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
        try {
            String ipAddress = req.getRemoteAddr();
            String lockUser = AAAUtil.checkAAA(ipAddress, OPERATION_NAME);
            MplsNmsInventoryConnector conn = MplsNmsInventoryConnector.getInstance();
            NaefDtoFacade facade = conn.getDtoFacade();
            LockManager manager = LockManager.getInstance();
            String[] lockTargets = req.getParameterValues(PARAM_LOCK_TARGET);
            if (lockTargets != null && lockTargets.length > 0) {
                for (String target : lockTargets) {
                    log.debug("lock requestd: " + target);
                    if (target == null) {
                        continue;
                    }
                    if (target.startsWith("mvo:")) {
                        target = target.replace("mvo:", "");
                    }
                    log.debug("lock target: " + target);
                    String caption = target;
                    MvoId mvoID = facade.toMvoId(target);
                    EntityDto dto = facade.getMvoDto(mvoID, null);
                    if (dto != null && dto instanceof NaefDto) {
                        caption = NameUtil.getCaption((NaefDto) dto);
                    }
                    manager.lock(target, caption, lockUser);
                    log.debug("locked: " + target);
                }
            }
            String[] unlockTargets = req.getParameterValues(PARAM_UNLOCK_TARGET);
            if (unlockTargets != null && unlockTargets.length > 0) {
                for (String target : unlockTargets) {
                    log.debug("unlock requestd: " + target);
                    if (target == null) {
                        continue;
                    }
                    if (target.startsWith("mvo:")) {
                        target = target.replace("mvo:", "");
                    }
                    manager.unlock(target);
                    if (null == manager.getLockUser(target)) {
                        log.info("unlock success.");
                    } else {
                        log.info("unlock fail.");
                    }
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        } catch (Exception e) {
            ServletUtil.handleException(resp, e);
        }
    }

}