package voss.multilayernms.inventory.nmscore.rendering;

public class RenderingUtil {

    protected static String convertNull2ZeroString(String value) {
        if (value == null) {
            return "";
        } else {
            return value;
        }
    }

}