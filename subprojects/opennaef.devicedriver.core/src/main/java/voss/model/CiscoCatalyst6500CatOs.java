package voss.model;

public class CiscoCatalyst6500CatOs extends AbstractEthernetSwitch {
    private static final long serialVersionUID = 1L;
    public static final String VENDOR_NAME = "Cisco";
    public static final String OS_VERSION_PATTERN = "CatOS.*";

    public static boolean isAdaptive(String vendorName, String modelTypeName, String osVersion) {
        return (vendorName != null && vendorName.equals(VENDOR_NAME))
                && (osVersion != null && osVersion.matches(OS_VERSION_PATTERN));
    }

    public CiscoCatalyst6500CatOs() {
        setVendorName(VENDOR_NAME);
    }
}