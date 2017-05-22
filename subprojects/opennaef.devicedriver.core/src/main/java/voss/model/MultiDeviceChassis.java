package voss.model;

import java.util.ArrayList;
import java.util.List;

public class MultiDeviceChassis extends AbstractVlanModel {
    private static final long serialVersionUID = 1L;

    private final List<Device> devices = new ArrayList<Device>();
    private Device managementDevice = null;
    private String chassisName;
    private String vendorName;
    private String chassisType;

    public MultiDeviceChassis() {
        super();
    }

    public synchronized void addDevice(Device device) {
        MultiDeviceChassis current = device.getMultiDeviceChassis();
        if (current != null && current != this) {
            throw new IllegalArgumentException("device[" + device.getDeviceName() + "] is already" +
                    "member of another chassis[" + current.getChassisName() + "]");
        }
        device.setMultiDeviceChassis(this);
        if (this.devices.contains(device)) {
            return;
        }
        this.devices.add(device);
    }

    public synchronized List<Device> getDevices() {
        return new ArrayList<Device>(this.devices);
    }

    public synchronized Device getManagementDevice() {
        return managementDevice;
    }

    public synchronized void setManagementDevice(Device managementDevice) {
        this.managementDevice = managementDevice;
    }

    public synchronized String getChassisName() {
        return chassisName;
    }

    public synchronized void setChassisName(String chassisName) {
        this.chassisName = chassisName;
    }

    public synchronized String getVendorName() {
        return vendorName;
    }

    public synchronized void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public synchronized String getChassisType() {
        return chassisType;
    }

    public synchronized void setChassisType(String chassisType) {
        this.chassisType = chassisType;
    }

}