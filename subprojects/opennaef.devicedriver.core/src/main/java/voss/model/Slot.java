package voss.model;

public interface Slot extends VlanModel {

    public Device getDevice();

    public void initContainer(Container container);

    public Container getContainer();

    public int getSlotIndex() throws NotInitializedException;

    public void initSlotIndex(int slotIndex);

    public String getSlotId();

    public void setSlotName(String name);

    public String getSlotName();

    public void initSlotId(String slotId);

    public Module getModule();

    public void setModule(Module module);

    public boolean hasModule();
}