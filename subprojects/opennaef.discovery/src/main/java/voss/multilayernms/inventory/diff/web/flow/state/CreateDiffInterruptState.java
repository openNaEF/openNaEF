package voss.multilayernms.inventory.diff.web.flow.state;

import voss.multilayernms.inventory.diff.service.DiffSetManager;
import voss.multilayernms.inventory.diff.util.Util;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.nms.inventory.diff.DiffCategory;

import javax.servlet.ServletException;
import java.io.IOException;

public class CreateDiffInterruptState extends AbstractState {

    private DiffCategory category;

    public CreateDiffInterruptState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        category = getDiffCategoryFromParameter(context);
        try {
            DiffSetManager.getInstance().abort(category);
        } finally {
            Util.unlock(category, context.getUserName());
        }
    }

}