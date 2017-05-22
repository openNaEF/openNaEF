package voss.model;

public abstract class ESConverter extends AbstractDevice {
    private static final long serialVersionUID = 1L;

    public ESConverter() {
    }

    public EthernetStmBridge[] getEthernetStmBridges() {
        return selectPorts(EthernetStmBridge.class);
    }

    public synchronized EthernetStmBridge getEthernetStmBridge(StmLogicalPort stmLogicalPort) {
        EthernetStmBridge result = null;
        for (EthernetStmBridge bridge : getEthernetStmBridges()) {
            if (bridge.getStmLogicalPort() != stmLogicalPort) {
                continue;
            }
            if (result != null) {
                throw new IllegalStateException("Duplicate e/s-bridge exists for stm-logicalport: "
                        + result.getFullyQualifiedName() + ", " + bridge.getFullyQualifiedName());
            }
            result = bridge;
        }
        return result;
    }

    public synchronized EthernetStmBridge getEthernetStmBridge(EthernetPort ethernetPort) {
        EthernetStmBridge result = null;
        for (EthernetStmBridge bridge : getEthernetStmBridges()) {
            if (bridge.getEthernetPort() != ethernetPort) {
                continue;
            }
            if (result != null) {
                throw new IllegalStateException("Duplicate e/s-bridge exists for ethernet-port: "
                        + result.getFullyQualifiedName()
                        + ", " + bridge.getFullyQualifiedName());
            }
            result = bridge;
        }
        return result;
    }

    public synchronized StmLogicalPort getStmLogicalPort(EthernetPort ethport) {
        EthernetStmBridge bridge = getEthernetStmBridge(ethport);
        return bridge == null ? null : bridge.getStmLogicalPort();
    }

    public synchronized EthernetPort getEthernetPort(StmLogicalPort stmlogicalport) {
        EthernetStmBridge bridge = getEthernetStmBridge(stmlogicalport);
        return bridge == null ? null : bridge.getEthernetPort();
    }
}