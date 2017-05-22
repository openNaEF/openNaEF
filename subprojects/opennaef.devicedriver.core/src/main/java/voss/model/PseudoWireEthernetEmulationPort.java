package voss.model;


public interface PseudoWireEthernetEmulationPort extends PseudoWireEmulationPort,
        LogicalEthernetPort {

    EthernetPort[] getPhysicalPorts() throws NotInitializedException;

    void initPhysicalPort(EthernetPort port) throws AlreadyInitializedException;
}