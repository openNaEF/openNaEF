package voss.model;


public class VirtualServerDevice extends AbstractDevice implements ServerDevice {
    private static final long serialVersionUID = 1L;

    @Override
    public void addVirtualDevice(Device device) {
        throw new IllegalStateException("This device isn't be physical-device.");
    }

}