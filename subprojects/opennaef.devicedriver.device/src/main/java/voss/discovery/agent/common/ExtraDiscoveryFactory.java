package voss.discovery.agent.common;

import voss.discovery.iolib.DeviceAccess;

public interface ExtraDiscoveryFactory {
    DeviceDiscovery getDiscovery(String typeName, DeviceAccess access) throws Exception;
}