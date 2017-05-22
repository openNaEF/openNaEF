package voss.multilayernms.inventory.diff.web.flow.state;

import net.phalanx.compare.core.DiffItem;
import net.phalanx.compare.core.IDiffItem.CompareStatus;
import voss.multilayernms.inventory.diff.util.Util;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffStatus;
import voss.nms.inventory.diff.DiffUnit;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class IgnoreDiffState extends AbstractState {

    private DiffCategory category;

    public IgnoreDiffState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        category = getDiffCategoryFromParameter(context);
        hasLock(category, context.getUserName());
        Map<String, DiffItem> map = getInventoryIdItemMap(context);
        DiffSet set = Util.loadDiffSet(category);
        if (!hasAllInventoryId(set.getDiffUnits(), map.keySet())) {
            throw new ServletException("Diff set is already old.");
        }
        for (DiffUnit unit : set.getDiffUnits()) {
            DiffItem item = map.get(unit.getInventoryID());
            if (item == null) continue;
            unit.setStatus(DiffStatus.IGNORED);
            unit.setResultDescription("");
            item.setStatus(CompareStatus.IGNORED);
            item.setResult("");
            log.debug("InventoryID[" + unit.getInventoryID() + "] is ignored.");
        }
        Util.saveDiffSet(set);
        toXMLResponse(context, new ArrayList<DiffItem>(map.values()));
    }

}