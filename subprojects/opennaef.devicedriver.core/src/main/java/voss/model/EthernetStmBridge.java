package voss.model;

public class EthernetStmBridge extends AbstractLogicalPort {
    private static final long serialVersionUID = 1L;
    private final EthernetPort ethernetPort_;
    private final StmLogicalPort stmLogicalPort_;

    public EthernetStmBridge(EthernetPort ethernetPort, StmLogicalPort stmLogicalPort, String ifname) {
        if (ethernetPort == null || stmLogicalPort == null || ifname == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (ethernetPort.getDevice() != stmLogicalPort.getDevice()) {
            throw new IllegalArgumentException(
                    "The two ports connecting to the bridge must be ports of the same device: "
                            + ethernetPort.getFullyQualifiedName()
                            + ", " + stmLogicalPort.getFullyQualifiedName());
        }

        initDevice(stmLogicalPort.getDevice());
        initIfName(ifname);

        if (getDevice().getEthernetStmBridge(stmLogicalPort) != null
                || getDevice().getEthernetStmBridge(ethernetPort) != null) {
            throw new IllegalStateException("Duplicate E/S bridge definition already exists.");
        }
        ethernetPort_ = ethernetPort;
        stmLogicalPort_ = stmLogicalPort;
    }

    @Override
    public ESConverter getDevice() {
        return (ESConverter) super.getDevice();
    }

    public synchronized EthernetPort getEthernetPort() {
        return ethernetPort_;
    }

    public synchronized StmLogicalPort getStmLogicalPort() {
        return stmLogicalPort_;
    }
}