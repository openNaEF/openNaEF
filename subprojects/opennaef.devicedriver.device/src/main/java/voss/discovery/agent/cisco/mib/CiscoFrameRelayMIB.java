package voss.discovery.agent.cisco.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.TwoIntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.*;
import voss.util.ModelUtils;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.intEntryBuilder;
import static voss.discovery.iolib.snmp.SnmpHelper.twoIntegerKeyCreator;

public class CiscoFrameRelayMIB {
    private static final Logger log = LoggerFactory.getLogger(CiscoFrameRelayMIB.class);
    private final Device device;
    private final SnmpAccess snmp;

    public static final String OID_cfrCircuitType = ".1.3.6.1.4.1.9.9.49.1.2.1.1.4";
    public static final String SYMBOL_cfrCircuitType =
            ".iso.org.dod.internet.private.enterprises.cisco.ciscoMgmt.ciscoFrameRelayMIB" +
                    ".ciscoFrMIBObjects.cfrCircuitObjs.cfrCircuitTable.cfrCircuitEntry.cfrCircuitType";

    public CiscoFrameRelayMIB(Device device, SnmpAccess snmp) {
        this.device = device;
        this.snmp = snmp;
    }

    public void createFrameRelayDLCI() throws AbortedException, IOException {
        Map<TwoIntegerKey, IntSnmpEntry> cfrCircuitTypes =
                SnmpUtil.getWalkResult(snmp, OID_cfrCircuitType, intEntryBuilder, twoIntegerKeyCreator);
        for (Map.Entry<TwoIntegerKey, IntSnmpEntry> entry : cfrCircuitTypes.entrySet()) {
            int ifIndex = entry.getKey().intValue1();
            int dlci = entry.getKey().intValue2();

            Port port = device.getPortByIfIndex(ifIndex);
            if (port == null) {
                throw new IllegalStateException("port not found: "
                        + device.getDeviceName() + ":ifIndex=" + ifIndex);
            }

            FrameRelayFeature fr = ModelUtils.getFrameRelayFeature(port);
            if (fr == null) {
                throw new IllegalStateException("not frame relay port: "
                        + device.getDeviceName() + ":ifIndex=" + ifIndex
                        + ". type=" + port.getClass().getName());
            }

            FrameRelayDLCIEndPoint pvc = new FrameRelayDLCIEndPointImpl();
            pvc.initDevice(device);
            pvc.initIfName(port.getIfName() + ":" + dlci);
            pvc.initParentPort(fr);
            pvc.setDLCI(dlci);
            fr.addEndPoint(pvc);
            log.debug("@ create fr-pvc"
                    + " ifName='" + pvc.getIfName()
                    + "' parent='" + port.getIfName()
                    + "' dcli='" + dlci
                    + "' on '" + port.getDevice().getDeviceName() + "';");
        }
    }
}