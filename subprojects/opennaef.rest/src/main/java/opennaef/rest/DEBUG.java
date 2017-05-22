package opennaef.rest;

/**
 * デバッグ機能
 * <p>
 * java実行時のsystem propertyに -D_debug=DEAD_BEEF が指定されている場合にデバッグモードで実行される
 */
public class DEBUG {
    private static final String PROPERTY_NAME = "_debug";
    private static final String PROPERTY_VALUE = "DEAD_BEEF";

    private static final boolean _isEnabled;

    static {
        String value = System.getProperty(PROPERTY_NAME);
        _isEnabled = PROPERTY_VALUE.equals(value);
    }

    /**
     * @return デバッグモードで実行中の場合は true
     */
    public static boolean isEnabled() {
        return _isEnabled;
    }
}
