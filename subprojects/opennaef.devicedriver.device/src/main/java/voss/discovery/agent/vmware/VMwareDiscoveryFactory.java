package voss.discovery.agent.vmware;

import voss.discovery.agent.common.DeviceDiscovery;
import voss.discovery.agent.common.ExtraDiscoveryFactory;
import voss.discovery.iolib.DeviceAccess;

public class VMwareDiscoveryFactory implements ExtraDiscoveryFactory {

    @Override
    public DeviceDiscovery getDiscovery(String typeName, DeviceAccess access) throws Exception {
        return new VMwareDeviceDiscovery(access);
    }

}