package voss.multilayernms.inventory.web.util;

public class MiscUtil {

    public static String getNodePart(String withNodeName) {
        int index = withNodeName.indexOf(':');
        if (index > -1) {
            return withNodeName.substring(0, index);
        } else {
            return withNodeName;
        }
    }

    public static String getObjectPart(String withNodeName) {
        int index = withNodeName.indexOf(':');
        if (index > -1 && index < withNodeName.length()) {
            return withNodeName.substring(index + 1);
        } else {
            return withNodeName;
        }
    }

}