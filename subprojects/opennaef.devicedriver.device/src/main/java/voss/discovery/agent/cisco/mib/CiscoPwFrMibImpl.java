package voss.discovery.agent.cisco.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.IntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.intEntryBuilder;
import static voss.discovery.iolib.snmp.SnmpHelper.integerKeyCreator;

public class CiscoPwFrMibImpl {
    private static final Logger log = LoggerFactory.getLogger(CiscoPwFrMibImpl.class);
    private final MplsVlanDevice device;
    private final SnmpAccess snmp;

    public CiscoPwFrMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
    }

    public static final String OID_cpwVcFrEntry = ".1.3.6.1.4.1.9.10.112.1.1.1";
    public static final String OID_cpwVcFrIfIndex = ".1.3.6.1.4.1.9.10.112.1.1.1.2";
    public static final String OID_cpwVcFrDlci = ".1.3.6.1.4.1.9.10.112.1.1.1.3";
    public static final String OID_cpwVcFrAdminStatus = ".1.3.6.1.4.1.9.10.112.1.1.1.4";
    public static final String OID_cpwVcFrOperStatus = ".1.3.6.1.4.1.9.10.112.1.1.1.5";
    public static final String OID_cpwVcFrPw2FrOperStatus = ".1.3.6.1.4.1.9.10.112.1.1.1.6";

    public static final String OID_cpwVcFrPMEntry = ".1.3.6.1.4.1.9.10.112.1.2.1";
    public static final String OID_cpwVcFrPMIfIndex = ".1.3.6.1.4.1.9.10.112.1.2.1.2";
    public static final String OID_cpwVcFrPMDlci = ".1.3.6.1.4.1.9.10.112.1.2.1.3";
    public static final String OID_cpwVcFrPMAdminStatus = ".1.3.6.1.4.1.9.10.112.1.2.1.4";
    public static final String OID_cpwVcFrPMOperStatus = ".1.3.6.1.4.1.9.10.112.1.2.1.5";
    public static final String OID_cpwVcFrPMPw2FrOperStatus = ".1.3.6.1.4.1.9.10.112.1.2.1.6";

    public void connectPwAndFrameRelayPvc() throws AbortedException, IOException {
        Map<IntegerKey, IntSnmpEntry> frIfIndexes =
                SnmpUtil.getWalkResult(snmp, OID_cpwVcFrIfIndex, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> frDLCIs =
                SnmpUtil.getWalkResult(snmp, OID_cpwVcFrDlci, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> frAdminStatuses =
                SnmpUtil.getWalkResult(snmp, OID_cpwVcFrAdminStatus, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> frOperStatuses =
                SnmpUtil.getWalkResult(snmp, OID_cpwVcFrOperStatus, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> frPw2FrOperStatuses =
                SnmpUtil.getWalkResult(snmp, OID_cpwVcFrPw2FrOperStatus, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : frIfIndexes.keySet()) {
            int vcIndex = key.getInt();

            int frIfIndex = frIfIndexes.get(key).intValue();
            int frDLCI = frDLCIs.get(key).intValue();
            log.debug("vc[" + vcIndex + "]->Interface[" + frIfIndex + "]/DLCI[" + frDLCI + "]");

            int adminStatus = frAdminStatuses.get(key).intValue();
            String frPwAdminStatus = "unknown";
            switch (adminStatus) {
                case 1:
                    frPwAdminStatus = "up";
                    break;
                case 2:
                    frPwAdminStatus = "down";
                    break;
            }
            int operStatus = frOperStatuses.get(key).intValue();
            String frPwOperStatus = "unknown";
            switch (operStatus) {
                case 1:
                    frPwOperStatus = "active";
                    break;
                case 2:
                    frPwOperStatus = "inactive";
                    break;
            }
            int pw2FrStatus = frPw2FrOperStatuses.get(key).intValue();
            String frPw2FrStatus = "unknown";
            switch (pw2FrStatus) {
                case 1:
                    frPw2FrStatus = "active";
                    break;
                case 2:
                    frPw2FrStatus = "inactive";
                    break;
            }

            PseudoWirePort pw = device.getPseudoWirePortByVcId(vcIndex);
            if (pw == null) {
                throw new IllegalArgumentException("unknown vcIndex: " + vcIndex);
            }

            Port port = this.device.getPortByIfIndex(frIfIndex);
            FrameRelayFeature frPort = getFrameRelayPort(port);
            FrameRelayDLCIEndPoint pvc = frPort.getEndPoint(frDLCI);
            if (pvc == null) {
                throw new IllegalStateException("no DCLI found: "
                        + frPort.getFullyQualifiedName() + ":DLCI=" + frDLCI);
            }
            pw.setAttachedCircuitPort(pvc);
            pw.setAdminStatus(frPwAdminStatus);
            pw.setOperationalStatus(frPwOperStatus);
            pw.setStatus(frPw2FrStatus);
        }

    }

    private FrameRelayFeature getFrameRelayPort(Port port) {
        FrameRelayFeature frPort = null;
        if (port instanceof FrameRelayFeature) {
            frPort = (FrameRelayFeature) port;
        } else if (port instanceof SerialPort) {
            LogicalPort logical = ((SerialPort) port).getLogicalFeature();
            if (logical instanceof FrameRelayFeature) {
                frPort = (FrameRelayFeature) logical;
            }
        }
        if (frPort == null) {
            throw new IllegalStateException("no FrameRelayPort found: ifindex=" + port.getIfIndex());
        }
        return frPort;
    }

}