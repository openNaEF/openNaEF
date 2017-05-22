package voss.model;

import voss.model.impl.PortCrossConnectionEngineImpl;

import java.util.*;

public abstract class AbstractDevice extends AbstractContainerImpl implements Device {
    private static final long serialVersionUID = 1L;

    private final List<Device> virtuals = new ArrayList<Device>();
    private Device physicalDevice = null;

    private MultiDeviceChassis chassis = null;

    private String vendorName_;
    private String osTypeName_;
    private String osVersion_;

    private String site_;
    private String serialNumber_;
    private String modelRevision;

    private String deviceName_;
    private String domainName;
    private String description_;
    private String contactInfo_;
    private String location_;
    private String basePhysicalAddress_;
    private String[] physicalAddresses_ = new String[0];
    private String ipAddress_;
    private String[] ipAddresses_ = new String[0];

    private final Map<CidrAddress, Port> ipAddressPortMap = new HashMap<CidrAddress, Port>();

    private final PortCrossConnectionEngine portCrossConnectionEngine =
            new PortCrossConnectionEngineImpl();

    private String gatewayAddress_;
    private String[] trapReceiverAddresses_ = new String[0];
    private String[] syslogServerAddresses_ = new String[0];
    private Date time_;
    private Date sysUptime = new Date(0);
    private String communityRO_;
    private String osImageName = null;

    private ConfigTargets configTargets_ = new ConfigTargets(
            AbstractDevice.this);

    public AbstractDevice() {
    }

    public synchronized Device getDevice() {
        return this;
    }

