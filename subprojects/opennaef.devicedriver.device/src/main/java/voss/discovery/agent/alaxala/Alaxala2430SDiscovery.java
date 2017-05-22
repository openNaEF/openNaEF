package voss.discovery.agent.alaxala;

import voss.discovery.agent.alaxala.mib.Alaxala3600SMibImpl;
import voss.discovery.agent.alaxala.profile.AlaxalaVendorProfile;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.snmp.SnmpAccess;

import java.io.IOException;

public class Alaxala2430SDiscovery extends AlaxalaSwitchDiscovery {

    public Alaxala2430SDiscovery(DeviceAccess access, AlaxalaVendorProfile profile) {
        super(access, profile, new Alaxala3600SMibImpl(access.getSnmpAccess(), profile));
    }

    public static boolean isTargetDevice(String sysObjectId, SnmpAccess snmpAccess) throws Exception {
        return sysObjectId.startsWith(".1.3.6.1.4.1.21839.1.2.6");
    }

    @Override
    protected void collectSlotsAndModules() throws IOException, AbortedException {
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }
}