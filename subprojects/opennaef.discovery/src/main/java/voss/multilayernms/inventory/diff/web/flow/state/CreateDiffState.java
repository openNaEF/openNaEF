package voss.multilayernms.inventory.diff.web.flow.state;

import voss.multilayernms.inventory.diff.service.DiffSetManager;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.TaskException;

import javax.servlet.ServletException;
import java.io.IOException;

public class CreateDiffState extends AbstractState {

    private DiffCategory category;

    public CreateDiffState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        category = getDiffCategoryFromParameter(context);
        try {
            DiffSetManager.getInstance().start(category);
        } catch (TaskException e) {
            log.error("", e);
            throw new ServletException(e);
        }
    }

}