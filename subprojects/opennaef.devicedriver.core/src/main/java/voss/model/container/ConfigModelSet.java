package voss.model.container;


import voss.model.Device;
import voss.model.Link;
import voss.model.LogicalPort;
import voss.model.PortAssigner;

import java.io.Serializable;
import java.util.*;

public class ConfigModelSet implements Serializable {
    private static final long serialVersionUID = 1L;

    private int vlanId_;

    private final SortedMap<String, Device> devices_ = new TreeMap<String, Device>();
    private Set<Device> coreDevices_ = new HashSet<Device>();
    private List<PortAssigner> portAssigners_ = new ArrayList<PortAssigner>();
    private List<Link> links_ = new ArrayList<Link>();
    private Set<LogicalPort> broadcastDomainMembers = new HashSet<LogicalPort>();
    private LogicalPort thresholdPort = null;

    private Set<Device> tagConversionSwitches_;
    private Map<Device, Integer> deviceLevels_ = new HashMap<Device, Integer>();

    private String[] errorMessages_;

    public ConfigModelSet(int vlanId) {
        vlanId_ = vlanId;
    }

    public int getUserVlanId() {
        return vlanId_;
    }

    public void setThresholdPort(LogicalPort port) {
        this.thresholdPort = port;
    }

    public LogicalPort getThresholdPort() {
        return this.thresholdPort;
    }

    public void setBroadcastDomainMembers(Set<LogicalPort> members) {
        this.broadcastDomainMembers.addAll(members);
    }

    public Set<LogicalPort> getBroadcastDomainMembers() {
        HashSet<LogicalPort> ports = new HashSet<LogicalPort>();
        ports.addAll(this.broadcastDomainMembers);
        return Collections.unmodifiableSet(ports);
    }

    public void addDevice(Device device) {
        if (getDeviceByName(device.getDeviceName()) != null
                && getDeviceByName(device.getDeviceName()) != device) {
            throw new IllegalArgumentException("It is already registered device: "
                    + device.getDeviceName());
        }
        devices_.put(device.getDeviceName(), device);
    }

    public Device[] getDevices() {
        return devices_.values().toArray(new Device[0]);
    }

    public void addPortAssigner(PortAssigner portAssigner) {
        portAssigners_.add(portAssigner);
    }

    public PortAssigner[] getPortAssigners() {
        return portAssigners_.toArray(new PortAssigner[0]);
    }

    public void addLink(Link link) {
        links_.add(link);
    }

    public Link[] getLinks() {
        return links_.toArray(new Link[0]);
    }

    public void setErrorMessages(String[] errorMessages) {
        errorMessages_ = errorMessages;
    }

    public String[] getErrorMessages() {
        return errorMessages_;
    }

    public Device getDeviceByName(String deviceName) {
        return devices_.get(deviceName);
    }

    public void addCoreDevice(Device coreDevice) {
        if (getDeviceByName(coreDevice.getDeviceName()) == null) {
            throw new IllegalArgumentException("Devices not included in the equipment list can not be specified as core equipment.");
        }

        coreDevices_.add(coreDevice);
    }

    public Device[] getCoreDevices() {
        return coreDevices_.toArray(new Device[0]);
    }

    public void setTagConversionSwitches(Set<Device> tagConversionSwitches) {
        tagConversionSwitches_ = tagConversionSwitches;
    }

    public boolean isTagConversionSwitch(Device device) {
        return tagConversionSwitches_.contains(device);
    }

    public void setDeviceLevel(Device device, int level) {
        deviceLevels_.put(device, new Integer(level));
    }

    public int getDeviceLevel(Device device) {
        return ((Integer) deviceLevels_.get(device)).intValue();
    }
}