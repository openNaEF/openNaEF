package voss.discovery.agent.juniper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.MplsTunnel;
import voss.model.MplsVlanDevice;
import voss.model.PseudoWireOperStatus;
import voss.model.PseudoWirePort;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class JuniperVpnMibImpl {

    public static final String jnxVpnMIB = JuniperSmi.jnxMibs + ".26";

    public static final String jnxVpnMibObjects = jnxVpnMIB + ".1";
    public static final String jnxVpnTable = jnxVpnMibObjects + ".2";
    public static final String jnxVpnEntry = jnxVpnTable + ".1";
    public static final String jnxVpnRowStatus = jnxVpnEntry + ".3";
    public static final String jnxVpnIdentifierType = jnxVpnEntry + ".6";
    public static final String jnxVpnIdentifier = jnxVpnEntry + ".7";
    public static final String jnxVpnIfTable = jnxVpnMibObjects + ".3";
    public static final String jnxVpnIfEntry = jnxVpnIfTable + ".1";
    public static final String jnxVpnIfRowStatus = jnxVpnIfEntry + ".4";
    public static final String jnxVpnIfStatus = jnxVpnIfEntry + ".10";
    public static final String jnxVpnPwTable = jnxVpnMibObjects + ".4";
    public static final String jnxVpnPwEntry = jnxVpnPwTable + ".1";
    public static final String jnxVpnRemotePeIdAddress = jnxVpnPwEntry + ".10";
    public static final String jnxVpnPwTunnelName = jnxVpnPwEntry + ".12";
    public static final String jnxVpnPwStatus = jnxVpnPwEntry + ".15";


    private enum JnxVpnType {
        other(1),
        bgpIpVpn(2),
        bgpL2Vpn(3),
        bgpVpls(4),
        l2Circuit(5),
        ldpVpls(6),
        opticalVpn(7),
        vpOxc(8),
        ccc(9),
        bgpAtmVpn(10);
        private final int value;

        JnxVpnType(int value) {
            this.value = value;
        }

        public static JnxVpnType get(int value) {
            for (JnxVpnType e : JnxVpnType.values()) {
                if (e.value == value) {
                    return e;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(JuniperVpnMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public JuniperVpnMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
    }

    public String getOperationalStatus(String vpnName) throws IOException, AbortedException {
        Map<OidKey, IntSnmpEntry> jnxVpnRowStatusMap =
                SnmpUtil.getWalkResult(snmp, jnxVpnRowStatus, intEntryBuilder, oidKeyCreator);
        for (OidKey oidKey : jnxVpnRowStatusMap.keySet()) {
            String name = "";
            for (int i = 0; i < oidKey.getInt(1); i++) {
                name += (char) oidKey.getInt(i + 2);
            }
            int rowStatus = jnxVpnRowStatusMap.get(oidKey).intValue();
            if (name.equals(vpnName) && rowStatus == 1) {
                return "up";
            }
        }
        return "down";
    }

    public void setPseudoWireStatusAndRemoteAddress() throws IOException, AbortedException {
        final Map<OidKey, StringSnmpEntry> jnxVpnRemotePeIdAddressMap =
                SnmpUtil.getWalkResult(snmp, jnxVpnRemotePeIdAddress, stringEntryBuilder, oidKeyCreator);
        final Map<OidKey, IntSnmpEntry> jnxVpnIfStatusMap =
                SnmpUtil.getWalkResult(snmp, jnxVpnIfStatus, intEntryBuilder, oidKeyCreator);
        final Map<String, Integer> vpnIfStatus = new HashMap<String, Integer>();

        for (Map.Entry<OidKey, IntSnmpEntry> entry : jnxVpnIfStatusMap.entrySet()) {
            String name = toName(entry.getKey());
            Integer status = Integer.valueOf(entry.getValue().intValue());
            vpnIfStatus.put(name, status);
        }

        for (OidKey oidKey : jnxVpnRemotePeIdAddressMap.keySet()) {
            JnxVpnType type = JnxVpnType.get(oidKey.getInt(0));
            if (type == null || type != JnxVpnType.bgpL2Vpn) {
                continue;
            }
            String name = "";
            for (int i = 0; i < oidKey.getInt(1); i++) {
                name += (char) oidKey.getInt(i + 2);
            }
            PseudoWirePort pw = device.getPseudoWirePortByName(name);
            if (pw == null) {
                throw new IllegalStateException("PseudoWirePort '" + name + "' is not registered.");
            }

            String remotePe = jnxVpnRemotePeIdAddressMap.get(oidKey).getValue();
            String keyName = toName(oidKey);
            Integer _status = vpnIfStatus.get(keyName);
            int status;
            if (_status == null) {
                status = 0;
                log.debug("no oper-status found, so set 0. [" + keyName + "]");
            } else {
                status = _status.intValue();
            }
            pw.setPeerIpAddress(InetAddress.getByName(remotePe));
            PseudoWireOperStatus pwStatus = null;
            switch (status) {
                case 0:
                    pwStatus = PseudoWireOperStatus.unknown;
                    break;
                case 1:
                    pwStatus = PseudoWireOperStatus.noLocalInterface;
                    break;
                case 2:
                    pwStatus = PseudoWireOperStatus.disabled;
                    break;
                case 3:
                    pwStatus = PseudoWireOperStatus.encapsulationMismatch;
                    break;
                case 4:
                    pwStatus = PseudoWireOperStatus.down;
                    break;
                case 5:
                    pwStatus = PseudoWireOperStatus.up;
                    break;
                default:
                    log.warn("unexpected pseudowire oper-status value: " + name + " -> " + status);
            }
            pw.setPseudoWireOperStatus(pwStatus);
        }
    }

    public void setLowerLayerLsp() throws IOException, AbortedException {
        Map<OidKey, StringSnmpEntry> jnxVpnPwTunnelNameMap =
                SnmpUtil.getWalkResult(snmp, jnxVpnPwTunnelName, stringEntryBuilder, oidKeyCreator);
        for (OidKey oidKey : jnxVpnPwTunnelNameMap.keySet()) {
            JnxVpnType type = JnxVpnType.get(oidKey.getInt(0));
            if (type == null || type != JnxVpnType.bgpL2Vpn) {
                continue;
            }
            String name = "";
            for (int i = 0; i < oidKey.getInt(1); i++) {
                name += (char) oidKey.getInt(i + 2);
            }
            PseudoWirePort pw = device.getPseudoWirePortByName(name);
            if (pw == null) {
                throw new IllegalStateException("PseudoWirePort '" + name + "' is not registered.");
            }
            String tunnelName = jnxVpnPwTunnelNameMap.get(oidKey).getValue();
            MplsTunnel tunnel = (MplsTunnel) device.getPortByIfName(tunnelName);
            if (tunnel == null) {
                throw new IllegalStateException("MplsTunnel '" + tunnelName + "' is not registered.");
            }
            pw.setTransmitLsp(tunnel);
        }
    }

    private String toName(OidKey oidKey) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (oidKey.getInt(1) - 1); i++) {
            sb.append((char) oidKey.getInt(i + 2));
        }
        String name = sb.toString();
        return name;
    }

}