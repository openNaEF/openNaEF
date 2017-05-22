package voss.discovery.agent.atmbridge.exatrax;

class Vp {
    int slot = -1;
    int port = -1;
    int vpi = -1;
    int pcr = -1;
    int overSub = -1;

    Vp(int slot, int port, int vpi) {
        this.slot = slot;
        this.port = port;
        this.vpi = vpi;
    }
}