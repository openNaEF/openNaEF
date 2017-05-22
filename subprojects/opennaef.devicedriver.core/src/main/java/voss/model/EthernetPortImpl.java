package voss.model;

@SuppressWarnings("serial")
public class EthernetPortImpl extends AbstractPhysicalPort implements
        EthernetPort {

    private String macAddress;

    public EthernetPortImpl() {
    }

    public void setDuplex(Duplex duplex) {
        addProperty(duplex);
    }

    public Duplex getDuplex() {
        return (Duplex) selectConfigProperty(Duplex.class);
    }

    public void setAutoNego(AutoNego autoNego) {
        addProperty(autoNego);
    }

    public AutoNego getAutoNego() {
        return (AutoNego) selectConfigProperty(AutoNego.class);
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}