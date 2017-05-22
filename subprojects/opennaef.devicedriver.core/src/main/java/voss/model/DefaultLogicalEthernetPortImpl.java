package voss.model;

@SuppressWarnings("serial")
public class DefaultLogicalEthernetPortImpl extends AbstractLogicalEthernetPort implements DefaultLogicalEthernetPort {
    private EthernetPort physicalPort_;

    public DefaultLogicalEthernetPortImpl() {
    }

    public boolean isAggregated() {
        return false;
    }

    public synchronized EthernetPort getPhysicalPort() {
        return physicalPort_;
    }

    public synchronized void initPhysicalPort(EthernetPort physicalPort) {
        if (physicalPort == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (physicalPort_ == physicalPort) {
            return;
        }
        if (physicalPort_ != null) {
            throw new AlreadyInitializedException(physicalPort_, physicalPort);
        }
        if (physicalPort.getDevice() != getDevice()) {
            throw new IllegalArgumentException("Device does not match.");
        }
        if (AbstractLogicalEthernetPort.getLogicalEthernetPort(physicalPort) != null
                && AbstractLogicalEthernetPort.getLogicalEthernetPort(physicalPort) != this) {
            throw new AlreadyInitializedException(AbstractLogicalEthernetPort.getLogicalEthernetPort(physicalPort), this);
        }

        physicalPort_ = physicalPort;
    }

    public synchronized EthernetPort[] getPhysicalPorts() {
        return new EthernetPort[]{physicalPort_};
    }

    public synchronized boolean isDefaultLogicalEthernetPortOf(EthernetPort physicalPort) {
        return physicalPort_ == physicalPort;
    }
}