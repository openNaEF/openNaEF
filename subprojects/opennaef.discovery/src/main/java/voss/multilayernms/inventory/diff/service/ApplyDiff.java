package voss.multilayernms.inventory.diff.service;

import net.phalanx.compare.core.DiffItem;
import net.phalanx.compare.core.IDiffItem.CompareStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.Commands;
import voss.core.server.database.ShellConnector;
import voss.multilayernms.inventory.diff.util.Util;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffStatus;
import voss.nms.inventory.diff.DiffUnit;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

public class ApplyDiff {

    private static final Logger log = LoggerFactory.getLogger(ApplyDiff.class);
    private final DiffSet set;

    public ApplyDiff(DiffSet set) {
        this.set = set;
    }

    public ArrayList<DiffItem> doApply(Map<String, DiffItem> applyMap) throws IOException, ServletException {
        if (!hasAllInventoryId(set.getDiffUnits(), applyMap.keySet())) {
            throw new ServletException("Diff set is already old.");
        }
        List<DiffUnit> list = set.getDiffUnits();
        Collections.sort(list);
        for (DiffUnit unit : list) {
            DiffItem item = applyMap.get(unit.getInventoryID());
            if (item == null)
                continue;
            apply(unit, item);
        }
        Util.saveDiffSet(set);
        return new ArrayList<DiffItem>(applyMap.values());
    }

    public void applyAll() throws IOException {
        List<DiffUnit> list = set.getDiffUnits();
        Collections.sort(list);
        for (DiffUnit unit : list) {
            apply(unit, null);
        }
        Util.saveDiffSet(set);
    }

    private void apply(DiffUnit unit, DiffItem item) {
        try {
            DiffCategory category = Enum.valueOf(DiffCategory.class, unit.getSourceSystem());
            if (category != DiffCategory.INVENTORY) {
                Commands[] commands = unit.getShellCommands();
                if (commands != null && commands.length > 0) {
                    ShellConnector.getInstance().executes2(commands);
                }
            }
            unit.setStatus(DiffStatus.APPLIED);
            unit.setResultDescription("");
            if (item != null) {
                item.setStatus(CompareStatus.APPLIED);
                item.setResult("");
            }
        } catch (Exception e) {
            log.error("", e);
            String msg = getCause(e).getMessage();
            unit.setStatus(DiffStatus.FAIL);
            unit.setResultDescription(msg);
            if (item != null) {
                item.setStatus(CompareStatus.FAIL);
                item.setResult(msg);
            }
        }
    }

    private boolean hasAllInventoryId(List<DiffUnit> unitList, Set<String> itemIds) {
        List<String> unitIds = new ArrayList<String>();
        for (DiffUnit unit : unitList) {
            unitIds.add(unit.getInventoryID());
        }
        return unitIds.containsAll(itemIds);
    }

    private Exception getCause(Exception e) {
        while (e.getCause() != null) {
            e = (Exception) e.getCause();
        }
        return e;
    }

}