package voss.model;

public class MistpInstanceImpl extends AbstractVlanStpElement implements MistpInstance {
    private static final long serialVersionUID = 1L;
    private int instanceIndex_;

    public MistpInstanceImpl() {
    }

    public String getVlanStpElementId() {
        return Integer.toString(getInstanceIndex());
    }

    public synchronized int getInstanceIndex() {
        return instanceIndex_;
    }

    public synchronized void setInstanceIndex(int instanceIndex) {
        instanceIndex_ = instanceIndex;
    }
}