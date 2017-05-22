package voss.discovery.agent.alaxala;

import voss.discovery.agent.alaxala.mib.Alaxala3600SMibImpl;
import voss.discovery.agent.alaxala.profile.AlaxalaVendorProfile;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;

import java.io.IOException;

public class Alaxala3630SDiscovery extends AlaxalaSwitchDiscovery {

    public Alaxala3630SDiscovery(DeviceAccess access, AlaxalaVendorProfile profile) {
        super(access, profile, new Alaxala3600SMibImpl(access.getSnmpAccess(), profile));
    }

    @Override
    protected void collectSlotsAndModules() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }
}