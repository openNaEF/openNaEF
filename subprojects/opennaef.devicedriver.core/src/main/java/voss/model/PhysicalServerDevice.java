package voss.model;


public class PhysicalServerDevice extends AbstractVlanDevice implements ServerDevice {
    private static final long serialVersionUID = 1L;

    @Override
    public void setPhysicalDevice(Device device) {
        throw new IllegalStateException("This device isn't be virtual-device.");
    }

}