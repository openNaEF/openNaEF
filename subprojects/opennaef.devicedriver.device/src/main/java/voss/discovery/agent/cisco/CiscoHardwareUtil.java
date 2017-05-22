package voss.discovery.agent.cisco;

import voss.discovery.agent.common.PhysicalEntry;

public class CiscoHardwareUtil {

    public static boolean isSubSlot(PhysicalEntry pe) {
        if (pe == null) {
            throw new IllegalStateException("pe is null.");
        }
        if (pe.name == null) {
            return false;
        } else if (pe.name.equals("cevContainerDaughterCard")) {
            return true;
        } else if (pe.name.equals("cevContainerSPABay")) {
            return true;
        }
        return false;
    }

    public static boolean isPortModule(PhysicalEntry pe) {
        if (pe == null || pe.name == null) {
            return false;
        } else if (pe.name.equals("cevContainerSFP")) {
            return true;
        } else if (pe.physicalName.equals("cevSFP1000BaseSx")) {
            return true;
        }
        return false;
    }

    public static boolean isPort(PhysicalEntry pe) {
        if (pe == null) {
            throw new IllegalStateException("pe is null.");
        }
        if (pe.name == null) {
            return false;
        } else if (pe.name.equals("cevContainerFlashCardSlot")) {
            return false;
        }
        return true;
    }

}