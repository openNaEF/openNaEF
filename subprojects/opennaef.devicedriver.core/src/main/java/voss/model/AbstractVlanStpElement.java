package voss.model;

public abstract class AbstractVlanStpElement extends AbstractVlanModel implements VlanStpElement {
    private static final long serialVersionUID = 1L;
    private VlanDevice device_;
    private VlanIf[] vlanIfs_ = new VlanIf[0];

    protected AbstractVlanStpElement() {
    }

    public synchronized VlanDevice getDevice() {
        return device_;
    }

    public synchronized void initDevice(VlanDevice device) {
        assert device != null;
        device_ = device;
        device_.addVlanStpElement(this);
    }

    public synchronized VlanIf[] getVlanIfs() {
        return vlanIfs_;
    }

    public synchronized void setVlanIfs(VlanIf[] vlanIfs) {
        vlanIfs_ = vlanIfs;
    }
}