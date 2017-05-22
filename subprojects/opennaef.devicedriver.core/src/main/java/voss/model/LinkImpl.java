package voss.model;

public class LinkImpl extends AbstractVlanModel implements Link {
    private static final long serialVersionUID = 1L;
    private PhysicalPort port1_;
    private PhysicalPort port2_;

    public LinkImpl() {
    }

    public synchronized PhysicalPort getPort1() {
        if (port1_ == null) {
            throw new NotInitializedException();
        }

        return port1_;
    }

    public synchronized PhysicalPort getPort2() {
        if (port2_ == null) {
            throw new NotInitializedException();
        }

        return port2_;
    }

    public synchronized void initPorts(PhysicalPort port1, PhysicalPort port2) {
        if (port1 == null || port2 == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (port1_ != null) {
            throw new AlreadyInitializedException(port1_, port1);
        }
        if (port2_ != null) {
            throw new AlreadyInitializedException(port2_, port2);
        }

        port1_ = port1;
        port1_.setLink(LinkImpl.this);

        port2_ = port2;
        port2_.setLink(LinkImpl.this);
    }
}