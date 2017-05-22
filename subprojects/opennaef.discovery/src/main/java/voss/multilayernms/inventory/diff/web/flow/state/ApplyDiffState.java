package voss.multilayernms.inventory.diff.web.flow.state;

import net.phalanx.compare.core.DiffItem;
import voss.multilayernms.inventory.diff.service.ApplyDiff;
import voss.multilayernms.inventory.diff.util.Util;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffSet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

public class ApplyDiffState extends AbstractState {

    private DiffCategory category;

    public ApplyDiffState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        category = getDiffCategoryFromParameter(context);
        hasLock(category, context.getUserName());
        Map<String, DiffItem> map = getInventoryIdItemMap(context);
        DiffSet set = Util.loadDiffSet(category);
        ApplyDiff apply = new ApplyDiff(set);
        toXMLResponse(context, apply.doApply(map));
    }
}