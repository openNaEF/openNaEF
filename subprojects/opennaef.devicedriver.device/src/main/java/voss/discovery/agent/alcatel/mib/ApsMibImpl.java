package voss.discovery.agent.alcatel.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.agent.common.IanaIfType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.MplsVlanDevice;
import voss.model.POSAPSImpl;
import voss.model.POSImpl;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class ApsMibImpl {
    public static final String apsMIB = ".1.3.6.1.2.1.10.49";
    public static final String apsMIBObjects = apsMIB + ".1";
    public static final String apsMap = apsMIBObjects + ".3";
    public static final String apsMapTable = apsMap + ".2";
    public static final String apsMapEntry = apsMapTable + ".1";
    public static final String apsMapGroupName = apsMapEntry + ".2";
    public static final String apsMapChanNumber = apsMapEntry + ".3";
    private static final Logger log = LoggerFactory.getLogger(ApsMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    private final TimetraPortMibImpl timetraPortMib;

    public ApsMibImpl(Alcatel7710SRDiscovery discovery) {

        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDeviceInner();

        timetraPortMib = discovery.getTimetraPortMib();
    }

    public void createAPS() throws IOException, AbortedException {

        Map<IntegerKey, StringSnmpEntry> apsMapGroupNameMap =
                SnmpUtil.getWalkResult(snmp, apsMapGroupName, stringEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> apsMapChanNumberMap =
                SnmpUtil.getWalkResult(snmp, apsMapChanNumber, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : apsMapGroupNameMap.keySet()) {

            int ifIndex = key.getInt();
            String groupName = apsMapGroupNameMap.get(key).getValue();

            if (groupName.length() > 0) {

                IanaIfType ianaIfType = timetraPortMib.getIfType(ifIndex);
                int apsIfIndex = timetraPortMib.getIfIndex(groupName);

                if (ianaIfType == IanaIfType.sonet) {
                    POSAPSImpl aps = (POSAPSImpl) device.getPortByIfIndex(apsIfIndex);
                    if (aps == null) {
                        aps = new POSAPSImpl();
                        aps.initDevice(device);
                        aps.initIfIndex(apsIfIndex);
                        aps.initIfName(groupName);
                        log.debug("@ add port " + apsIfIndex + " type " + aps.getClass().getSimpleName()
                                + " to device='" + device.getDeviceName() + "'");
                    }

                    POSImpl pos = (POSImpl) device.getPortByIfIndex(ifIndex);
                    aps.addMemberPort(pos);
                    log.debug("@ add member port " + ifIndex + " to " + apsIfIndex);

                    int chanNumber = apsMapChanNumberMap.get(key).intValue();
                    if (chanNumber > 0) {
                        aps.setWorkingPort(pos);
                    }
                } else {
                    log.debug("Skip '" + groupName + "' type=" + ianaIfType);
                }
            }
        }
    }
}