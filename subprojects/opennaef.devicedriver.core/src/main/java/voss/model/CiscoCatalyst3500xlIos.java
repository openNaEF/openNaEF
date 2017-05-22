package voss.model;

public class CiscoCatalyst3500xlIos extends AbstractEthernetSwitch {
    private static final long serialVersionUID = 1L;

    static final String VENDOR_NAME = "Cisco";
    static final String MODEL_TYPE_NAME_PATTERN = "Catalyst.*35..XL";
    static final String OS_VERSION_PATTERN = "IOS.*";

    public static boolean isAdaptive(String vendorName, String modelTypeName, String osVersion) {
        return (vendorName != null && vendorName.equals(VENDOR_NAME))
                && (modelTypeName != null && modelTypeName.matches(MODEL_TYPE_NAME_PATTERN))
                && (osVersion != null && osVersion.matches(OS_VERSION_PATTERN));
    }

    public CiscoCatalyst3500xlIos() {
        setVendorName(VENDOR_NAME);
    }
}