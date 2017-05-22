package voss.discovery.agent.fortigate;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.PhysicalEntry;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fortigate1000CEntityFactory extends AbstractFortigateEntityFactory {
    private static final Logger log = LoggerFactory.getLogger(Fortigate1000CEntityFactory.class);

    @Override
    public Port createPort(PhysicalEntry pe, Device device) {
        String ifName = pe.physicalName;
        int ifIndex = pe.ifindex;
        EthernetPort eth = new EthernetPortImpl();
        eth.initDevice(device);
        eth.initIfName(ifName);
        eth.initIfIndex(ifIndex);
        eth.initPortIndex(pe.position);
        return eth;
    }

    @Override
    public void createVdom(DeviceAccess access, MplsVlanDevice device) throws IOException, AbortedException {
        Map<Integer, MplsVlanDevice> vdoms = new HashMap<Integer, MplsVlanDevice>();
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(access.getSnmpAccess(), FortigateMib.fgVdEntName);
            for (StringSnmpEntry entry : entries) {
                int id = entry.getLastOIDIndex().intValue();
                String vdomName = entry.getValue();
                if ("root".equals(vdomName)) {
                    continue;
                }
                MplsVlanDevice vdom = new MplsVlanDevice();
                vdom.setDeviceName(vdomName);
                vdom.setPhysicalDevice(device);
                device.addVirtualDevice(vdom);
                VdomIdRenderer renderer = new VdomIdRenderer(vdom);
                renderer.set(Integer.valueOf(id));
                vdoms.put(Integer.valueOf(id), vdom);
            }
            List<IntSnmpEntry> aliasPortEntries = SnmpUtil.getIntSnmpEntries(access.getSnmpAccess(), FortigateMib.fgIntfEntVdom);
            for (IntSnmpEntry entry : aliasPortEntries) {
                int ifIndex = entry.getLastOIDIndex().intValue();
                int vdomIndex = entry.intValue();
                log.debug("found alias: vdom=" + vdomIndex + ", ifIndex=" + ifIndex);
                if (vdomIndex == 1) {
                    log.debug("- ignore (root vdom)");
                    continue;
                }
                MplsVlanDevice vdom = vdoms.get(Integer.valueOf(vdomIndex));
                if (vdom == null) {
                    log.warn("- no vdom found: ifIndex=" + ifIndex + ", vdomIndex=" + vdomIndex);
                    continue;
                }
                Port source = device.getPortByIfIndex(ifIndex);
                if (source == null) {
                    log.warn("- no alias source port found: ifIndex=" + ifIndex + ", vdomIndex=" + vdomIndex);
                    continue;
                }
                log.debug("- found alias-source: " + source.getFullyQualifiedName());
                if (VlanIf.class.isInstance(source)) {
                    VlanIf vif = new RouterVlanIfImpl();
                    vif.initDevice(vdom);
                    vif.initIfIndex(ifIndex);
                    vif.initIfName(source.getIfName());
                    vif.setAliasSource(source);
                    log.debug("- alias created: [VlanIf]" + vif.getIfName());
                } else if (EthernetPort.class.isInstance(source)) {
                    EthernetPortImpl eth = new EthernetPortImpl();
                    eth.initDevice(vdom);
                    eth.initIfIndex(ifIndex);
                    eth.initIfName(source.getIfName());
                    eth.setAliasSource(source);
                    log.debug("- alias created: [EthernetPort]" + eth.getIfName());
                } else if (EthernetPortsAggregator.class.isInstance(source)) {
                    EthernetPortsAggregator lag = new EthernetPortsAggregatorImpl();
                    lag.initDevice(vdom);
                    lag.initIfIndex(ifIndex);
                    lag.initIfName(source.getIfName());
                    lag.initAggregationGroupId(((EthernetPortsAggregator) source).getAggregationGroupId());
                    lag.setAggregationName(((EthernetPortsAggregator) source).getAggregationName());
                    lag.setAliasSource(source);
                    log.debug("- alias created: [EthernetPortsAggregator]" + lag.getIfName());
                } else {
                    log.warn("* unknown source type: " + source.getFullyQualifiedName()
                            + "(" + source.getClass().getName() + ")");
                }
            }
        } catch (RepeatedOidException e) {
            throw new IOException("Unexpected problem.", e);
        } catch (SnmpResponseException e) {
            throw new IOException("Unexpected problem.", e);
        }
    }
}