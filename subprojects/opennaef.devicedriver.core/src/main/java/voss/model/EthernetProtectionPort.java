package voss.model;


public interface EthernetProtectionPort extends LogicalEthernetPort {
    EthernetPort getWorkingPort();

    void setWorkingPort(EthernetPort port);
}