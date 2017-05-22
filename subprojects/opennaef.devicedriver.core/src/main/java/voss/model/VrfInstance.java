package voss.model;

import java.util.List;
import java.util.Map;


public interface VrfInstance extends LogicalPort {
    String getVrfID();

    void initVrfID(String id);

    String getRouteDistinguisher();

    void setRouteDistinguisher(String rd);

    String getRouteTarget();

    void setRouteTarget(String rt);

    void addAttachmentPort(Port port);

    void removeAttachmentPort(Port port);

    List<Port> getAttachmentPorts();

    void addVpnIpAddress(Port port, CidrAddress vpnIpAddress);

    void removeVpnIpAddress(Port port);

    CidrAddress getVpnIpAddress(Port port);

    Map<Port, CidrAddress> getVpnPortAddressMap();
}