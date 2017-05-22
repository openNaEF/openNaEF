package voss.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.core.server.builder.Commands;
import voss.core.server.database.ShellConnector;
import voss.nms.inventory.diff.DiffCategory;
import voss.nms.inventory.diff.DiffSet;
import voss.nms.inventory.diff.DiffSetManagerImpl;
import voss.nms.inventory.diff.DiffUnit;
import voss.nms.inventory.diff.network.NetworkDiffThread;

import java.util.ArrayList;
import java.util.List;

public class DiffTest {

    public static void main(String[] args) {
        try {
            Logger log = LoggerFactory.getLogger(DiffTest.class);
            List<String> targets = new ArrayList<String>();
            DiffSetManagerImpl manager = DiffSetManagerImpl.getInstance();
            NetworkDiffThread th = new NetworkDiffThread(manager, DiffCategory.DISCOVERY);
            th.setDaemon(true);
            th.setTargetNodes(targets);
            th.start();
            th.join();

            DiffSet set = manager.getDiffSet(DiffCategory.DISCOVERY);
            List<DiffUnit> units = set.getDiffUnits();
            for (DiffUnit unit : units) {
                log.debug(unit.getInventoryID() + ":" + unit.getDepth());
            }
            for (DiffUnit unit : units) {
                log.debug("Diff: " + unit.getInventoryID());
                log.debug("- desc: " + unit.getDiffContentDescription());
                log.debug("- type: " + unit.getTypeName());
                for (Commands cmd : unit.getShellCommands()) {
                    try {
                        ShellConnector.getInstance().execute2(cmd);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}