package voss.model;

@SuppressWarnings("serial")
public class SiiExatrax extends EAConverter {

    public static final String VENDOR_NAME = "SII";

    public static boolean isAdaptive
            (String vendorName, String modelTypeName, String osVersion) {
        return (vendorName != null && vendorName.equals(VENDOR_NAME))
                ;
    }

    public SiiExatrax() {
    }
}