package voss.model;


@SuppressWarnings("serial")
public class FrameRelayDLCIEndPointImpl extends AbstractLogicalPort implements
        FrameRelayDLCIEndPoint {
    private FrameRelayFeature parent;
    private int dlci = -1;

    @Override
    public String getIfName() {
        if (this.parent == null || this.dlci == -1) {
            throw new NotInitializedException();
        }
        String ifName = this.parent.getIfName();
        if (ifName == null) {
            if (parent.getParentPort() != null) {
                ifName = parent.getParentPort().getIfName();
            } else {
                throw new NotInitializedException();
            }
        }
        return ifName + ":" + this.dlci;
    }

    public void setDLCI(int dlci) {
        this.dlci = dlci;
    }

    public int getDLCI() {
        return this.dlci;
    }

    public FrameRelayFeature getParentPort() {
        return parent;
    }

    public void initParentPort(FrameRelayFeature parent) {
        if (this.parent != null) {
            throw new AlreadyInitializedException(this.parent, parent);
        }
        this.parent = parent;
    }

}