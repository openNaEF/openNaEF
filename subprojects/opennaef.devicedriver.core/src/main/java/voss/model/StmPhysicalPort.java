package voss.model;

public class StmPhysicalPort extends AbstractPhysicalPort {
    private static final long serialVersionUID = 1L;

    public StmPhysicalPort
            (ESConverter esConverter, String ifName, String portTypeName) {
        initDevice(esConverter);
        initIfName(ifName);
        setPortTypeName(portTypeName);
    }
}