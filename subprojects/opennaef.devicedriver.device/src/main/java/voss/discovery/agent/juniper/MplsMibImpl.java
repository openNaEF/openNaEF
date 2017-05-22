package voss.discovery.agent.juniper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.LabelSwitchedPathEndPoint;
import voss.model.MplsTunnel;
import voss.model.MplsVlanDevice;
import voss.model.PseudoWireOperStatus;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class MplsMibImpl {

    public static final String mpls = JuniperSmi.jnxMibs + ".2";

    public static final String mplsLspInfoList = mpls + ".5";
    public static final String mplsLspInfoEntry = mplsLspInfoList + ".1";
    public static final String mplsLspInfoState = mplsLspInfoEntry + ".2";
    public static final String mplsLspInfoFrom = mplsLspInfoEntry + ".15";
    public static final String mplsLspInfoTo = mplsLspInfoEntry + ".16";
    public static final String mplsPathInfoName = mplsLspInfoEntry + ".17";

    private enum MplsLspInfoState {
        unknown(1),
        up(2),
        down(3),
        notInService(4),
        backupActive(5);
        final int value;

        MplsLspInfoState(int value) {
            this.value = value;
        }

        public static MplsLspInfoState get(int value) {
            for (MplsLspInfoState mplsLspInfoState : MplsLspInfoState.values()) {
                if (mplsLspInfoState.value == value) {
                    return mplsLspInfoState;
                }
            }
            throw new IllegalStateException();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(MplsMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public MplsMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
    }

    public void setupLsp() throws IOException, AbortedException {

        Map<OidKey, IntSnmpEntry> mplsLspInfoStateMap =
                SnmpUtil.getWalkResult(snmp, mplsLspInfoState, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IpAddressSnmpEntry> mplsLspInfoFromMap =
                SnmpUtil.getWalkResult(snmp, mplsLspInfoFrom, ipAddressEntryBuilder, oidKeyCreator);
        Map<OidKey, IpAddressSnmpEntry> mplsLspInfoToMap =
                SnmpUtil.getWalkResult(snmp, mplsLspInfoTo, ipAddressEntryBuilder, oidKeyCreator);

        Map<OidKey, StringSnmpEntry> mplsPathInfoNameMap =
                SnmpUtil.getWalkResult(snmp, mplsPathInfoName, stringEntryBuilder, oidKeyCreator);

        for (OidKey key : mplsLspInfoStateMap.keySet()) {

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < key.getOID().length; i++) {
                sb.append((char) key.getInt(i));
            }
            String lspInfoName = sb.toString();

            MplsLspInfoState lspInfoState = MplsLspInfoState.get(mplsLspInfoStateMap.get(key).intValue());
            PseudoWireOperStatus status;
            switch (lspInfoState) {
                case unknown:
                    status = PseudoWireOperStatus.unknown;
                    break;
                case up:
                    status = PseudoWireOperStatus.up;
                    break;
                case down:
                    status = PseudoWireOperStatus.down;
                    break;
                case notInService:
                    status = PseudoWireOperStatus.down;
                    break;
                case backupActive:
                    status = PseudoWireOperStatus.up;
                    break;
                default:
                    status = PseudoWireOperStatus.undefined;
            }

            String lspInfoFrom = mplsLspInfoFromMap.get(key).getIpAddress();
            String lspInfoTo = mplsLspInfoToMap.get(key).getIpAddress();

            log.debug("Found LSP.");
            log.debug("\tlspInfoName(ifName)=" + lspInfoName);
            log.debug("\tlspInfoState=" + lspInfoState);
            log.debug("\tlspInfoFrom=" + lspInfoFrom);
            log.debug("\tlspInfoTo=" + lspInfoTo);

            MplsTunnel mplsTunnel = (MplsTunnel) device.getPortByIfName(lspInfoName);
            if (mplsTunnel == null) {
                mplsTunnel = new MplsTunnel();
                mplsTunnel.initDevice(device);
                mplsTunnel.initIfName(lspInfoName);
            }
            mplsTunnel.setStatus(status.toString());
            mplsTunnel.setFrom(lspInfoFrom);
            mplsTunnel.setTo(lspInfoTo);

            String pathInfoName = mplsPathInfoNameMap.get(key).getValue();
            if (!pathInfoName.equals("")) {
                log.debug("\tActive pathInfoName=" + pathInfoName);
                LabelSwitchedPathEndPoint lsp =
                        (LabelSwitchedPathEndPoint) device.getPortByIfName(lspInfoName + "::" + pathInfoName);
                mplsTunnel.setActiveHops(lsp);
                lsp.setOperationalStatus("active");
                lsp.setLspStatus(true);
            }
        }
    }

    public void setStatus() throws IOException, AbortedException {
        Map<OidKey, IntSnmpEntry> mplsLspInfoStateMap =
                SnmpUtil.getWalkResult(snmp, mplsLspInfoState, intEntryBuilder, oidKeyCreator);
        Map<OidKey, StringSnmpEntry> mplsPathInfoNameMap =
                SnmpUtil.getWalkResult(snmp, mplsPathInfoName, stringEntryBuilder, oidKeyCreator);

        for (OidKey key : mplsLspInfoStateMap.keySet()) {

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < key.getOID().length; i++) {
                sb.append((char) key.getInt(i));
            }
            String lspInfoName = sb.toString();

            MplsLspInfoState lspInfoState = MplsLspInfoState.get(mplsLspInfoStateMap.get(key).intValue());
            PseudoWireOperStatus status;
            switch (lspInfoState) {
                case unknown:
                    status = PseudoWireOperStatus.unknown;
                    break;
                case up:
                    status = PseudoWireOperStatus.up;
                    break;
                case down:
                    status = PseudoWireOperStatus.down;
                    break;
                case notInService:
                    status = PseudoWireOperStatus.down;
                    break;
                case backupActive:
                    status = PseudoWireOperStatus.up;
                    break;
                default:
                    status = PseudoWireOperStatus.undefined;
            }

            log.debug("Found LSP.");
            log.debug("\tlspInfoName=" + lspInfoName);
            log.debug("\tlspInfoState=" + lspInfoState);
            MplsTunnel mplsTunnel = (MplsTunnel) device.getPortByIfName(lspInfoName);
            if (mplsTunnel == null) {
                mplsTunnel = new MplsTunnel();
                mplsTunnel.initDevice(device);
                mplsTunnel.initIfName(lspInfoName);
            }
            mplsTunnel.setStatus(status.toString());

            String pathInfoName = mplsPathInfoNameMap.get(key).getValue();
            if (!pathInfoName.equals("")) {
                LabelSwitchedPathEndPoint lsp =
                        (LabelSwitchedPathEndPoint) device.getPortByIfName(lspInfoName + "::" + pathInfoName);
                mplsTunnel.setActiveHops(lsp);
                lsp.setOperationalStatus("active");
                lsp.setLspStatus(true);
                log.debug("\tActive pathInfoName=" + pathInfoName);
            }
        }
    }
}