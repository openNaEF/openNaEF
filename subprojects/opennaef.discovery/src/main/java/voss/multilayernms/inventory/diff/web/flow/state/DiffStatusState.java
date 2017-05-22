package voss.multilayernms.inventory.diff.web.flow.state;

import voss.multilayernms.inventory.diff.service.DiffSetManager;
import voss.multilayernms.inventory.diff.util.Util;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.nms.inventory.diff.DiffCategory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;

public class DiffStatusState extends AbstractState {

    private final String jspFileName;

    public DiffStatusState(StateId stateId, String jspFileName) {
        super(stateId);
        this.jspFileName = jspFileName;
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {

        DiffSetManager manager = DiffSetManager.getInstance();
        context.getHttpServletRequest().setAttribute("isExternalInventoryDBRunning", manager.isRunning(DiffCategory.EXTERNAL_INVENTORY_DB));
        context.getHttpServletRequest().setAttribute("isDiscoveryRunning", manager.isRunning(DiffCategory.DISCOVERY));
        context.getHttpServletRequest().setAttribute("externalInventoryDBLatestDate", manager.getLatestDate(DiffCategory.EXTERNAL_INVENTORY_DB));
        context.getHttpServletRequest().setAttribute("discoveryLatestDate", manager.getLatestDate(DiffCategory.DISCOVERY));
        context.getHttpServletRequest().setAttribute("externalInventoryDBLatestResult", manager.getLatestResult(DiffCategory.EXTERNAL_INVENTORY_DB));
        context.getHttpServletRequest().setAttribute("discoveryLatestResult", manager.getLatestResult(DiffCategory.DISCOVERY));
        String user = Util.getLockUserName(DiffCategory.EXTERNAL_INVENTORY_DB);
        if (user == null                                        ) user = "";
        context.getHttpServletRequest().setAttribute("externalInventoryDBLockUser", user);
        user = Util.getLockUserName(DiffCategory.DISCOVERY);
        if (user == null                                        ) user = "";
        context.getHttpServletRequest().setAttribute("discoveryLockUser", user);

        RequestDispatcher dispatcher = context.getServletContext().getRequestDispatcher(jspFileName);
        dispatcher.forward(context.getHttpServletRequest(), context.getHttpServletResponse());
    }

}