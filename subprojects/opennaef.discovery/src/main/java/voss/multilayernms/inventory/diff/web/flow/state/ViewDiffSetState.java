package voss.multilayernms.inventory.diff.web.flow.state;

import net.phalanx.compare.core.DiffDelta;
import net.phalanx.compare.core.DiffItem;
import net.phalanx.compare.core.IDiffDelta;
import net.phalanx.compare.core.IDiffItem;
import net.phalanx.compare.core.IDiffItem.CompareStatus;
import net.phalanx.compare.core.IDiffItem.CompareType;
import net.phalanx.compare.core.IDiffItem.DataSource;
import voss.multilayernms.inventory.config.MplsNmsDiffConfiguration;
import voss.multilayernms.inventory.config.MplsNmsDiffConfiguration.FromToValueHolder;
import voss.multilayernms.inventory.diff.util.Util;
import voss.multilayernms.inventory.diff.web.flow.FlowContext;
import voss.nms.inventory.diff.*;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

public class ViewDiffSetState extends AbstractState {

    public ViewDiffSetState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException,
            IOException {
        DiffCategory category = getDiffCategoryFromParameter(context);
        log.debug("category[" + category + "]");
        DiffSet set = Util.loadDiffSet(category);
        toXMLResponse(context, createDiffItemList(set));
    }

    private List<IDiffItem> createDiffItemList(DiffSet set) {
        List<IDiffItem> result = new ArrayList<IDiffItem>();
        if (set == null) return result;
        DiffUnit[] unitArray = set.getDiffUnits().toArray(new DiffUnit[set.getDiffUnits().size()]);
        Arrays.sort(unitArray);
        for (DiffUnit unit : unitArray) {
            if (unit.getStatus() == DiffStatus.APPLIED ||
                    unit.getStatus() == DiffStatus.IGNORED ||
                    unit.getOpType() == DiffOperationType.INFORMATION) continue;
            DiffItem item = new DiffItem();
            item.setDelta(createDiffDeltaArray(unit.getDiffs()));
            item.setDataSource(Enum.valueOf(DataSource.class, unit.getSourceSystem()));
            item.setId(UUID.randomUUID().toString());
            item.setInventoryId(unit.getInventoryID());
            item.setResult(unit.getResultDescription());
            item.setStatus(Enum.valueOf(CompareStatus.class, unit.getStatus().name()));
            item.setTarget(unit.getTypeName());
            item.setType(Enum.valueOf(CompareType.class, unit.getOpType().name()));
            result.add(item);
        }
        return result;
    }

    private IDiffDelta[] createDiffDeltaArray(Set<Diff> diffs) {
        ArrayList<DiffDelta> list = new ArrayList<DiffDelta>();
        for (Diff diff : diffs) {
            DiffDelta delta = new DiffDelta();
            delta.setName(getDiffAttributeName(diff.getAttributeName()));
            delta.setBefore(nullToString(diff.getCurrentValue()));
            delta.setAfter(nullToString(diff.getChangedValue()));
            list.add(delta);
        }
        return (IDiffDelta[]) list.toArray(new IDiffDelta[list.size()]);
    }

    private String getDiffAttributeName(String attrName) {
        try {
            for (FromToValueHolder holder : MplsNmsDiffConfiguration.getInstance().getValueMappings()) {
                if (holder.from.equals(attrName)) {
                    return holder.to;
                }
            }
        } catch (IOException e) {
            log.error("", e);
        }
        return attrName;
    }

    private String nullToString(String src) {
        if (src == null) return "";
        return src;
    }

}