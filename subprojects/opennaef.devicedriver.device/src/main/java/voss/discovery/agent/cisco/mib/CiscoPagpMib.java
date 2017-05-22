package voss.discovery.agent.cisco.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.IntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.EthernetPort;
import voss.model.EthernetPortsAggregator;
import voss.model.EthernetPortsAggregatorImpl;
import voss.model.VlanDevice;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.intEntryBuilder;
import static voss.discovery.iolib.snmp.SnmpHelper.integerKeyCreator;

public class CiscoPagpMib {
    private final static Logger log = LoggerFactory.getLogger(CiscoPagpMib.class);
    public static final String pagpGroupIfIndex = ".1.3.6.1.4.1.9.9.98.1.1.1.1.8";
    public static final String SYMBOL_pagpGroupIfIndex = "enterprises.cisco.ciscoMgmt.ciscoPagpMIB.ciscoPagpMIBObjects"
            + ".pagpGroupCapabilityConfiguration.pagpEtherChannelTable.pagpEtherChannelEntry"
            + ".pagpGroupIfIndex";

    private final SnmpAccess snmp;

    public CiscoPagpMib(SnmpAccess snmp) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
    }

    public void createAggregationGroup(VlanDevice device) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> pagpGroupIfIndices =
                SnmpUtil.getWalkResult(snmp, pagpGroupIfIndex, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : pagpGroupIfIndices.keySet()) {
            int basePortIndex = key.getInt();
            int aggregatorIfIndex = pagpGroupIfIndices.get(key).intValue();
            log.debug("basePortIndex=" + basePortIndex + ", aggregatorIfIndex=" + aggregatorIfIndex);

            if (aggregatorIfIndex == 0 || aggregatorIfIndex == basePortIndex) {
                continue;
            }

            EthernetPortsAggregator aggregator =
                    (EthernetPortsAggregator) device.getPortByIfIndex(aggregatorIfIndex);
            if (aggregator == null) {
                log.debug("@device '" + device.getDeviceName() + "'; create lag ifIndex '" + aggregatorIfIndex + "';");
                aggregator = new EthernetPortsAggregatorImpl();
                aggregator.initDevice(device);
                aggregator.initIfIndex(aggregatorIfIndex);
            }
            EthernetPort port = (EthernetPort) device.getPortByIfIndex(basePortIndex);
            log.debug("@device '" + device.getDeviceName()
                    + "'; add lag ifIndex '" + aggregatorIfIndex
                    + "' member '" + port.getIfIndex() + "';");
            aggregator.addPhysicalPort(port);
        }
    }
}