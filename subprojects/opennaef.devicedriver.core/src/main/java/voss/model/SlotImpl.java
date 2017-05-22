package voss.model;

public class SlotImpl extends AbstractVlanModel implements Slot {
    private static final long serialVersionUID = 1L;

    private Container container;
    private Integer slotIndex_;
    private String slotId_;
    private Module module_;
    private String name;

    public SlotImpl() {
    }

    public synchronized Device getDevice() {
        if (this.container == null) {
            return null;
        }
        return container.getDevice();
    }

    public synchronized void initContainer(Container container) {
        if (container == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (this.container == container) {
            return;
        }
        if (this.container != null) {
            throw new AlreadyInitializedException(this.container, container);
        }

        this.container = container;
        this.container.addSlot(this);
    }

    public synchronized Container getContainer() {
        return this.container;
    }

    public synchronized int getSlotIndex() throws NotInitializedException {
        if (slotIndex_ == null) {
            throw new NotInitializedException();
        }

        return slotIndex_.intValue();
    }

    public synchronized void initSlotIndex(int slotIndex) {
        if (slotIndex_ != null && slotIndex_.intValue() == slotIndex) {
            return;
        }
        if (slotIndex_ != null) {
            throw new AlreadyInitializedException(slotIndex_, new Integer(
                    slotIndex));
        }
        if (container != null) {
            for (Slot slot : container.getSlots()) {
                if (slot == this) {
                    continue;
                }
                try {
                    int slotIndex_ = slot.getSlotIndex();
                    if (slotIndex_ == slotIndex) {
                        throw new AlreadyInitializedException(slot, this);
                    }
                } catch (NotInitializedException e) {
                }
            }
        }
        slotIndex_ = new Integer(slotIndex);
    }

    public synchronized String getSlotId() {
        return slotId_ == null ? Integer.toString(getSlotIndex()) : slotId_;
    }

    public synchronized void initSlotId(String slotId) {
        if (slotId_ != null && slotId_.equals(slotId)) {
            return;
        }
        if (slotId_ != null) {
            throw new AlreadyInitializedException(slotId_, slotId);
        }

        slotId_ = slotId;
    }

    public synchronized Module getModule() {
        return module_;
    }

    public synchronized void setModule(Module module) {
        if (module_ == module) {
            return;
        }

        module_ = module;
        module_.initSlot(this);
    }

    public boolean hasModule() {
        return getModule() != null;
    }

    public String getSlotName() {
        return this.name;
    }

    public void setSlotName(String name) {
        this.name = name;
    }
}