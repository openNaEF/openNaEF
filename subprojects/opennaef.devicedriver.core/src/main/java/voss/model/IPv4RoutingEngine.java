package voss.model;


import java.util.HashMap;
import java.util.Map;

public class IPv4RoutingEngine implements FunctionalComponent {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, CidrAddress> ifIndexToIpAddressMaskMap = new HashMap<Integer, CidrAddress>();
    private final Map<Port, CidrAddress> resolvedPortToIpAddressMap = new HashMap<Port, CidrAddress>();
    private MplsVlanDevice device = null;
    private final String key;

    public IPv4RoutingEngine(String key) {
        this.key = key;
    }

    public IPv4RoutingEngine(MplsVlanDevice device) {
        this.key = device.getDeviceName() + ":IPv4RoutingEngine";
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getExtInfoKey() {
        return null;
    }

    public synchronized void initDevice(MplsVlanDevice device) {
        assert device != null;
        if (this.device != null) {
            throw new AlreadyInitializedException(this.device, device);
        }
        this.device = device;
        this.device.setIPv4RoutingEngine(this);
    }

    public synchronized void addRoutedPort(Port port, CidrAddress addressMask) {
        assert this.device != null;
        assert port != null;
        assert addressMask != null;

        resolvedPortToIpAddressMap.put(port, addressMask);
        ifIndexToIpAddressMaskMap.put(port.getIfIndex(), addressMask);
    }

    public synchronized void addRoutedPort(int ifIndex, CidrAddress addressMask) {
        assert this.device != null;
        assert addressMask != null;

        ifIndexToIpAddressMaskMap.put(ifIndex, addressMask);
        Port port = this.device.getPortByIfIndex(ifIndex);
        if (port != null) {
            resolvedPortToIpAddressMap.put(port, addressMask);
        }
    }

    public synchronized Map<Integer, CidrAddress> getIfIndexAndInetAddressMap() {
        HashMap<Integer, CidrAddress> result = new HashMap<Integer, CidrAddress>();
        result.putAll(this.ifIndexToIpAddressMaskMap);

        resolvePortAndInetAddressMap();
        return result;
    }

    public synchronized Map<Port, CidrAddress> getPortAndInetAddressMap() {
        resolvePortAndInetAddressMap();
        HashMap<Port, CidrAddress> result = new HashMap<Port, CidrAddress>();
        result.putAll(this.resolvedPortToIpAddressMap);
        return result;
    }

    private synchronized void resolvePortAndInetAddressMap() {
        for (Map.Entry<Integer, CidrAddress> entry : ifIndexToIpAddressMaskMap.entrySet()) {
            int ifIndex = entry.getKey().intValue();
            Port port = this.device.getPortByIfIndex(ifIndex);
            if (port != null && resolvedPortToIpAddressMap.get(port) == null) {
                resolvedPortToIpAddressMap.put(port, entry.getValue());
            }
        }
    }

    public synchronized void removeRoutedPort(Port port) {
        assert this.device != null;
        assert port != null;
    }

    public synchronized void removeRoutedPort(int ifIndex) {
        assert this.device != null;

        ifIndexToIpAddressMaskMap.remove(ifIndex);
        Port target = null;
        for (Port key : resolvedPortToIpAddressMap.keySet()) {
            if (key != null && key.getIfIndex() == ifIndex) {
                target = key;
                break;
            }
        }
        if (target != null) {
            resolvedPortToIpAddressMap.remove(target);
        }
    }

}