package voss.discovery.agent.fortigate;

import voss.discovery.agent.common.ConfigurationStructure;
import voss.discovery.agent.common.PhysicalEntry;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.model.Device;
import voss.model.MplsVlanDevice;
import voss.model.Port;

import java.io.IOException;

public interface FortigateEntityFactory {
    Port createPort(PhysicalEntry pe, Device device);

    void createVdom(DeviceAccess access, MplsVlanDevice device) throws IOException, AbortedException;

    void createVdomLink(ConfigurationStructure config, MplsVlanDevice device) throws IOException, AbortedException;
}