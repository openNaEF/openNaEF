package voss.nms.inventory.util;

import naef.dto.mpls.RsvpLspDto;
import naef.dto.mpls.RsvpLspIdPoolDto;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.core.server.util.ExceptionUtils;

public class RsvpLspUtil extends voss.core.server.util.RsvpLspUtil {

    public static RsvpLspDto getRsvpLspByInventoryID(RsvpLspIdPoolDto pool, String inventoryID) {
        try {
            for (RsvpLspDto rsvpLsp : pool.getUsers()) {
                String id = InventoryIdCalculator.getId(rsvpLsp);
                if (id.equals(inventoryID)) {
                    return rsvpLsp;
                }
            }
            return null;
        } catch (Exception e) {
            throw ExceptionUtils.throwAsRuntime(e);
        }
    }

    public static boolean hasPath(RsvpLspDto lsp, String pathName) {
        if (lsp.getHopSeries1() != null && lsp.getHopSeries1().getName().equals(pathName)) {
            return true;
        } else if (lsp.getHopSeries2() != null && lsp.getHopSeries2().getName().equals(pathName)) {
            return true;
        }
        return false;
    }
}