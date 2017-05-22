package voss.discovery.agent.juniper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.IanaIfType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.AtmAPSImpl;
import voss.model.AtmPortImpl;
import voss.model.MplsVlanDevice;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class ApsMibImpl {

    public static final String apsMIB = JuniperSmi.jnxMibs + ".24";

    public static final String apsMIBObjects = apsMIB + ".1";
    public static final String apsStatusTable = apsMIBObjects + ".2";
    public static final String apsStatusEntry = apsStatusTable + ".1";
    public static final String apsStatusSwitchedChannel = apsStatusEntry + ".8";
    public static final String apsMap = apsMIBObjects + ".3";
    public static final String apsMapTable = apsMap + ".2";
    public static final String apsMapEntry = apsMapTable + ".1";
    public static final String apsMapGroupName = apsMapEntry + ".2";
    public static final String apsMapChanNumber = apsMapEntry + ".3";
    public static final String apsChanStatusTable = apsMIBObjects + ".6";
    public static final String apsChanStatusEntry = apsChanStatusTable + ".1";
    public static final String apsChanStatusCurrent = apsChanStatusEntry + ".1";


    private static final Logger log = LoggerFactory.getLogger(ApsMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    private final JuniperIfMibImpl juniperIfMib;

    public ApsMibImpl(JuniperJunosDiscovery discovery) {

        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDeviceInner();

        this.juniperIfMib = discovery.getJuniperIfMib();
    }

    public void createAPS() throws IOException, AbortedException {

        Map<IntegerKey, StringSnmpEntry> apsMapGroupNameMap =
                SnmpUtil.getWalkResult(snmp, apsMapGroupName, stringEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> apsMapChanNumberMap =
                SnmpUtil.getWalkResult(snmp, apsMapChanNumber, intEntryBuilder, integerKeyCreator);
        Map<OidKey, ByteSnmpEntry> apsChanStatusCurrentMap =
                SnmpUtil.getWalkResult(snmp, apsChanStatusCurrent, byteEntryBuilder, oidKeyCreator);

        for (IntegerKey key : apsMapGroupNameMap.keySet()) {

            int ifIndex = key.getInt();
            String groupName = apsMapGroupNameMap.get(key).getValue();
            if (device.getPortByIfIndex(ifIndex) == null) {
                log.warn("broken mib: " + groupName + "(" + ifIndex + ")");
            }

            if (groupName.startsWith("MEMBER_OF_")) {
                String apsIfName = groupName.replace("MEMBER_OF_", "");
                int apsIfIndex = juniperIfMib.getIfIndex(apsIfName);
                AtmAPSImpl aps = (AtmAPSImpl) device.getPortByIfIndex(apsIfIndex);
                if (aps == null) {
                    aps = new AtmAPSImpl();
                    aps.initDevice(device);
                    aps.initIfIndex(apsIfIndex);
                    aps.initIfName(apsIfName);
                    log.debug("@ add port " + apsIfIndex + " type " + aps.getClass().getSimpleName()
                            + " to device '" + device.getDeviceName() + "'");
                    log.debug("@ set port " + apsIfIndex + " ifName '" + apsIfName + "'");
                }

                IanaIfType ianaIfType = null;
                try {
                    ianaIfType = juniperIfMib.getIfType(ifIndex);
                } catch (Exception e) {
                    log.warn("unexpected response: ifIndex=" + ifIndex, e);
                }
                if (ianaIfType == null) {
                    continue;
                } else if (ianaIfType == IanaIfType.atm) {
                    AtmPortImpl atm = (AtmPortImpl) device.getPortByIfIndex(ifIndex);
                    if (atm != null) {
                        aps.addMemberPort(atm);
                        log.debug("@ add member port " + ifIndex + " to " + apsIfIndex);
                    }

                    int chanNumber = apsMapChanNumberMap.get(key).intValue();
                    BigInteger[] bi = new BigInteger[groupName.length() + 2];
                    bi[0] = BigInteger.valueOf(groupName.length());
                    for (int i = 0; i < groupName.length(); i++) {
                        bi[i + 1] = BigInteger.valueOf(groupName.getBytes()[i]);
                    }
                    bi[groupName.length() + 1] = BigInteger.valueOf(chanNumber);
                    OidKey oidKey = new OidKey(bi);
                    int[] status = SnmpUtil.decodeBitList(apsChanStatusCurrentMap.get(oidKey).value);
                    for (int s : status) {
                        if (s == 3 + 1) {
                            aps.setWorkingPort(atm);
                            log.debug("@ set working port " + atm.getIfName() + " of " + apsIfName);
                            break;
                        }
                    }
                } else {
                    log.debug("Skip '" + groupName + "' type=" + ianaIfType);
                }
            }
        }
    }
}