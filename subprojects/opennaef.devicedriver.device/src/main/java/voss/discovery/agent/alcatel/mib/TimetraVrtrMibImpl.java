package voss.discovery.agent.alcatel.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.agent.alcatel.mib.TimetraPortMib.TmnxPortEncapType;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class TimetraVrtrMibImpl implements TimetraVrtrMib {

    private static final Logger log = LoggerFactory.getLogger(TimetraVrtrMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    private final TimetraPortMibImpl timetraPortMib;

    public TimetraVrtrMibImpl(Alcatel7710SRDiscovery discovery) {
        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDeviceInner();

        this.timetraPortMib = discovery.getTimetraPortMib();
    }

    public void createLogicalPorts() throws IOException, AbortedException {

        Map<OidKey, StringSnmpEntry> vRtrIfNameMap =
                SnmpUtil.getWalkResult(snmp, vRtrIfName, stringEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> vRtrIfPortIDMap =
                SnmpUtil.getWalkResult(snmp, vRtrIfPortID, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> vRtrIfEncapValueMap =
                SnmpUtil.getWalkResult(snmp, vRtrIfEncapValue, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> vRtrIfOperStateMap =
                SnmpUtil.getWalkResult(snmp, vRtrIfOperState, intEntryBuilder, oidKeyCreator);

        for (OidKey key : vRtrIfNameMap.keySet()) {

            int vrtrId = key.getInt(0);
            int ifIndex = key.getInt(1);
            String ifName = vRtrIfNameMap.get(key).getValue();
            int portId = vRtrIfPortIDMap.get(key).intValue();

            log.debug("vrtrId=" + vrtrId + " ifIndex=" + ifIndex + " ifName=" + ifName + " portId=" + portId);

            if (portId == 0 ||
                    (timetraPortMib.isPhysicalPort(portId) || timetraPortMib.isChannel(portId))
                            && timetraPortMib.getPortState(portId) == 2) {
                continue;
            }

            if (vrtrId != 1) continue;

            Port port = device.getPortByIfIndex(ifIndex);
            device.removePort(port);

            Port physical = device.getPortByIfIndex(portId);

            if (physical == null) {
                LoopbackInterface logical = new LoopbackInterface();
                logical.initDevice(device);
                logical.initIfName(ifName);
                logical.initIfIndex(ifIndex);
            } else if (physical instanceof EthernetPort) {

                TmnxPortEncapType encapType = timetraPortMib.getPortEncapType(portId);
                if (encapType == TmnxPortEncapType.qEncap) {

                    int vlanId = vRtrIfEncapValueMap.get(key).intValue();
                    LogicalEthernetPort logical = device.getLogicalEthernetPort((EthernetPort) physical);
                    if (logical == null) {
                        logical = new DefaultLogicalEthernetPortImpl();
                        logical.initDevice(device);
                        ((DefaultLogicalEthernetPort) logical).initPhysicalPort((EthernetPort) physical);
                        logical.initIfName("[logical]" + timetraPortMib.getPortName(portId));
                    }
                    log.debug("vlanId=" + vlanId + " logical=" + logical);

                    RouterVlanIf vlanIf = new RouterVlanIfImpl();
                    vlanIf.initDevice(device);
                    vlanIf.initIfIndex(ifIndex);
                    vlanIf.initVlanId(vlanId);
                    vlanIf.initIfName(ifName);
                    vlanIf.setConfigName(timetraPortMib.getPortName(portId) + ":" + vlanId);
                    vlanIf.initVlanIfIndex(ifIndex);
                    vlanIf.initRouterPort(logical);
                    vlanIf.addTaggedPort(logical);

                    int operStatus = vRtrIfOperStateMap.get(key).intValue();
                    if (operStatus == 2) {
                        vlanIf.setOperationalStatus("up");
                    } else {
                        vlanIf.setOperationalStatus("down");
                    }
                } else {
                    DefaultLogicalEthernetPort logical = new DefaultLogicalEthernetPortImpl();
                    logical.initDevice(device);
                    logical.initPhysicalPort((EthernetPort) physical);
                    logical.initIfName("[logical]" + timetraPortMib.getPortName(portId));
                    logical.setConfigName(ifName);
                    logical.initIfIndex(ifIndex);
                }
            } else {
                log.debug("Unknown type: " + physical.getClass().getName());
            }
        }
    }
}