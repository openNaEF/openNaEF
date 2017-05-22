package voss.multilayernms.inventory.diff.web.flow.state;

import voss.multilayernms.inventory.diff.util.Util;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.nms.inventory.diff.DiffCategory;

import javax.servlet.ServletException;
import java.io.IOException;

public class UnlockState extends AbstractState {

    public UnlockState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        DiffCategory category = getDiffCategoryFromParameter(context);
        Util.unlock(category, context.getUserName());
    }

}