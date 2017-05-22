package voss.core.server.util;

import voss.core.server.exception.InventoryException;

public class InventoryUtil {

    public static void checkRange(String range, boolean allowZero) throws InventoryException {
        if (range == null) {
            throw new InventoryException("The ID range is unspecified.");
        }
        if (range.indexOf('-') == -1) {
            throw new InventoryException("\"-\" is not included in ID range.");
        }
        String[] s = range.split("-");
        if (s.length > 2) {
            throw new InventoryException("Two or more integers are specified in the ID range.");
        } else if (s.length < 2) {
            throw new InventoryException("The ID range must consist of two integers.");
        }
        try {
            long long1 = Long.parseLong(s[0]);
            long long2 = Long.parseLong(s[1]);
            if (long1 > long2) {
                throw new InventoryException("ID range must be [lower limit] <= [upper limit]. Can not be specified like 100-10.");
            }
            if (long1 < 0) {
                throw new InventoryException("ID range must be a positive integer.");
            }
            if (long1 == 0 && !allowZero) {
                throw new InventoryException("ID range can not contain 0.");
            }
        } catch (NumberFormatException e) {
            throw new InventoryException("Please specify ID range with half-width positive integers and \"-\".");
        }
    }

}