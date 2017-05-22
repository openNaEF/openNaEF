package voss.model;

public interface VlanIf extends LogicalPort {

    public static final int NULL_VLAN_ID = -1;
    public static final String STATUS_ENABLED = "enabled";
    public static final String STATUS_DISABLED = "disabled";

    public void initVlanKey(String key);

    public String getVlanKey();

    public int getVlanIfIndex() throws NotInitializedException;

    public void initVlanIfIndex(int vlanIfIndex);

    public Integer getEoeId();

    public int getVlanId() throws NotInitializedException;

    public void initVlanId(Integer eoeId, int vlanId);

    public void initVlanId(int vlanId);

    public String getExtendedVlanId();

    public boolean isSameVlan(Integer eoeID, int vlanID);

    public String getVlanName();

    public void setVlanName(String vlanName);

    public void setTaggedPorts(LogicalEthernetPort[] taggedPorts);

    public void addTaggedPort(LogicalEthernetPort taggedPort);

    public void removeTaggedPort(LogicalEthernetPort taggedPort);

    public LogicalEthernetPort[] getTaggedPorts();

    public void setUntaggedPorts(LogicalEthernetPort[] untaggedPorts);

    public void addUntaggedPort(LogicalEthernetPort untaggedPort);

    public void removeUntaggedPort(LogicalEthernetPort untaggedPort);

    public LogicalEthernetPort[] getUntaggedPorts();

    public LogicalEthernetPort[] getBindedPorts();

    public boolean isBindedAsTagged(LogicalEthernetPort logicalEth);

    public boolean isBindedAsUntagged(LogicalEthernetPort logicalEth);

    public String[] getIpAddresses();

    public void setIpAddresses(String[] ipAddresses);

    public VlanStpElement getVlanStpElement();

    public void setVlanStpElement(VlanStpElement vlanStpElement);

    public boolean isBridge();

    public void setBridge(boolean bridge);
}