package voss.model;

import java.util.ArrayList;
import java.util.List;


public class EthernetProtectionPortImpl extends AbstractLogicalEthernetPort implements EthernetProtectionPort {
    private static final long serialVersionUID = 1L;
    private EthernetPort workingPort = null;
    private final List<EthernetPort> memberPorts = new ArrayList<EthernetPort>();

    @Override
    public boolean isAggregated() {
        return true;
    }

    public void addPhysicalPort(EthernetPort eth) {
        if (eth == null) {
            throw new IllegalArgumentException();
        }
        this.memberPorts.add(eth);
    }

    @Override
    public EthernetPort[] getPhysicalPorts() throws NotInitializedException {
        return memberPorts.toArray(new EthernetPort[0]);
    }

    public void resetPhysicalPorts() {
        this.memberPorts.clear();
    }

    @Override
    public EthernetPort getWorkingPort() {
        return this.workingPort;
    }

    @Override
    public void setWorkingPort(EthernetPort port) {
        this.workingPort = port;
    }

}