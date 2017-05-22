package voss.model;


import java.util.ArrayList;
import java.util.List;

public class VMwareServerImpl extends PhysicalServerDevice implements VMwareServer {
    private static final long serialVersionUID = 1L;
    private final List<EthernetSwitch> vSwitches = new ArrayList<EthernetSwitch>();
    private final List<VirtualServerDevice> virutalHosts = new ArrayList<VirtualServerDevice>();

    @Override
    public synchronized List<EthernetSwitch> getVSwitches() {
        return new ArrayList<EthernetSwitch>(this.vSwitches);
    }

    @Override
    public synchronized void addVSwitch(EthernetSwitch vSwitch) {
        if (this.vSwitches.contains(vSwitch)) {
            return;
        }
        this.vSwitches.add(vSwitch);
    }

    @Override
    public synchronized List<VirtualServerDevice> getVirtualHosts() {
        return new ArrayList<VirtualServerDevice>(this.virutalHosts);
    }

    @Override
    public synchronized void addVirtualHost(VirtualServerDevice virtualHost) {
        if (this.virutalHosts.contains(virtualHost)) {
            return;
        }
        this.virutalHosts.add(virtualHost);
    }

}