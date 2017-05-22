package voss.model;

public class StmLogicalPort extends AbstractLogicalPort {
    private static final long serialVersionUID = 1L;
    private StmPhysicalPort physicalPort_;

    public StmLogicalPort(StmPhysicalPort physicalPort, String ifName) {
        initDevice(physicalPort.getDevice());
        initIfName(ifName);

        physicalPort_ = physicalPort;
    }

    public synchronized StmPhysicalPort getPhysicalPort() {
        return physicalPort_;
    }
}