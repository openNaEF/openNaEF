package voss.model;

public interface Container extends VlanModel {

    public Device getDevice();

    public String getModelTypeName();

    public void setModelTypeName(String modelTypeName);

    public Port[] getPorts();

    public Port[] getRawPorts();

    public PhysicalPort[] getPhysicalPorts();

    public LogicalPort[] getLogicalPorts();

    public Port getPortByIfIndex(int ifIndex);

    public Port getPortByIfName(String ifName);

    public void addPort(Port port);

    public void updatePort(Port port);

    public void removePort(Port port);

    public Slot[] getSlots();

    public Slot getSlotBySlotId(String slotId);

    public Slot getSlotBySlotIndex(int slotIndex);

    public void addSlot(Slot slot);

    public void setSerialNumber(String serial);

    public String getSerialNumber();

    public <T extends Port> T[] selectPorts(Class<T> type);
}