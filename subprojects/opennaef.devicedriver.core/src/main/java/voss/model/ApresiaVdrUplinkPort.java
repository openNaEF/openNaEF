package voss.model;


public class ApresiaVdrUplinkPort extends AbstractLogicalPort implements LogicalPort {
    private static final long serialVersionUID = 1L;
    private ApresiaVdr vdr;
    private LogicalEthernetPort uplink;

    public void initVdr(ApresiaVdr vdr) throws NotInitializedException {
        if (this.vdr != null) {
            throw new AlreadyInitializedException(this.vdr, vdr);
        }
        this.vdr = vdr;
    }

    public ApresiaVdr getVdr() {
        return this.vdr;
    }

    public void initLogicalEthernetPort(LogicalEthernetPort uplink) throws NotInitializedException {
        if (this.uplink != null) {
            throw new AlreadyInitializedException(this.uplink, uplink);
        }
        this.uplink = uplink;
    }

    public LogicalEthernetPort getUplinkLogicalEthernetPort() {
        return this.uplink;
    }
}