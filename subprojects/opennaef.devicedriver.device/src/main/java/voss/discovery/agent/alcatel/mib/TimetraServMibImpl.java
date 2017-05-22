package voss.discovery.agent.alcatel.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.agent.alcatel.AlcatelExtInfoNames;
import voss.discovery.agent.common.MplsModelBuilder;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;
import voss.model.impl.PortCrossConnectionImpl;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.intEntryBuilder;
import static voss.discovery.iolib.snmp.SnmpHelper.stringEntryBuilder;

public class TimetraServMibImpl implements TimetraServMib {

    private static final Logger log = LoggerFactory.getLogger(TimetraServMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;
    private final MplsModelBuilder builder;

    private final TimetraSdpMibImpl timetraSdpMib;
    private final TimetraSapMibImpl timetraSapMib;
    private final TimetraMplsMibImpl timetraMplsMib;

    public TimetraServMibImpl(Alcatel7710SRDiscovery discovery) throws IOException, AbortedException {

        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDeviceInner();
        this.builder = discovery.getMplsModelBuilder();

        this.timetraSdpMib = discovery.getTimetraSdpMib();
        this.timetraSapMib = new TimetraSapMibImpl(discovery);
        this.timetraMplsMib = discovery.getTimetraMplsMib();
    }

    public void getService() throws IOException, AbortedException {

        Map<String, IntSnmpEntry> svcIdMap =
                SnmpUtil.getWalkResult(snmp, svcId, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> svcTypeMap =
                SnmpUtil.getWalkResult(snmp, svcType, intEntryBuilder, stringKeyCreator);
        Map<String, StringSnmpEntry> svcDescriptionMap =
                SnmpUtil.getWalkResult(snmp, svcDescription, stringEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> svcAdminStatusMap =
                SnmpUtil.getWalkResult(snmp, svcAdminStatus, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> svcOperStatusMap =
                SnmpUtil.getWalkResult(snmp, svcOperStatus, intEntryBuilder, stringKeyCreator);

        for (String key : svcIdMap.keySet()) {
            int svcId = svcIdMap.get(key).intValue();
            SvcType type = SvcType.get(svcTypeMap.get(key).intValue());

            PseudoWireOperStatus adminStatus = getPseudoWireStatus(svcAdminStatusMap, key);
            PseudoWireOperStatus operStatus = getPseudoWireStatus(svcOperStatusMap, key);

            switch (type) {
                case epipe:
                case apipe:
                case fpipe:
                case cpipe:
                    int sdpId = timetraSdpMib.getSdpId(svcId);
                    String descr = svcDescriptionMap.get("." + svcId).getValue();
                    if (sdpId == 0) {
                        timetraSapMib.createNodePipe(svcId, descr);
                        continue;
                    }

                    int vcId = timetraSdpMib.getVcId(svcId);
                    List<Integer> lspIds = timetraSdpMib.getLspList(sdpId);
                    int lspId = lspIds.get(0);
                    InetAddress peer = InetAddress.getByName(timetraSdpMib.getFarEndIpAddress(sdpId));

                    log.debug("svc " + svcId + " type=" + type + " [" + descr + "] sdp=" + sdpId + " lsp=" + lspId + " vc=" + vcId + " farend=" + peer.getHostAddress());

                    builder.buildPseudoWire(vcId);
                    builder.setPseudoWireName(vcId, descr);
                    builder.setPseudoWirePeerAddress(vcId, peer);
                    switch (type) {
                        case epipe:
                            builder.setAlcatelPipeType(vcId, AlcatelPipeType.EPIPE);
                            break;
                        case apipe:
                            builder.setAlcatelPipeType(vcId, AlcatelPipeType.APIPE);
                            break;
                        case cpipe:
                        case fpipe:
                            builder.setAlcatelPipeType(vcId, AlcatelPipeType.CPIPE);
                            break;
                    }
                    builder.setPseudoWireVcIndex(vcId, vcId);
                    MplsTunnel tunnel = (MplsTunnel) device.getPortByIfName(timetraMplsMib.getMplsTunnelName(lspId));
                    builder.setPseudoWireTransmitLsp(vcId, tunnel);

                    builder.setPseudoWireAdminStatus(vcId, adminStatus);
                    builder.setPseudoWireOperStatus(vcId, operStatus);

                    PseudoWirePort pw = device.getPseudoWirePortByPwId(vcId);
                    pw.gainConfigurationExtInfo().put(AlcatelExtInfoNames.CONFIG_PW_SERVICE_ID, String.valueOf(svcId));
                    pw.setAlcatelSdpId(sdpId);
                    pw.setControlWord(timetraSdpMib.getBindOperControlWord(svcId));
                    pw.setBandwidth(timetraSdpMib.getSdpBindOperBandwidth(svcId));

                    int portId = timetraSapMib.getPortBySap(svcId);
                    if (portId != 0) {
                        Port port = device.getPortByIfIndex(portId);
                        if (port == null) {
                            log.debug(" portId=" + portId + " not found. skip setAttachedCircuitPort.");
                            continue;
                        }
                        if (port instanceof EthernetPort) {
                            port = device.getLogicalEthernetPort((EthernetPort) port);
                        }
                        log.debug(" portId=" + portId + " port=" + port.getIfName());

                        pw.setAttachedCircuitPort(port);
                        PortCrossConnection xc = new PortCrossConnectionImpl();
                        xc.initDevice(this.device);
                        xc.addPort(pw);
                        xc.addPort(port);
                        this.device.getPortCrossConnectionEngine().addPortCrossConnection(xc);
                        log.debug("setAttachedCircuitPort " + port.getIfName());
                    } else {
                        log.debug("Can not setAttachedCircuitPort. SAP not found.");
                    }

                    break;
                default:
                    log.debug("skip " + type + " " + svcId);
            }
        }
    }

    public void getServiceStatus() throws IOException, AbortedException {
        Map<String, IntSnmpEntry> svcIdMap =
                SnmpUtil.getWalkResult(snmp, svcId, intEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> svcOperStatusMap =
                SnmpUtil.getWalkResult(snmp, svcOperStatus, intEntryBuilder, stringKeyCreator);

        for (String key : svcIdMap.keySet()) {
            int id = svcIdMap.get(key).intValue();
            PseudoWireOperStatus operStatus = getPseudoWireStatus(svcOperStatusMap, key);
            int sdpId = timetraSdpMib.getSdpId(id);
            if (sdpId == 0) {
                NodePipeImpl<Port> nodePipe = timetraSapMib.createNodePipe(id, "");
                nodePipe.setOperationalStatus(operStatus.name());
                continue;
            }
            int vcId = timetraSdpMib.getVcId(id);
            log.debug("svc " + id + " vc=" + vcId + " sdp=" + sdpId);
            builder.buildPseudoWire(vcId);
            builder.setPseudoWireOperStatus(vcId, operStatus);
        }
    }

    private PseudoWireOperStatus getPseudoWireStatus(Map<String, IntSnmpEntry> svcOperStatusMap, String key) throws IOException, AbortedException {
        Map<String, IntSnmpEntry> svcIdMap =
                SnmpUtil.getWalkResult(snmp, svcId, intEntryBuilder, stringKeyCreator);
        int svcId = svcIdMap.get(key).intValue();

        PseudoWireOperStatus operStatus;
        switch (svcOperStatusMap.get(key).intValue()) {
            case 1:
                operStatus = PseudoWireOperStatus.up;
                break;
            case 2:
                operStatus = PseudoWireOperStatus.down;
                break;
            default:
                throw new IllegalStateException();
        }
        List<Integer> statusList = timetraSdpMib.getSdpBindPwPeerStatusBits(svcId);
        if (statusList.size() > 0) {
            log.debug("setting down. status=" + statusList);
            operStatus = PseudoWireOperStatus.down;
        }
        return operStatus;
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