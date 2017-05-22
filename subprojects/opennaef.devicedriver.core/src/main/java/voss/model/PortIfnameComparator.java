package voss.model;

import java.util.Comparator;

public class PortIfnameComparator implements Comparator<Port> {

    private final Comparator<String> ifnameComparator_ = new IfNameComparator();

    public synchronized int compare(Port port1, Port port2) {
        if (port1 == null ? port2 == null : port1.equals(port2)) {
            return 0;
        } else if (port1 == null) {
            return -1;
        } else if (port2 == null) {
            return 1;
        }

        Device device1 = port1.getDevice();
        Device device2 = port2.getDevice();

        if (device1 != device2) {
            return device1.getDeviceName().compareTo(device2.getDeviceName());
        } else {
            return ifnameComparator_.compare(port1.getIfName(), port2.getIfName());
        }
    }
}