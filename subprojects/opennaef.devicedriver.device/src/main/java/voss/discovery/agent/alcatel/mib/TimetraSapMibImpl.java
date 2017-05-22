package voss.discovery.agent.alcatel.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpHelper.OidKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.EthernetPort;
import voss.model.MplsVlanDevice;
import voss.model.NodePipeImpl;
import voss.model.Port;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.intEntryBuilder;

public class TimetraSapMibImpl implements TimetraSapMib {

    private static final Logger log = LoggerFactory.getLogger(TimetraSapMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public TimetraSapMibImpl(Alcatel7710SRDiscovery discovery) {
        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDevice();
    }

    public NodePipeImpl<Port> createNodePipe(int svcId, String descr) throws IOException, AbortedException {

        Map<OidKey, IntSnmpEntry> sapPortIdMap =
                SnmpUtil.getWalkResult(snmp, sapPortId, intEntryBuilder, SnmpHelper.oidKeyCreator);

        NodePipeImpl<Port> nodePipe = new NodePipeImpl<Port>();
        nodePipe.initDevice(device);
        nodePipe.initIfName("NodePipe" + svcId);
        nodePipe.setPipeName(String.valueOf(svcId));
        nodePipe.setIfDescr(descr);

        for (OidKey key : sapPortIdMap.keySet()) {
            if (key.getInt(0) == svcId) {
                int ifIndex = key.getInt(1);
                Port port = device.getPortByIfIndex(ifIndex);
                if (port instanceof EthernetPort) {
                    port = device.getLogicalEthernetPort((EthernetPort) port);
                }

                if (nodePipe.getAttachmentCircuit1() == null) {
                    nodePipe.setAttachmentCircuit1(port);
                } else if (nodePipe.getAttachmentCircuit2() == null) {
                    nodePipe.setAttachmentCircuit2(port);
                } else {
                    log.warn("NodePipe" + svcId + " already has two attachment circuit.");
                }
            }
        }

        return nodePipe;
    }

    public int getPortBySap(int id) throws IOException, AbortedException {

        Map<OidKey, IntSnmpEntry> sapPortIdMap =
                SnmpUtil.getWalkResult(snmp, sapPortId, intEntryBuilder, SnmpHelper.oidKeyCreator);

        for (OidKey key : sapPortIdMap.keySet()) {
            if (key.getInt(0) == id) {
                return sapPortIdMap.get(key).intValue();
            }
        }

        return 0;
    }

}