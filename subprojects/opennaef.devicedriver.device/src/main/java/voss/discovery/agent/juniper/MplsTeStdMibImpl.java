package voss.discovery.agent.juniper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.LabelSwitchedPathEndPoint;
import voss.model.MplsTunnel;
import voss.model.MplsVlanDevice;
import voss.model.PseudoWireOperStatus;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class MplsTeStdMibImpl {
    public static final String mplsStdMIB = ".1.3.6.1.2.1.10.166";

    public static final String mplsTeStdMIB = mplsStdMIB + ".3";
    public static final String mplsTeObjects = mplsTeStdMIB + ".2";
    public static final String mplsTunnelTable = mplsTeObjects + ".2";
    public static final String mplsTunnelEntry = mplsTunnelTable + ".1";
    public static final String mplsTunnelName = mplsTunnelEntry + ".5";
    public static final String mplsTunnelRole = mplsTunnelEntry + ".10";
    public static final String mplsTunnelSessionAttributes = mplsTunnelEntry + ".15";
    public static final String mplsTunnelPrimaryInstance = mplsTunnelEntry + ".18";
    public static final String mplsTunnelARHopTableIndex = mplsTunnelEntry + ".22";
    public static final String mplsTunnelAdminStatus = mplsTunnelEntry + ".34";
    public static final String mplsTunnelOperStatus = mplsTunnelEntry + ".35";
    public static final String mplsTunnelARHopTable = mplsTeObjects + ".7";
    public static final String mplsTunnelARHopEntry = mplsTunnelARHopTable + ".1";
    public static final String mplsTunnelARHopAddrType = mplsTunnelARHopEntry + ".3";
    public static final String mplsTunnelARHopIpAddr = mplsTunnelARHopEntry + ".4";

    private enum MplsTunnelAdminStatus {
        up(1),
        down(2),
        testing(3);
        private int value;

        MplsTunnelAdminStatus(int value) {
            this.value = value;
        }

        public static MplsTunnelAdminStatus get(int value) {
            for (MplsTunnelAdminStatus adminStatus : MplsTunnelAdminStatus.values()) {
                if (adminStatus.value == value) {
                    return adminStatus;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    private enum MplsTunnelOperStatus {
        up(1),
        down(2),
        testing(3),
        unknown(4),
        dormant(5),
        notPresent(6),
        lowerLayerDown(7);
        int value;

        MplsTunnelOperStatus(int value) {
            this.value = value;
        }

        public static MplsTunnelOperStatus get(int value) {
            for (MplsTunnelOperStatus operStatus : MplsTunnelOperStatus.values()) {
                if (operStatus.value == value) {
                    return operStatus;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    private enum MplsTunnelRole {
        head(1),
        transit(2),
        tail(3),
        headTail(4);
        int value;

        MplsTunnelRole(int value) {
            this.value = value;
        }

        public static MplsTunnelRole get(int value) {
            for (MplsTunnelRole role : MplsTunnelRole.values()) {
                if (role.value == value) {
                    return role;
                }
            }
            throw new IllegalArgumentException();
        }
    }


    private static final Logger log = LoggerFactory.getLogger(MplsTeStdMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public MplsTeStdMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
    }

    public void getMplsTunnel() throws IOException, AbortedException {

        Map<OidKey, StringSnmpEntry> mplsTunnelNameMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelName, stringEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelPrimaryInstanceMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelPrimaryInstance, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelAdminStatusMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelAdminStatus, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelOperStatusMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelOperStatus, intEntryBuilder, oidKeyCreator);

        for (OidKey key : mplsTunnelNameMap.keySet()) {
            int index = key.getInt(0);
            int instance = key.getInt(1);
            String name = mplsTunnelNameMap.get(key).getValue();
            int primaryInstance = mplsTunnelPrimaryInstanceMap.get(key).intValue();
            MplsTunnelAdminStatus adminStatus = MplsTunnelAdminStatus.get(mplsTunnelAdminStatusMap.get(key).intValue());
            MplsTunnelOperStatus operStatus = MplsTunnelOperStatus.get(mplsTunnelOperStatusMap.get(key).intValue());

            log.debug(name);
            log.debug("\tindex=" + index + ", instance=" + instance + ", primary=" + primaryInstance);
            log.debug("\tadminStatus=" + adminStatus + ", operStatus=" + operStatus);
        }
    }

    public void setupMplsTunnel(String lspName, String pathName, boolean primary) throws IOException, AbortedException {

        Map<OidKey, StringSnmpEntry> mplsTunnelNameMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelName, stringEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelPrimaryInstanceMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelPrimaryInstance, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelAdminStatusMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelAdminStatus, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelOperStatusMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelOperStatus, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelRoleMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelRole, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelSessionAttributesMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelSessionAttributes, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> mplsTunnelARHopTableIndexMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelARHopTableIndex, intEntryBuilder, oidKeyCreator);

        for (OidKey key : mplsTunnelNameMap.keySet()) {
            int instance = key.getInt(1);

            String name = mplsTunnelNameMap.get(key).getValue();
            MplsTunnelRole role = MplsTunnelRole.get(mplsTunnelRoleMap.get(key).intValue());

            if (!name.equals(lspName)) {
                continue;
            } else if (role != MplsTunnelRole.head) {
                continue;
            }

            int primaryInstance = mplsTunnelPrimaryInstanceMap.get(key).intValue();
            if ((instance == primaryInstance) == primary) {

                LabelSwitchedPathEndPoint lsp = (LabelSwitchedPathEndPoint) device.getPortByIfName(lspName + "::" + pathName);
                MplsTunnelAdminStatus adminStatus = MplsTunnelAdminStatus.get(mplsTunnelAdminStatusMap.get(key).intValue());
                MplsTunnelOperStatus operStatus = MplsTunnelOperStatus.get(mplsTunnelOperStatusMap.get(key).intValue());
                lsp.setAdminStatus(adminStatus.name());
                lsp.setOperationalStatus("inactive");
                PseudoWireOperStatus status;
                switch (operStatus) {
                    case up:
                        status = PseudoWireOperStatus.up;
                        break;
                    case down:
                        status = PseudoWireOperStatus.down;
                        break;
                    case testing:
                        status = PseudoWireOperStatus.testing;
                        break;
                    case unknown:
                        status = PseudoWireOperStatus.unknown;
                        break;
                    case dormant:
                        status = PseudoWireOperStatus.dormant;
                        break;
                    case notPresent:
                        status = PseudoWireOperStatus.notPresent;
                        break;
                    case lowerLayerDown:
                        status = PseudoWireOperStatus.lowerLayerDown;
                        break;
                    default:
                        throw new IllegalStateException();
                }
                lsp.setStatus(status.toString());

                int hopTableIndex = mplsTunnelARHopTableIndexMap.get(key).intValue();
                String s = lsp.getIfName();
                log.debug(s + " (" + hopTableIndex + ")");
                Map<OidKey, IntSnmpEntry> mplsTunnelARHopAddrTypeMap =
                        SnmpUtil.getWalkResult(snmp, mplsTunnelARHopAddrType + "." + hopTableIndex, intEntryBuilder, oidKeyCreator);
                Map<OidKey, ByteSnmpEntry> mplsTunnelARHopIpAddrMap =
                        SnmpUtil.getWalkResult(snmp, mplsTunnelARHopIpAddr + "." + hopTableIndex, byteEntryBuilder, oidKeyCreator);
                for (int i = 1; i <= mplsTunnelARHopAddrTypeMap.size(); i++) {
                    OidKey key2 = new OidKey(new BigInteger[]{BigInteger.valueOf(i)});
                    int hopAddrType = mplsTunnelARHopAddrTypeMap.get(key2).intValue();
                    if (hopAddrType == 1) {
                        byte[] b = mplsTunnelARHopIpAddrMap.get(key2).value;
                        String hop = (int) (b[0] & 0xff) + "." + (int) (b[1] & 0xff) + "." + (int) (b[2] & 0xff) + "." + (int) (b[3] & 0xff);
                        lsp.addLspHop(hop);
                        log.debug("- " + s + " hop[" + i + "] " + hop);
                    } else {
                        lsp.addLspHop("unknown");
                    }
                }

                MplsTunnel tunnel = (MplsTunnel) device.getPortByIfName(lspName);

                int sessionAttributes = mplsTunnelSessionAttributesMap.get(key).intValue();
                for (int i = 1; i < 128; i = i * 2) {
                    int attribute = sessionAttributes & i;
                    switch (attribute) {
                        case 0:
                            continue;
                        case 128:
                            tunnel.setFastReroute(true);
                            break;
                        case 64:
                            tunnel.setMergePermitted(true);
                            break;
                        case 32:
                            tunnel.setPersistent(true);
                            break;
                        case 16:
                            tunnel.setRecordRoute(true);
                            break;
                        default:
                            throw new IllegalArgumentException("unkwnon option: " + attribute);
                    }
                }
            }
        }
    }
}