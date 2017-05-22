package voss.model;

public interface DefaultLogicalEthernetPort extends LogicalEthernetPort {

    public EthernetPort getPhysicalPort();

    public void initPhysicalPort(EthernetPort physicalPort);

    public boolean isDefaultLogicalEthernetPortOf(EthernetPort physicalPort);
}