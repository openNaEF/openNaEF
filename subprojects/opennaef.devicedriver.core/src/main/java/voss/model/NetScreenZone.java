package voss.model;


import java.util.ArrayList;
import java.util.List;

public class NetScreenZone implements FunctionalComponent {
    private static final long serialVersionUID = 1L;
    public static final String KEY = "NetScreen:Zone";
    private final Device device;
    private final String name;
    private final int id;
    private final List<Port> members = new ArrayList<Port>();

    public NetScreenZone(Device device, String name, int id) {
        if (device == null || name == null) {
            throw new IllegalArgumentException();
        }
        this.device = device;
        this.name = name;
        this.id = id;
        Object extinfo = this.device.gainConfigurationExtInfo().get(KEY);
        if (extinfo == null) {
            extinfo = new ArrayList<NetScreenZone>();
            this.device.gainConfigurationExtInfo().put(KEY, extinfo);
        }
    }

    public String getExtInfoKey() {
        return KEY;
    }

    @Override
    public String getKey() {
        return this.name;
    }

    public String getZoneName() {
        return this.name;
    }

    public int getZoneId() {
        return this.id;
    }

    public void addZoneMemberPort(Port port) {
        if (this.members.contains(port)) {
            return;
        } else if (!this.device.equals(port.getDevice())) {
            throw new IllegalArgumentException("device not match: " + port.getFullyQualifiedName());
        }
        this.members.add(port);
    }

    public void clearZoneMemberPorts() {
        this.members.clear();
    }

    public List<Port> getZoneMemberPorts() {
        List<Port> result = new ArrayList<Port>();
        result.addAll(members);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (!NetScreenZone.class.isInstance(o)) {
            return false;
        }
        NetScreenZone another = (NetScreenZone) o;
        return this.id == another.id;
    }

    @Override
    public int hashCode() {
        return this.device.hashCode() + this.id;
    }

    @Override
    public String toString() {
        return "NetScreenZone:" + this.device.getDeviceName() + ":" + this.name + "(" + this.id + ")";
    }
}