package voss.model;

public interface Module extends Container {

    public Slot getSlot();

    public void initSlot(Slot slot);

    public String getHardwareRevision();

    public void setHardwareRevision(String rev);

    public void addPort(PhysicalPort port);

}