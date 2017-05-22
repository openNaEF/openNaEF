package voss.discovery.agent.alaxala;

import voss.discovery.agent.alaxala.mib.Alaxala7800SMibImpl;
import voss.discovery.agent.alaxala.profile.AlaxalaVendorProfile;
import voss.discovery.iolib.DeviceAccess;

public class Alaxala5400SDiscovery extends Alaxala7800SDiscovery {

    public Alaxala5400SDiscovery(DeviceAccess access, AlaxalaVendorProfile profile) {
        super(access, profile, new Alaxala7800SMibImpl(access.getSnmpAccess(), profile));
    }
}