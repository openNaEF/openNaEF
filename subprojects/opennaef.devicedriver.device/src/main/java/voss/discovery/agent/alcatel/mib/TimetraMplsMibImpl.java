package voss.discovery.agent.alcatel.mib;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.agent.alcatel.mib.TimetraTcMib.TmnxAdminState;
import voss.discovery.agent.alcatel.mib.TimetraTcMib.TmnxOperState;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class TimetraMplsMibImpl implements TimetraMplsMib {

    private static final Logger log = LoggerFactory.getLogger(TimetraMplsMibImpl.class);
    private final MplsVlanDevice device;
    private final SnmpAccess snmp;

    private final MplsTeMibImpl mplsTeMib;

    public TimetraMplsMibImpl(Alcatel7710SRDiscovery discovery) {

        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDeviceInner();

        this.mplsTeMib = new MplsTeMibImpl(snmp, device);
    }

    public void getMplsConfiguration() throws IOException, AbortedException {

        Map<String, StringSnmpEntry> vRtrMplsLspNameMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspName, stringEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspOperStateMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspOperState, intEntryBuilder, stringKeyCreator);
        Map<String, IpAddressSnmpEntry> vRtrMplsLspFromAddrMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspFromAddr, ipAddressEntryBuilder, stringKeyCreator);
        Map<String, IpAddressSnmpEntry> vRtrMplsLspToAddrMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspToAddr, ipAddressEntryBuilder, stringKeyCreator);

        Map<String, IntSnmpEntry> vRtrMplsLspPathTypeMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathType, intEntryBuilder, stringKeyCreator);
        Map<String, SnmpUtil.ByteSnmpEntry> vRtrMplsLspPathPropertiesMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathProperties, SnmpHelper.byteEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspPathStateMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathState, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspPathBandwidthMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathBandwidth, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspPathAdminStateMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathAdminState, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspPathOperStateMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathOperState, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspPathTunnelARHopListIndexMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathTunnelARHopListIndex, intEntryBuilder, stringKeyCreator);

        Map<String, IntSnmpEntry> vRtrMplsLspConfiguredPathsMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspConfiguredPaths, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspStandbyPathsMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspStandbyPaths, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspOperationalPathsMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspOperationalPaths, intEntryBuilder, stringKeyCreator);

        for (String key : vRtrMplsLspNameMap.keySet()) {

            String name = vRtrMplsLspNameMap.get(key).getValue();

            TmnxOperState lspOperState = TmnxOperState.get(vRtrMplsLspOperStateMap.get(key).intValue());
            PseudoWireOperStatus status = getLspOperStatus(lspOperState);

            String from = vRtrMplsLspFromAddrMap.get(key).getIpAddress();
            String to = vRtrMplsLspToAddrMap.get(key).getIpAddress();

            MplsTunnel tunnel = new MplsTunnel();
            tunnel.initDevice(device);
            tunnel.initIfName(name);
            tunnel.setStatus(status.toString());
            tunnel.setFrom(from);
            tunnel.setTo(to);
            device.addTeTunnel(tunnel);

            int configuredPaths = vRtrMplsLspConfiguredPathsMap.get(key).intValue();
            int standbyPaths = vRtrMplsLspStandbyPathsMap.get(key).intValue();
            int operationalPaths = vRtrMplsLspOperationalPathsMap.get(key).intValue();

            log.debug(key + " Paths configured=" + configuredPaths + " standby=" + standbyPaths + " operational=" + operationalPaths);

            for (String key2 : vRtrMplsLspPathStateMap.keySet()) {

                if (key2.startsWith(key + ".")) {

                    String key3 = key2.substring(key.length());

                    VRtrMplsLspPathType pathType = VRtrMplsLspPathType.get(vRtrMplsLspPathTypeMap.get(key2).intValue());
                    int pathBandwidth = vRtrMplsLspPathBandwidthMap.get(key2).intValue();
                    VRtrMplsLspPathState pathState = VRtrMplsLspPathState.get(vRtrMplsLspPathStateMap.get(key2).intValue());
                    TmnxAdminState pathAdminState = TmnxAdminState.get(vRtrMplsLspPathAdminStateMap.get(key2).intValue());
                    TmnxOperState pathOperState = TmnxOperState.get(vRtrMplsLspPathOperStateMap.get(key2).intValue());

                    String tunnelName = mplsTeMib.getTunnelName(key3);
                    if (tunnelName == null) {
                        continue;
                    }
                    int tunnelInstancePriority = mplsTeMib.getTunnelInstancePriority(key3);

                    LabelSwitchedPathEndPoint lsp = new LabelSwitchedPathEndPoint();
                    lsp.initDevice(device);
                    lsp.initIfName(name + "::" + tunnelName);
                    lsp.setLspName(tunnelName);
                    lsp.setLspStatus(pathOperState == TmnxOperState.inService);
                    lsp.setStatus(pathOperState.toString());
                    lsp.setInOperation(pathState == VRtrMplsLspPathState.active);
                    lsp.setAdminStatus(pathAdminState.toString());
                    lsp.setOperationalStatus(pathState.toString());
                    lsp.setBandwidth(1000000L * pathBandwidth);
                    log.debug("Add LabelSwitchedPathEndPoint " + name + "::" + tunnelName);

                    log.debug("\t" + key3 + " \"" + tunnelName + "\" priority=" + tunnelInstancePriority);
                    log.debug("\t" + pathOperState + " / " + pathState + " / " + pathType + " / " + pathBandwidth + "Mbps");

                    int index = vRtrMplsLspPathTunnelARHopListIndexMap.get(key2).intValue();
                    tunnel.addMemberLsp(index, lsp);
                    log.debug("\thop index " + index + " (" + tunnel.getMemberLsps().size() + ")");

                    if (pathState == VRtrMplsLspPathState.active) {
                        log.debug("setActiveHops " + tunnelName);
                        tunnel.setActiveHops(lsp);
                    }

                    LspHopSeries hops = mplsTeMib.lspHopSeries(index);
                    if (index == 0) {
                        hops = mplsTeMib.getMplsTunnelHop(key3);
                    }
                    for (String hop : hops.getHops()) {
                        lsp.addLspHop(hop);
                    }

                    if (lsp.getInOperation()) {
                        byte bytes[] = vRtrMplsLspPathPropertiesMap.get(key2).getValue();
                        int[] flags = SnmpUtil.decodeBitList(bytes);
                        for (int flag : flags) {
                            switch (flag - 1) {
                                case 0:
                                    tunnel.setRecordRoute(true);
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    tunnel.setMergePermitted(true);
                                    break;
                                case 4:
                                    tunnel.setFastReroute(true);
                                    break;
                                default:
                                    throw new IllegalStateException("unknown attribute value: " + flag);
                            }
                        }

                        tunnel.putLspHopSeries(BigInteger.ZERO, hops);
                    }
                }
            }

            log.trace("tunnel found: " + key + "\t" + name + " ("
                            + ")" + " " + status + " [" + from + "]->[" + to + "] "
            );
        }
    }

    public void getMplsStatus() throws IOException, AbortedException {

        Map<String, StringSnmpEntry> vRtrMplsLspNameMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspName, stringEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspOperStateMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspOperState, intEntryBuilder, stringKeyCreator);

        Map<String, IntSnmpEntry> vRtrMplsLspPathStateMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathState, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> vRtrMplsLspPathOperStateMap =
                SnmpUtil.getWalkResult(snmp, vRtrMplsLspPathOperState, intEntryBuilder, stringKeyCreator);

        for (String key : vRtrMplsLspNameMap.keySet()) {
            String name = vRtrMplsLspNameMap.get(key).getValue();
            TmnxOperState lspOperState = TmnxOperState.get(vRtrMplsLspOperStateMap.get(key).intValue());
            PseudoWireOperStatus status = getLspOperStatus(lspOperState);

            MplsTunnel tunnel = new MplsTunnel();
            tunnel.initDevice(device);
            tunnel.initIfName(name);
            tunnel.setStatus(status.toString());
            device.addTeTunnel(tunnel);

            for (String key2 : vRtrMplsLspPathStateMap.keySet()) {
                if (key2.startsWith(key + ".")) {
                    String key3 = key2.substring(key.length());

                    VRtrMplsLspPathState pathState = VRtrMplsLspPathState.get(vRtrMplsLspPathStateMap.get(key2).intValue());
                    TmnxOperState pathOperState = TmnxOperState.get(vRtrMplsLspPathOperStateMap.get(key2).intValue());
                    String tunnelName = mplsTeMib.getTunnelName(key3);

                    LabelSwitchedPathEndPoint lsp = new LabelSwitchedPathEndPoint();
                    lsp.initDevice(device);
                    lsp.initIfName(name + "::" + tunnelName);
                    lsp.setLspName(tunnelName);
                    lsp.setLspStatus(pathOperState == TmnxOperState.inService);
                    lsp.setStatus(pathOperState.toString());
                    lsp.setInOperation(pathState == VRtrMplsLspPathState.active);
                    lsp.setOperationalStatus(pathState.toString());
                    int index = tunnel.getMemberLsps().size() + 1;
                    tunnel.addMemberLsp(index, lsp);
                    log.debug("Add LabelSwitchedPathEndPoint " + name + "::" + tunnelName);
                    log.debug("\t" + key3 + " \"" + tunnelName + "\" / " + pathOperState + " / " + pathState);
                    log.debug("\thop index " + index + " (" + tunnel.getMemberLsps().size() + ")");

                    if (pathState == VRtrMplsLspPathState.active) {
                        log.debug("setActiveHops " + tunnelName);
                        tunnel.setActiveHops(lsp);
                    }
                }
            }
            log.trace("tunnel found: " + key + "\t" + name + " " + status);
        }
    }

    private PseudoWireOperStatus getLspOperStatus(TmnxOperState lspOperState) {
        PseudoWireOperStatus status;

        switch (lspOperState) {
            case unknown:
                status = PseudoWireOperStatus.unknown;
                break;
            case inService:
                status = PseudoWireOperStatus.up;
                break;
            case outOfService:
                status = PseudoWireOperStatus.down;
                break;
            case transition:
                status = PseudoWireOperStatus.undefined;
                break;
            default:
                throw new IllegalArgumentException();
        }
        return status;
    }

    public String getMplsTunnelName(int lspId) throws IOException, AbortedException {
        try {
            return SnmpUtil.getString(snmp, vRtrMplsLspName + ".1." + lspId);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    private final KeyCreator<String> stringKeyCreator =
            new KeyCreator<String>() {
                private static final long serialVersionUID = 1L;

                public String getKey(BigInteger[] oidSuffix) {
                    String key = "";
                    for (BigInteger bi : oidSuffix) {
                        key += "." + bi.toString();
                    }
                    return key;
                }
            };
}