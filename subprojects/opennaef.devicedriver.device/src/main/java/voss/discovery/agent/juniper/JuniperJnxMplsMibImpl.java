package voss.discovery.agent.juniper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.LspHopSeries;
import voss.model.MplsTunnel;
import voss.model.MplsVlanDevice;
import voss.model.PseudoWireOperStatus;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class JuniperJnxMplsMibImpl implements JuniperJnxMplsMib {
    private static final Logger log = LoggerFactory.getLogger(JuniperJnxMplsMibImpl.class);
    private final MplsVlanDevice device;
    private final SnmpAccess snmp;

    public JuniperJnxMplsMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        if (device == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
        this.device = device;
    }

    public void getMplsConfiguration() throws IOException, AbortedException {
        Map<JnxMplsLspListKey, SnmpEntry> mplsLspNames =
                SnmpUtil.getWalkResult(snmp, mplsLspName, defaultEntryBuilder, jnxMplsLspListKeyCreator);
        Map<JnxMplsLspListKey, IntSnmpEntry> mplsLspStates =
                SnmpUtil.getWalkResult(snmp, mplsLspState, intEntryBuilder, jnxMplsLspListKeyCreator);
        Map<JnxMplsLspListKey, IpAddressSnmpEntry> mplsLspFroms =
                SnmpUtil.getWalkResult(snmp, mplsLspFrom, ipAddressEntryBuilder, jnxMplsLspListKeyCreator);
        Map<JnxMplsLspListKey, IpAddressSnmpEntry> mplsLspTos =
                SnmpUtil.getWalkResult(snmp, mplsLspTo, ipAddressEntryBuilder, jnxMplsLspListKeyCreator);
        Map<JnxMplsLspListKey, IntSnmpEntry> mplsLspPackets_ =
                SnmpUtil.getWalkResult(snmp, mplsLspPackets, intEntryBuilder, jnxMplsLspListKeyCreator);
        Map<JnxMplsLspListKey, IntSnmpEntry> mplsLspOctets_ =
                SnmpUtil.getWalkResult(snmp, mplsLspOctets, intEntryBuilder, jnxMplsLspListKeyCreator);

        Map<JnxMplsLspListKey, IntSnmpEntry> mplsPathTypes =
                SnmpUtil.getWalkResult(snmp, mplsPathType, intEntryBuilder, jnxMplsLspListKeyCreator);
        Map<JnxMplsLspListKey, StringSnmpEntry> mplsPathExplicitRoutes =
                SnmpUtil.getWalkResult(snmp, mplsPathExplicitRoute, stringEntryBuilder, jnxMplsLspListKeyCreator);
        Map<JnxMplsLspListKey, StringSnmpEntry> mplsPathRecordRoutes =
                SnmpUtil.getWalkResult(snmp, mplsPathRecordRoute, stringEntryBuilder, jnxMplsLspListKeyCreator);
        Map<JnxMplsLspListKey, IntSnmpEntry> mplsPathProperties_ =
                SnmpUtil.getWalkResult(snmp, mplsPathProperties, intEntryBuilder, jnxMplsLspListKeyCreator);

        for (JnxMplsLspListKey key : mplsLspNames.keySet()) {
            String name = getNameWithoutTrailingZeros(mplsLspNames.get(key).value);
            int statusId = mplsLspStates.get(key).intValue();
            PseudoWireOperStatus status;
            switch (statusId) {
                case 1:
                    status = PseudoWireOperStatus.unknown;
                    break;
                case 2:
                    status = PseudoWireOperStatus.up;
                    break;
                case 3:
                    status = PseudoWireOperStatus.down;
                    break;
                default:
                    status = PseudoWireOperStatus.undefined;
            }
            String from = mplsLspFroms.get(key).getIpAddress();
            String to = mplsLspTos.get(key).getIpAddress();
            long packets = mplsLspPackets_.get(key).longValue();
            long octets = mplsLspOctets_.get(key).longValue();

            MplsTunnel tunnel = new MplsTunnel();
            tunnel.initDevice(device);
            device.addTeTunnel(tunnel);
            tunnel.initIfName(name);
            tunnel.setStatus(status.toString());
            tunnel.setFrom(from);
            tunnel.setTo(to);

            int pathProperties = mplsPathProperties_.get(key).intValue();
            for (int i = 1; i < 128; i = i * 2) {
                int property = pathProperties & i;
                switch (property) {
                    case 0:
                        continue;
                    case 1:
                        tunnel.setRecordRoute(true);
                        break;
                    case 2:
                        break;
                    case 4:
                        break;
                    case 8:
                        tunnel.setMergePermitted(true);
                        break;
                    case 16:
                        break;
                    case 32:
                        break;
                    case 64:
                        tunnel.setFastReroute(true);
                        break;

                    default:
                        throw new IllegalArgumentException("unkwnon option: " + property);
                }
            }

            int pathTypeId = mplsPathTypes.get(key).intValue();
            JnxMplsPathType pathType = JnxMplsPathType.getById(pathTypeId);
            String recordRoute = mplsPathRecordRoutes.get(key).getValue();
            String explicitRoute = mplsPathExplicitRoutes.get(key).getValue();
            LspHopSeries lsp = getLspHopSeries(recordRoute);
            tunnel.putLspHopSeries(BigInteger.ZERO, lsp);

            log.trace("tunnel found: " + key + "\t" + name + " (" + pathType
                    + ")" + " " + status + " [" + from + "]->[" + to + "] "
                    + packets + " packets," + octets + " octets\r\n"
                    + "\texplicit route=" + explicitRoute + "\r\n"
                    + "\trecord route=" + recordRoute);
        }
    }

    private LspHopSeries getLspHopSeries(String hopString) {
        log.debug("hopString=" + hopString);
        LspHopSeries lsp = new LspHopSeries();
        String[] hops = hopString.split(" ");
        for (String hop : hops) {
            hop = hop.trim();
            log.debug("- [" + hop + "]");
            Matcher matcher = hopPattern.matcher(hop);
            if (matcher.matches()) {
                hop = matcher.group(1);
                log.debug("-- " + "[" + hop + "]");
                lsp.addHop(hop);
            }
        }
        return lsp;
    }

    private final Pattern hopPattern = Pattern.compile("([0-9\\.:]+)(\\()?.*(\\))?");

    private static String getNameWithoutTrailingZeros(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            if (b == 0x00) {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }

    private final KeyCreator<JnxMplsLspListKey> jnxMplsLspListKeyCreator =
            new KeyCreator<JnxMplsLspListKey>() {
                private static final long serialVersionUID = 1L;

                public JnxMplsLspListKey getKey(BigInteger[] oidSuffix) {
                    return new JnxMplsLspListKey(oidSuffix);
                }

            };

    private static class JnxMplsLspListKey {
        public final String name;

        public JnxMplsLspListKey(BigInteger[] oidSuffix) {
            if (oidSuffix == null || oidSuffix.length == 0) {
                throw new IllegalArgumentException();
            }
            StringBuffer sb = new StringBuffer();
            for (BigInteger bi : oidSuffix) {
                if (bi.intValue() == 0) {
                    continue;
                }
                sb.append((char) bi.intValue());
            }
            this.name = sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof JnxMplsLspListKey) {
                return this.name.equals(((JnxMplsLspListKey) o).name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int i = this.name.hashCode();
            return i * i + i + 41;
        }

        @Override
        public String toString() {
            return "JnxMplsLspListKey:" + this.name;
        }
    }

}