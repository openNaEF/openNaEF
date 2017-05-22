package voss.model;


import java.util.List;

public interface VMwareServer extends VlanDevice {

    List<EthernetSwitch> getVSwitches();

    void addVSwitch(EthernetSwitch vSwitch);

    List<VirtualServerDevice> getVirtualHosts();

    void addVirtualHost(VirtualServerDevice virtualHost);
}