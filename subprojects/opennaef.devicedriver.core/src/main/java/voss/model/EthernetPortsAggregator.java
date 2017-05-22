package voss.model;

public interface EthernetPortsAggregator extends LogicalEthernetPort {

    public void addPhysicalPort(EthernetPort physicalPort);

    public boolean isAggregatorOf(EthernetPort physicalPort);

    public EthernetPort getMasterPort();

    public void initMasterPort(EthernetPort masterPort);

    public Integer getAggregationGroupId();

    public void initAggregationGroupId(int aggregationGroupId);

    public String getAggregationName();

    public void setAggregationName(String aggregationName);
}