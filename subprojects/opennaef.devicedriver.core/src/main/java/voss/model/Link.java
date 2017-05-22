package voss.model;

public interface Link extends VlanModel {

    public PhysicalPort getPort1();

    public PhysicalPort getPort2();

    public void initPorts(PhysicalPort port1, PhysicalPort port2);
}