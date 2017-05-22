package voss.model;

public class ExtremeEthernetSwitch extends AbstractEthernetSwitch {
    private static final long serialVersionUID = 1L;

    public static final String VENDOR_NAME = "Extreme";

    public static boolean isAdaptive(String vendorName, String modelTypeName, String osVersion) {
        return (vendorName != null && vendorName.equals(VENDOR_NAME));
    }

    public ExtremeEthernetSwitch() {
        setVendorName(VENDOR_NAME);
    }
}