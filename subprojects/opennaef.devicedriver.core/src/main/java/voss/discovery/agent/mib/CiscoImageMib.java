package voss.discovery.agent.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.OSType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.IntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.Device;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.integerKeyCreator;
import static voss.discovery.iolib.snmp.SnmpHelper.stringEntryBuilder;

public class CiscoImageMib {
    private static final Logger log = LoggerFactory.getLogger(CiscoImageMib.class);
    public static final String ciscoImageString = ".1.3.6.1.4.1.9.9.25.1.1.1.2";
    public static final String SYMBOL_ciscoImageString =
            "enterprises.cisco.ciscoMgmt.ciscoImageMIB.ciscoImageMIBObjects.ciscoImageTable"
                    + ".ciscoImageEntry.ciscoImageString";

    public final static String IOS_SIGNIFICANT1 = "Cisco Internetwork Operating System Software";
    public final static String IOS_SIGNIFICANT2 = "Cisco IOS Software";
    public final static String IOS_XR_SIGNIFICANT = "Cisco IOS XR Software";
    public final static String CATOS_SIGNIFICANT = "Catalyst Operating System";

    public static final String KEY_SYSDESCR = "CW_SYSDESCR";
    public static final String KEY_VERSION = "CW_VERSION";

    private final Map<String, String> imageString = new HashMap<String, String>();

    public CiscoImageMib(SnmpAccess snmp) throws IOException, AbortedException {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        Map<IntegerKey, StringSnmpEntry> ciscoImageStrings =
                SnmpUtil.getWalkResult(snmp, ciscoImageString, stringEntryBuilder, integerKeyCreator);

        for (IntegerKey key : ciscoImageStrings.keySet()) {
            String value = ciscoImageStrings.get(key).getValue();
            String[] values = value.split("\\$");
            if (values.length < 2) {
                log.warn("unexpected value: " + value + " (length=" + values.length + ")");
                continue;
            }
            imageString.put(values[0], values[1]);
        }
    }

    public String getSysDescription() {
        return this.getImageString(KEY_SYSDESCR);
    }

    public void setDeviceDescription(Device device) {
        device.setDescription(getImageString(KEY_SYSDESCR));
    }

    public void setOsVersion(Device device) {
        device.setOsVersion(getImageString(KEY_VERSION));
    }

    private String getImageString(String key) {
        return this.imageString.get(key);
    }

    public static OSType getOSType(SnmpAccess snmp) throws AbortedException, IOException {
        CiscoImageMib ciscoImageMib = new CiscoImageMib(snmp);

        String sysDescr = ciscoImageMib.getSysDescription();
        log.debug("sysDescr: [" + sysDescr + "]");
        if (sysDescr.contains(IOS_SIGNIFICANT1)) {
            return OSType.IOS;
        } else if (sysDescr.contains(IOS_SIGNIFICANT2)) {
            return OSType.IOS;
        } else if (sysDescr.contains(CATOS_SIGNIFICANT)) {
            return OSType.CATOS;
        }

        sysDescr = Mib2Impl.getSysDescr(snmp);
        if (sysDescr.contains(IOS_XR_SIGNIFICANT)) {
            return OSType.IOS_XR;
        }
        throw new IllegalStateException("unknown os type: " + sysDescr);
    }
}