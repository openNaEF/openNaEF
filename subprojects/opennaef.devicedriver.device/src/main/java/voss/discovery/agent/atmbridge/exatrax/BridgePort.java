package voss.discovery.agent.atmbridge.exatrax;

class BridgePort {
    int port = -1;
    int adminState = -1;
    int operState = -1;
    String name = null;
    int pvid = -1;

    BridgePort(int port) {
        this.port = port;
    }
}