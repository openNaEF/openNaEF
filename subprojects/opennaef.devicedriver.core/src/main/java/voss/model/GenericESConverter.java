package voss.model;

public class GenericESConverter extends ESConverter {
    private static final long serialVersionUID = 1L;

    public static boolean isAdaptive(String vendorName, String modelTypeName, String osVersion) {
        return true;
    }

    public GenericESConverter() {
    }
}