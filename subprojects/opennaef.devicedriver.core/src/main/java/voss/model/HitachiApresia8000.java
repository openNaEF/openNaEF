package voss.model;

import java.util.Arrays;

public class HitachiApresia8000 extends AbstractEthernetSwitch {
    private static final long serialVersionUID = 1L;
    public static final String VENDOR_NAME = "Hitachi Cable";

    public static interface ExtInfoKeys {

        public static interface LogicalEthernetPort {
            public static final String INGRESS_LIMITING_MAX_BURST_SIZE = "ingress-limiting-max-burst-size";
        }
    }

    public static class RedundancyRegionDeviceLocalInstance extends AbstractVlanStpElement implements VlanStpElement {
        private static final long serialVersionUID = 1L;
        private String regionId_;
        private String[] vdrConfigPortIfnames_;

        public RedundancyRegionDeviceLocalInstance() {
        }

        public RedundancyRegionDeviceLocalInstance(String regionId, String[] vdrConfigPortIfnames) {
            if (regionId == null || vdrConfigPortIfnames == null) {
                throw new NullArgumentIsNotAllowedException();
            }

            regionId_ = regionId;
            vdrConfigPortIfnames_ = vdrConfigPortIfnames;
        }

        public String getVlanStpElementId() {
            return getRegionId();
        }

        public synchronized String getRegionId() {
            return regionId_;
        }

        public synchronized String[] getVdrConfigPortIfnames() {
            return vdrConfigPortIfnames_;
        }
    }

    public static boolean isAdaptive(String vendorName, String modelTypeName, String osVersion) {
        return vendorName != null && vendorName.equals(VENDOR_NAME)
                && modelTypeName.toLowerCase().matches("apresia ?8.*");
    }

    private RedundancyRegionDeviceLocalInstance[] redundancyRegionDeviceLocalInstances_;

    public HitachiApresia8000() {
        setVendorName(VENDOR_NAME);
    }

    public synchronized void setRedundancyRegionDeviceLocalInstances(
            RedundancyRegionDeviceLocalInstance[] redundancyRegionDeviceLocalInstances) {
        if (redundancyRegionDeviceLocalInstances == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        redundancyRegionDeviceLocalInstances_ = redundancyRegionDeviceLocalInstances;
    }

    public synchronized RedundancyRegionDeviceLocalInstance[] getRedundancyRegionDeviceLocalInstances() {
        return redundancyRegionDeviceLocalInstances_;
    }

    public boolean isVdrConfigPort(String redundancyRegionId, LogicalEthernetPort logicalEth) {
        if (logicalEth.getDevice() != this) {
            throw new IllegalArgumentException("Device does not match.");
        }
        RedundancyRegionDeviceLocalInstance deviceLocalInstance = getRedundancyRegionDeviceLocalInstance(redundancyRegionId);
        if (deviceLocalInstance == null) {
            return false;
        }
        return Arrays.asList(deviceLocalInstance.getVdrConfigPortIfnames()).contains(logicalEth.getIfName());
    }

    private synchronized RedundancyRegionDeviceLocalInstance getRedundancyRegionDeviceLocalInstance(String redundancyRegionId) {
        for (int i = 0; i < redundancyRegionDeviceLocalInstances_.length; i++) {
            if (redundancyRegionDeviceLocalInstances_[i].getRegionId().equals(redundancyRegionId)) {
                return redundancyRegionDeviceLocalInstances_[i];
            }
        }
        return null;
    }
}