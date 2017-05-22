package voss.model;

@SuppressWarnings("serial")
public class NecSuperHub extends EAConverter {

    public static final String VENDOR_NAME = "NEC";

    public static boolean isAdaptive
            (String vendorName, String modelTypeName, String osVersion) {
        return (vendorName != null && vendorName.equals(VENDOR_NAME))
                ;
    }

    public NecSuperHub() {
    }
}