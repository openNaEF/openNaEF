package voss.model;


@SuppressWarnings("serial")
public class ModuleImpl extends AbstractContainerImpl implements Module {

    private Slot slot_;
    private String modelTypeName_;
    private String serialNumber_;
    private String hardwareRevision_;
    public ModuleImpl() {
    }

    public synchronized Device getDevice() {
        return this.slot_.getDevice();
    }

    public synchronized void initSlot(Slot slot) {
        if (slot == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (slot_ == slot) {
            return;
        }
        if (slot_ != null) {
            throw new AlreadyInitializedException(slot_, slot);
        }
        if (slot.getModule() != null && slot.getModule() != this) {
            throw new IllegalStateException(slot.getDevice() + "."
                    + slot.getSlotId() + " already has another module set.");
        }

        slot_ = slot;
        slot_.setModule(this);
    }

    public synchronized Slot getSlot() {
        return slot_;
    }

    public synchronized String getModelTypeName() {
        return modelTypeName_;
    }

    public synchronized void setModelTypeName(String modelTypeName) {
        modelTypeName_ = modelTypeName;
    }

    public synchronized String getSerialNumber() {
        return serialNumber_;
    }

    public synchronized void setSerialNumber(String serialNumber) {
        serialNumber_ = serialNumber;
    }

    public synchronized String getHardwareRevision() {
        return this.hardwareRevision_;
    }

    public synchronized void setHardwareRevision(String rev) {
        this.hardwareRevision_ = rev;
    }

    public synchronized void addPort(PhysicalPort port) {
        if (port == null) {
            throw new IllegalStateException();
        }
        super.addPort(port);
    }

    public synchronized PhysicalPort[] getPorts() {
        return super.getPhysicalPorts();
    }

    public synchronized String getContainerName() {
        return this.getDevice() + ":" + getSlot().getSlotId() + ":" + this.modelTypeName_;
    }
}