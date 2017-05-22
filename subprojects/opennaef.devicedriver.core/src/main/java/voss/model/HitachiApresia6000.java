package voss.model;

public class HitachiApresia6000 extends AbstractEthernetSwitch {
    private static final long serialVersionUID = 1L;

    public static final String VENDOR_NAME = "Hitachi Cable";

    public static boolean isAdaptive(String vendorName, String modelTypeName, String osVersion) {
        return vendorName != null && vendorName.equals(VENDOR_NAME)
                && modelTypeName.toLowerCase().matches("apresia ?6.*");
    }

    public HitachiApresia6000() {
        setVendorName(VENDOR_NAME);
    }
}