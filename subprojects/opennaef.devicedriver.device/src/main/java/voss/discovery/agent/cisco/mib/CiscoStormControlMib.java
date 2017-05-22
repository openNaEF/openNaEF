package voss.discovery.agent.cisco.mib;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.ext.StormControlActionRenderer;
import voss.discovery.agent.cisco.ext.StormControlBroadcastLevelRenderer;
import voss.discovery.agent.cisco.ext.StormControlNotificationActionRenderer;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.Device;
import voss.model.Port;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class CiscoStormControlMib {

    public static final String ciscoPortStormControlMIB = ".1.3.6.1.4.1.9.9.362";

    public static final String ciscoPortStormControlMIBObjects = ciscoPortStormControlMIB + ".1";

    public static final String cpscConfigObjects = ciscoPortStormControlMIBObjects + ".1";

    public static final String cpscThresholdTable = cpscConfigObjects + ".1";

    public static final String cpscUpperThreshold = cpscThresholdTable + ".1.2";

    public static final String cpscLowerThreshold = cpscThresholdTable + ".1.3";

    public static final String cpscActionTable = cpscConfigObjects + ".2";

    public static final String cpscActionEntry = cpscActionTable + ".1";

    public static final String cpscAction = cpscActionEntry + ".1";

    public static final String cpscNotificationControl = cpscActionEntry + ".2";


    public static void setStormControl(SnmpAccess access, Device device) throws IOException, AbortedException {
        final Logger log = LoggerFactory.getLogger(CiscoStormControlMib.class);
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(access, CiscoStormControlMib.cpscUpperThreshold);
            for (IntSnmpEntry entry : entries) {
                BigInteger[] suffixes = entry.oidSuffix;
                if (suffixes.length != 2) {
                    throw new IllegalStateException("unexpected oid suffix: " + entry.oidSuffix);
                }
                int ifIndex = suffixes[0].intValue();
                int type = suffixes[1].intValue();
                int level = entry.intValue();
                if (type != 1) {
                    continue;
                }
                Port p = device.getPortByIfIndex(ifIndex);
                if (p == null) {
                    log.warn("no port found: ifIndex=" + ifIndex);
                    continue;
                }
                StormControlBroadcastLevelRenderer renderer = new StormControlBroadcastLevelRenderer(p);
                renderer.set(Integer.valueOf(level));
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }

        MibTable ifTable = new MibTable(access, "", CiscoStormControlMib.cpscActionEntry);
        ifTable.addColumn("1", "cpscAction");
        ifTable.addColumn("2", "cpscNotificationControl");
        ifTable.walk();
        for (TableRow row : ifTable.getRows()) {
            KeyHolder key = row.getKey();
            int ifIndex = key.intValue(0);
            Port port = device.getPortByIfIndex(ifIndex);
            if (port == null) {
                continue;
            }
            int actionValue = row.getColumnValue("1", SnmpHelper.intEntryBuilder).intValue();
            StormControlActionRenderer renderer1 = new StormControlActionRenderer(port);
            renderer1.set(actionValue);
            int trapValue = row.getColumnValue("2", SnmpHelper.intEntryBuilder).intValue();
            StormControlNotificationActionRenderer renderer2 = new StormControlNotificationActionRenderer(port);
            renderer2.set(trapValue);
        }
    }
}