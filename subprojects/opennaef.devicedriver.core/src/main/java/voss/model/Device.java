package voss.model;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface Device extends Container {
    boolean isVirtualDevice();

    void setPhysicalDevice(Device physical);

    Device getPhysicalDevice();

    void addVirtualDevice(Device virtual);

    List<Device> getVirtualDevices();

    List<Device> clearVirtualDevices();

    MultiDeviceChassis getMultiDeviceChassis();

    void setMultiDeviceChassis(MultiDeviceChassis chassis);

    boolean hasMultiDeviceChassis();

    public String getVendorName();

    public void setVendorName(String vendorName);

    String getModelRevision();

    void setModelRevision(String revision);

    public String getOsTypeName();

    public void setOsTypeName(String osTypName);

    public String getOsVersion();

    public void setOsVersion(String osVersion);

    public String getSite();

    public void setSite(String site);

    public Date getSysUpTime();

    public void setSysUpTime(Date uptime);

    public String getDeviceName();

    public void setDeviceName(String deviceName);

    public String getDomainName();

    public void setDomainName(String domainName);

    public String getDescription();

    public void setDescription(String description);

    public String getContactInfo();

    public void setContactInfo(String contactInfo);

    public String getLocation();

    public void setLocation(String location);

    public String getBasePhysicalAddress();

    public void setBasePhysicalAddress(String basePhysicalAddress);

    public String[] getPhysicalAddresses();

    public void setPhysicalAddresses(String[] physicalAddresses);

    public String getIpAddress();

    public void setIpAddress(String ipAddress);

    public String[] getIpAddresses();

    public void setIpAddresses(String[] ipAddresses);

    public String getGatewayAddress();

    public void addIpAddressToPort(CidrAddress ipAddress, Port port);

    public Set<CidrAddress> getIpAddresses(Port port);

    public Port getPortByIpAddress(CidrAddress ipAddress);

    public Map<CidrAddress, Port> getIpAddressesWithMask();

    public PortCrossConnectionEngine getPortCrossConnectionEngine();

    public void setGatewayAddress(String gatewayAddress);

    public String[] getTrapReceiverAddresses();

    public void setTrapReceiverAddresses(String[] trapReceiverAddresses);

    public String[] getSyslogServerAddresses();

    public void setSyslogServerAddresses(String[] syslogServerAddresses);

    public PhysicalPort[] getFixedChassisPhysicalPorts();

    public PhysicalPort[] getModulePhysicalPorts();

    public Link[] getLinks();

    public ConfigTargets getConfigTargets();

    public void setTime(Date time);

    public Date getTime();

    public void setCommunityRO(String communityRO);

    public String getCommunityRO();
}