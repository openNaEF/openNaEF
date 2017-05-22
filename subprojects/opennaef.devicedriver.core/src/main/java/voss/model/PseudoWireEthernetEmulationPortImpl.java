package voss.model;


public class PseudoWireEthernetEmulationPortImpl extends
        AbstractLogicalEthernetPort implements PseudoWireEthernetEmulationPort {

    private static final long serialVersionUID = 1L;
    private PseudoWirePort pw;
    private EthernetPort ethernet;

    public PseudoWirePort getPseudWirePort() {
        return this.pw;
    }

    public void initPseudoWirePort(PseudoWirePort pw) {
        if (this.pw != null) {
            throw new AlreadyInitializedException(this.pw, pw);
        }
        this.pw = pw;
    }


    public EthernetPort[] getPhysicalPorts() throws NotInitializedException {
        if (this.ethernet != null) {
            return new EthernetPort[]{ethernet};
        }
        return new EthernetPort[0];
    }

    public void initPhysicalPort(EthernetPort port) throws AlreadyInitializedException {
        if (this.ethernet != null) {
            throw new AlreadyInitializedException(this.ethernet, port);
        }
        this.ethernet = port;
    }

    public boolean isAggregated() {
        return false;
    }

}