package voss.discovery.agent.common;

import voss.model.DefaultLogicalEthernetPort;
import voss.model.DefaultLogicalEthernetPortImpl;
import voss.model.EthernetPort;
import voss.model.VlanDevice;

public class DiscoveryUtil {

    public static void supplementLogicalEthernetPort(VlanDevice device) {
        for (EthernetPort port : device.getEthernetPorts()) {
            if (device.getLogicalEthernetPort(port) == null) {
                DefaultLogicalEthernetPort logical = new DefaultLogicalEthernetPortImpl();
                logical.initDevice(device);
                logical.initPhysicalPort(port);
                logical.initIfName("[logical]" + port.getIfName());
            }
        }
    }

}