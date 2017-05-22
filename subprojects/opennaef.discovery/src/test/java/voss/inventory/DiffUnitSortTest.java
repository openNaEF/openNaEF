package voss.inventory;

import voss.nms.inventory.diff.DiffOperationType;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffUnit;

public class DiffUnitSortTest {
    public static void main(String[] args) {
        try {
            DiffUnit unit1 = create("a", DiffOperationType.ADD, 5);
            DiffUnit unit2 = create("b", DiffOperationType.REMOVE, 4);
            DiffUnit unit3 = create("c", DiffOperationType.UPDATE, 3);
            DiffUnit unit4 = create("d", DiffOperationType.REMOVE, 2);
            DiffUnit unit5 = create("e", DiffOperationType.ADD, 1);
            DiffUnit unit6 = create("!", DiffOperationType.ADD, 2);
            DiffUnit unit7 = create("z", DiffOperationType.REMOVE, 3);
            DiffUnit unit8 = create("y", DiffOperationType.UPDATE, 4);
            DiffUnit unit9 = create("x", DiffOperationType.REMOVE, 5);
            DiffUnit unit10 = create("w", DiffOperationType.ADD, 6);
            DiffSet set = new DiffSet("test");
            set.addDiffUnit(unit1);
            set.addDiffUnit(unit2);
            set.addDiffUnit(unit3);
            set.addDiffUnit(unit4);
            set.addDiffUnit(unit5);
            set.addDiffUnit(unit6);
            set.addDiffUnit(unit7);
            set.addDiffUnit(unit8);
            set.addDiffUnit(unit9);
            set.addDiffUnit(unit10);
            set.sortDiffUnit();
            for (DiffUnit unit : set.getDiffUnits()) {
                print(unit);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void print(DiffUnit unit) {
        System.out.println(unit.getInventoryID() + ":" + unit.getOpType().name() + ":" + unit.getDepth());
    }

    private static DiffUnit create(String id, DiffOperationType type, int depth) {
        DiffUnit unit = new DiffUnit(id, type);
        unit.setDepth(depth);
        return unit;
    }
}