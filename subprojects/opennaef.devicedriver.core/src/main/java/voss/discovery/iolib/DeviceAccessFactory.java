package voss.discovery.iolib;

import voss.discovery.constant.AccessMode;
import voss.model.NodeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public interface DeviceAccessFactory {

    public DeviceAccess getDeviceAccess(NodeInfo nodeinfo) throws IOException, AbortedException;

    public DeviceAccess getDeviceAccess(NodeInfo nodeinfo, List<InetAddress> specified)
            throws IOException, AbortedException;

    public AccessMode getMode();

}