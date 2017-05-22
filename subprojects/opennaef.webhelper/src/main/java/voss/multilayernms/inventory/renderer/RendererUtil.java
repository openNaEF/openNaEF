package voss.multilayernms.inventory.renderer;


public class RendererUtil {

    public static Long getGuaranteedBandwidth(Long total, Double coefficient) {
        if (total == null) {
            return null;
        }
        if (coefficient == null) {
            coefficient = 1.0D;
        }
        double guaranteed = (double) total.longValue() * coefficient;
        guaranteed = Math.floor(guaranteed);
        return Long.valueOf((long) guaranteed);
    }
}