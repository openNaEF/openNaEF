package voss.multilayernms.inventory.diff.web.flow.state;

import voss.multilayernms.inventory.diff.util.Util;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.nms.inventory.diff.DiffCategory;

import javax.servlet.ServletException;
import java.io.IOException;

public class LockState extends AbstractState {

    public LockState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        DiffCategory category = getDiffCategoryFromParameter(context);
        if (Util.lock(category, context.getUserName())) return;
        String lockUserName = Util.getLockUserName(category);
        if (!lockUserName.equalsIgnoreCase(context.getUserName())) {
            String errorMsg = lockUserName + " has already acquired the lock.";
            throw new ServletException(errorMsg);
        }
    }

}