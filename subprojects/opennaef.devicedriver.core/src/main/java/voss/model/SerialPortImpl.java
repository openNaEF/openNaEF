package voss.model;


public class SerialPortImpl extends AbstractPhysicalPort implements SerialPort, PhysicalPort {
    private static final long serialVersionUID = 1L;
    private Feature feature;

    private boolean isAsyncMode = false;

    public boolean isAsyncMode() {
        return this.isAsyncMode;
    }

    public void setAsyncMode(boolean async) {
        this.isAsyncMode = async;
    }

    public void setLogicalFeature(Feature logical) {
        this.feature = logical;
        this.feature.setParentPort(this);
    }

    public Feature getLogicalFeature() {
        return this.feature;
    }

}