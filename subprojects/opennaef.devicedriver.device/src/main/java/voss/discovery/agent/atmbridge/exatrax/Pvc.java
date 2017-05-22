package voss.discovery.agent.atmbridge.exatrax;

public class Pvc {
    int slot = -1;
    int port = -1;
    public int vpi = -1;
    public int vci = -1;
    int pcr = -1;
    int adminState = -1;
    int mcr = -1;
    int associatePort = -1;
    String associatePortName = null;
    int bridgeOperState = -1;
    int bridgeAdminState = -1;
    int overSub = -1;

    Pvc(int slot, int port, int vpi, int vci) {
        this.slot = slot;
        this.port = port;
        this.vpi = vpi;
        this.vci = vci;
    }
}