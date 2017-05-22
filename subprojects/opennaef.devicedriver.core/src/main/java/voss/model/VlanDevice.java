package voss.model;


public interface VlanDevice extends Device {

    public void setEoeEnable(boolean eoeEnable);

    public boolean isEoeEnable();

    public String getSpanningTreeType();

    public void setSpanningTreeType(String spanningTreeType);

    public VlanIf[] getVlanIfs();

    public EthernetPort[] getEthernetPorts();

    public LogicalEthernetPort[] getLogicalEthernetPorts();

    public EthernetPortsAggregator[] getEthernetPortsAggregators();

    public LogicalEthernetPort.TagChanger[] getTagChangers();

    public VlanIf getUntaggedVlanIf(EthernetPort ethernetPort);

    public LogicalEthernetPort getLogicalEthernetPort(EthernetPort ethernetPort);

    public EthernetPortsAggregator getEthernetPortsAggregatorByAggregationGroupId
            (int aggregationGroupId);

    public VlanIf getVlanIfByVlanId(int vlanId);

    public VlanIf getVlanIfBy(Integer eoeId, int vlanId);

    public VlanStpElement[] getVlanStpElements();

    public void addVlanStpElement(VlanStpElement vlanStpElement);
}