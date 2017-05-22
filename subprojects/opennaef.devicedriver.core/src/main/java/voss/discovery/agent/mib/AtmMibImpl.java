package voss.discovery.agent.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class AtmMibImpl {
    private static final Logger log = LoggerFactory.getLogger(AtmMibImpl.class);

    public static final String OID_atmVplAdminStatus = ".1.3.6.1.2.1.37.1.6.1.2";

    public static final String OID_atmVplOperStatus = ".1.3.6.1.2.1.37.1.6.1.3";

    public static final String OID_atmVclAdminStatus = ".1.3.6.1.2.1.37.1.7.1.3";

    public static final String OID_atmVclOperStatus = ".1.3.6.1.2.1.37.1.7.1.4";

    public static final String OID_atmVccAalType = ".1.3.6.1.2.1.37.1.7.1.8";

    private final SnmpAccess snmp;
    private final Device device;

    public AtmMibImpl(SnmpAccess snmp, Device device) {
        this.snmp = snmp;
        this.device = device;
    }

    public void createAtmVp() throws AbortedException, IOException {
        Map<TwoIntegerKey, IntSnmpEntry> atmVplAdminStatuses =
                SnmpUtil.getWalkResult(snmp, OID_atmVplAdminStatus, intEntryBuilder, twoIntegerKeyCreator);
        Map<TwoIntegerKey, IntSnmpEntry> atmVplOperStatuses =
                SnmpUtil.getWalkResult(snmp, OID_atmVplOperStatus, intEntryBuilder, twoIntegerKeyCreator);
        for (Map.Entry<TwoIntegerKey, IntSnmpEntry> entry : atmVplAdminStatuses.entrySet()) {
            TwoIntegerKey key = entry.getKey();
            int ifIndex = key.intValue1();
            int vpi = key.intValue2();
            IntSnmpEntry vpAdminStatusEntry = entry.getValue();
            int vpAdminStatus = vpAdminStatusEntry.intValue();
            int vpOperStatus = atmVplOperStatuses.get(key).intValue();

            Port port = device.getPortByIfIndex(ifIndex);
            AtmPort atm = null;
            if (port instanceof AtmPort) {
                atm = (AtmPort) port;
            } else if (port instanceof SerialPort) {
                LogicalPort feature = ((SerialPort) port).getLogicalFeature();
                if (feature instanceof AtmPort) {
                    atm = (AtmPort) feature;
                }
            }
            if (atm == null) {
                throw new IllegalStateException("not atm port: ifIndex=" + ifIndex
                        + ", port=" + (port == null ? "null" : port.getClass().getName()));
            }
            AtmVp vp = new AtmVp(atm, vpi);
            vp.initDevice(device);
            vp.setAdminStatus(getStatus(vpAdminStatus));
            vp.setOperationalStatus(getStatus(vpOperStatus));
            log.debug("@ create vp vpi='" + vpi + "' on '" + this.device.getDeviceName() + "';");
        }
    }

    public void createAtmVc() throws AbortedException, IOException {
        Map<OidKey, IntSnmpEntry> atmVclAdminStatuses =
                SnmpUtil.getWalkResult(snmp, OID_atmVclAdminStatus, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> atmVclOperStatuses =
                SnmpUtil.getWalkResult(snmp, OID_atmVclOperStatus, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> atmVccAalTypes =
                SnmpUtil.getWalkResult(snmp, OID_atmVccAalType, intEntryBuilder, oidKeyCreator);
        for (Map.Entry<OidKey, IntSnmpEntry> entry : atmVclAdminStatuses.entrySet()) {
            OidKey key = entry.getKey();
            int ifIndex = key.getInt(0);
            int vpi = key.getInt(1);
            int vci = key.getInt(2);
            int vcAdminStatus = entry.getValue().intValue();
            int vcOperStatus = atmVclOperStatuses.get(key).intValue();
            int aalType = atmVccAalTypes.get(key).intValue();

            Port port = device.getPortByIfIndex(ifIndex);
            AtmPort atm = null;
            if (port instanceof AtmPort) {
                atm = (AtmPort) port;
            } else if (port instanceof SerialPort) {
                LogicalPort feature = ((SerialPort) port).getLogicalFeature();
                if (feature instanceof AtmPort) {
                    atm = (AtmPort) feature;
                }
            }
            if (atm == null) {
                throw new IllegalStateException("not atm port: ifIndex=" + ifIndex);
            }
            AtmVp vp = atm.getVp(vpi);
            if (vp == null) {
                vp = new AtmVp(atm, atm.getIfName() + "/" + vpi, vpi);
                vp.initDevice(device);
            }
            AtmPvc pvc = new AtmPvc(vp, vci);
            pvc.initDevice(device);
            pvc.setAdminStatus(getStatus(vcAdminStatus));
            pvc.setOperationalStatus(getStatus(vcOperStatus));
            pvc.setAalType(getAalType(aalType));
            log.debug("@ create vc vpi='" + vpi + "' vci='" + vci + "' on '" + this.device.getDeviceName() + "';");
        }
    }

    private String getStatus(int status) {
        switch (status) {
            case 1:
                return "up";
            case 2:
                return "down";
            default:
                return "unknown(" + status + ")";
        }
    }

    public String getAalType(int type) {
        switch (type) {
            case 1:
                return "aal1";
            case 2:
                return "aal34";
            case 3:
                return "aal5";
            case 4:
                return "other";
            case 5:
                return "unknown";
            default:
                return "unknown(" + type + ")";
        }
    }
}