    @Override
    public synchronized void setPhysicalDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device is null.");
        } else if (device.isVirtualDevice()) {
            throw new IllegalArgumentException("device[" + device.getDeviceName() + "] is virtual device.");
        }
        this.physicalDevice = device;
    }

    public synchronized Device getPhysicalDevice() {
        return this.physicalDevice;
    }

    @Override
    public synchronized boolean isVirtualDevice() {
        return this.physicalDevice != null;
    }

    @Override
    public synchronized void addVirtualDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device is null.");
        } else if (!device.isVirtualDevice()) {
            throw new IllegalArgumentException("device is not virtual device.");
        } else if (this.virtuals.contains(device)) {
            return;
        }
        this.virtuals.add(device);
    }

    @Override
    public synchronized List<Device> getVirtualDevices() {
        return new ArrayList<Device>(this.virtuals);
    }

    @Override
    public synchronized List<Device> clearVirtualDevices() {
        List<Device> result = new ArrayList<Device>();
        for (Device device : this.virtuals) {
            device.setPhysicalDevice(null);
            result.add(device);
        }
        this.virtuals.clear();
        return result;
    }

    public synchronized MultiDeviceChassis getMultiDeviceChassis() {
        return chassis;
    }

    public synchronized void setMultiDeviceChassis(MultiDeviceChassis chassis) {
        this.chassis = chassis;
    }

    public synchronized boolean hasMultiDeviceChassis() {
        return this.chassis != null;
    }

    public synchronized PortCrossConnectionEngine getPortCrossConnectionEngine() {
        return this.portCrossConnectionEngine;
    }

    public synchronized String getVendorName() {
        return vendorName_;
    }

    public synchronized void setVendorName(String vendorName) {
        vendorName_ = vendorName;
    }

    public synchronized String getModelRevision() {
        return this.modelRevision;
    }

    public synchronized void setModelRevision(String revision) {
        this.modelRevision = revision;
    }

    public synchronized String getOsTypeName() {
        return osTypeName_;
    }

    public synchronized void setOsTypeName(String osTypeName) {
        osTypeName_ = osTypeName;
    }

    public synchronized String getOsVersion() {
        return osVersion_;
    }

    public synchronized void setOsVersion(String osVersion) {
        osVersion_ = osVersion;
    }

    public synchronized String getSite() {
        return site_;
    }

    public synchronized void setSite(String site) {
        site_ = site;
    }

    public synchronized String getSerialNumber() {
        return this.serialNumber_;
    }

    public synchronized void setSerialNumber(String serial) {
        this.serialNumber_ = serial;
    }

    public synchronized String getDeviceName() {
        return deviceName_;
    }

    public synchronized void setDeviceName(String deviceName) {
        deviceName_ = deviceName;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public synchronized String getDescription() {
        return description_;
    }

    public synchronized void setDescription(String description) {
        description_ = description;
    }

    public synchronized String getContactInfo() {
        return contactInfo_;
    }

    public synchronized void setContactInfo(String contactInfo) {
        contactInfo_ = contactInfo;
    }

    public synchronized String getLocation() {
        return location_;
    }

    public synchronized void setLocation(String location) {
        location_ = location;
    }

    public synchronized String getBasePhysicalAddress() {
        return basePhysicalAddress_;
    }

    public synchronized void setBasePhysicalAddress(String basePhysicalAddress) {
        basePhysicalAddress_ = basePhysicalAddress;
    }

    public synchronized String[] getPhysicalAddresses() {
        return physicalAddresses_;
    }

    public synchronized void setPhysicalAddresses(String[] physicalAddresses) {
        physicalAddresses_ = physicalAddresses;
    }

    public synchronized String getIpAddress() {
        return ipAddress_;
    }

    public synchronized void setIpAddress(String ipAddress) {
        ipAddress_ = ipAddress;
    }

    public synchronized String[] getIpAddresses() {
        return ipAddresses_;
    }

    public synchronized void setIpAddresses(String[] ipAddresses) {
        ipAddresses_ = ipAddresses;
    }

    public synchronized String getGatewayAddress() {
        return gatewayAddress_;
    }

    public synchronized void setGatewayAddress(String gatewayAddress) {
        gatewayAddress_ = gatewayAddress;
    }

    public synchronized String[] getTrapReceiverAddresses() {
        return trapReceiverAddresses_;
    }

    public synchronized void setTrapReceiverAddresses(
            String[] trapReceiverAddresses) {
        trapReceiverAddresses_ = trapReceiverAddresses;
    }

    public synchronized String[] getSyslogServerAddresses() {
        return syslogServerAddresses_;
    }

    public synchronized void setSyslogServerAddresses(
            String[] syslogServerAddresses) {
        syslogServerAddresses_ = syslogServerAddresses;
    }

    public synchronized void setOsImageName(String osImageName) {
        this.osImageName = osImageName;
    }

    public synchronized String getOsImageName() {
        return this.osImageName;
    }

    public PhysicalPort[] getFixedChassisPhysicalPorts() {
        List<PhysicalPort> result = new ArrayList<PhysicalPort>();
        PhysicalPort[] physicalPorts = getPhysicalPorts();
        for (int i = 0; i < physicalPorts.length; i++) {
            PhysicalPort port = physicalPorts[i];
            if (port.isFixedChassisPort()) {
                result.add(port);
            }
        }
        return result.toArray(new PhysicalPort[0]);
    }

    public PhysicalPort[] getModulePhysicalPorts() {
        List<PhysicalPort> result = new ArrayList<PhysicalPort>();
        PhysicalPort[] physicalPorts = getPhysicalPorts();
        for (int i = 0; i < physicalPorts.length; i++) {
            PhysicalPort port = physicalPorts[i];
            if (!port.isFixedChassisPort()) {
                result.add(port);
            }
        }
        return result.toArray(new PhysicalPort[0]);
    }

    public Link[] getLinks() {
        PhysicalPort[] physicalPorts = getPhysicalPorts();
        List<Link> result = new ArrayList<Link>();
        for (int i = 0; i < physicalPorts.length; i++) {
            Link link = physicalPorts[i].getLink();
            if (link != null) {
                result.add(link);
            }
        }
        return result.toArray(new Link[0]);
    }

    public synchronized ConfigTargets getConfigTargets() {
        return configTargets_;
    }

    public synchronized void setTime(Date time) {
        time_ = time;
    }

    public synchronized Date getTime() {
        return time_;
    }

    public void setSysUpTime(Date uptime) {
        this.sysUptime.setTime(uptime.getTime());
    }

    public Date getSysUpTime() {
        return new Date(this.sysUptime.getTime());
    }

    public synchronized void setCommunityRO(String communityRO) {
        communityRO_ = communityRO;
    }

    public synchronized String getCommunityRO() {
        return communityRO_;
    }

    public synchronized void addIpAddressToPort(CidrAddress ipAddress, Port port) {
        if (ipAddress == null) {
            throw new IllegalArgumentException();
        }
        if (port == null) {
            throw new IllegalArgumentException();
        }
        ipAddressPortMap.put(ipAddress, port);
    }

    public synchronized Set<CidrAddress> getIpAddresses(Port port) {
        Set<CidrAddress> result = new HashSet<CidrAddress>();
        for (Map.Entry<CidrAddress, Port> entry : ipAddressPortMap.entrySet()) {
            if (entry.getValue() == port) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public synchronized Port getPortByIpAddress(CidrAddress ipAddress) {
        return ipAddressPortMap.get(ipAddress);
    }

    public synchronized Map<CidrAddress, Port> getIpAddressesWithMask() {
        Map<CidrAddress, Port> result = new HashMap<CidrAddress, Port>();
        result.putAll(this.ipAddressPortMap);
        return result;

    }

    public synchronized String getContainerName() {
        return this.deviceName_;
    }
}