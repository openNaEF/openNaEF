package voss.discovery.agent.atmbridge.exatrax;

class EtherPort {
    int port = -1;
    int gbic = -1;
    int autoNego = -1;
    int adminSpeed = -1;
    int adminDuplex = -1;
    int operState = -1;
    int operSpeed = -1;
    int operDuplex = -1;
    String portType = null;
    int presence = -1;

    EtherPort(int port) {
        this.port = port;
    }
}