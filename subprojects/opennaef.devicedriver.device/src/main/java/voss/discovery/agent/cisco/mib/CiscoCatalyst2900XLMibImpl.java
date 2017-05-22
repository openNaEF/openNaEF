package voss.discovery.agent.cisco.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.cisco.mib.CiscoMibUtil.CiscoModulePortKey;
import voss.discovery.agent.common.Constants;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;
import voss.model.EthernetPort.Duplex;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.intEntryBuilder;

public class CiscoCatalyst2900XLMibImpl implements CiscoCatalyst2900XLMib {
    private final static Logger log = LoggerFactory.getLogger(CiscoCatalyst2900XLMibImpl.class);
    private final SnmpAccess snmp;

    public CiscoCatalyst2900XLMibImpl(SnmpAccess snmp) {
        this.snmp = snmp;
    }

    public void createSlotsAndModules(VlanDevice device) throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, c2900ModuleType);
            for (IntSnmpEntry entry : entries) {
                int slotIndex = entry.getLastOIDIndex().intValue();
                int moduleId = entry.intValue();
                String moduleName = getModuleName(moduleId);
                Slot slot = new SlotImpl();
                slot.initContainer(device);
                slot.initSlotIndex(slotIndex);

                Module module = new ModuleImpl();
                module.initSlot(slot);
                module.setModelTypeName(moduleName);

                slot.setModule(module);
            }
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private String getModuleName(int value) {
        return value == 1
                ? "other"
                : value == 2
                ? null
                : value == 3
                ? "wsx2914xl"
                : value == 4
                ? "wsx2922xl"
                : value == 5
                ? "atm155SMLRFiber"
                : value == 6
                ? "atm155SMMRFiber"
                : value == 7
                ? "atm155MMFiber"
                : value == 8
                ? "atm155UTP"
                : value == 9
                ? "wsx2914xlv"
                : value == 10
                ? "wsx2922xlv"
                : value == 11
                ? "wsx2924xlv"
                : value == 12
                ? "wsx2931xl"
                : value == 13
                ? "wsx2932xl"
                : "---";
    }

    public void createC2900Interfaces(VlanDevice device) throws IOException, AbortedException {
        Map<CiscoModulePortKey, IntSnmpEntry> c2900PortGroupIndices =
                SnmpUtil.getWalkResult(snmp, c2900PortGroupIndex, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoModulePortKey, IntSnmpEntry> c2900PortDuplexStatuses =
                SnmpUtil.getWalkResult(snmp, c2900PortDuplexStatus, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoModulePortKey, IntSnmpEntry> c2900PortIfIndices =
                SnmpUtil.getWalkResult(snmp, c2900PortIfIndex, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoModulePortKey, IntSnmpEntry> c2900PortAdminSpeeds =
                SnmpUtil.getWalkResult(snmp, c2900PortAdminSpeed, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);
        Map<CiscoModulePortKey, IntSnmpEntry> c2900PortUsageApplications =
                SnmpUtil.getWalkResult(snmp, c2900PortUsageApplication, intEntryBuilder, CiscoMibUtil.ciscoModulePortKeyCreator);

        for (CiscoModulePortKey key : c2900PortGroupIndices.keySet()) {
            int slotIndex = key.slotIndex;
            int portIndex = key.portIndex;
            int ifindex = c2900PortIfIndices.get(key).intValue();
            log.debug("process port: " + slotIndex + "/" + portIndex + "(ifindex=" + ifindex + ")");

            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initIfIndex(ifindex);
            port.initPortIndex(portIndex);

            String ifname = Mib2Impl.getIfName(snmp, ifindex);
            port.initIfName(ifname);

            if (device.getSlots() == null || device.getSlots().length == 0 || slotIndex == 0) {
            } else {
                device.getSlotBySlotIndex(slotIndex).getModule().addPort(port);
            }

            int duplexValue = c2900PortDuplexStatuses.get(key).intValue();
            Duplex duplex = getDuplexStatus(duplexValue);
            port.setDuplex(duplex);

            PortSpeedValue.Admin adminSpeed = getAsSpeed(c2900PortAdminSpeeds.get(key).intValue());
            port.setPortAdministrativeSpeed(adminSpeed);

            Catalyst2900MibPortUsage usage =
                    Catalyst2900MibPortUsage.valueOf(c2900PortUsageApplications.get(key).intValue());

            if (usage.equals(Catalyst2900MibPortUsage.portGrouping)) {
                int aggregationGroupId = c2900PortGroupIndices.get(key).intValue();
                EthernetPortsAggregator aggregator =
                        device.getEthernetPortsAggregatorByAggregationGroupId(aggregationGroupId);
                if (aggregator == null) {
                    aggregator = new EthernetPortsAggregatorImpl();
                    initializeEthernetPortsAggregator(device, aggregator, aggregationGroupId);
                }
                aggregator.addPhysicalPort(port);
            } else {
                DefaultLogicalEthernetPort defaultLogical = new DefaultLogicalEthernetPortImpl();
                defaultLogical.initDevice(device);
                defaultLogical.initPhysicalPort(port);
                defaultLogical.initIfName("[default]" + port.getIfName());
            }
        }

    }

    private void initializeEthernetPortsAggregator(VlanDevice device,
                                                   EthernetPortsAggregator aggregator, int aggregationGroupId)
            throws IOException, AbortedException {
        aggregator.initDevice(device);

        aggregator.initAggregationGroupId(aggregationGroupId);

        int ifindex = getPortGroupIfIndex(aggregationGroupId);
        aggregator.initIfIndex(ifindex);

        aggregator.initIfName("Port-Channel" + aggregationGroupId);
        aggregator.setAggregationName("Port-Channel" + aggregationGroupId);
    }

    private int getPortGroupIfIndex(int aggregationId) throws IOException, AbortedException {
        try {
            List<StringSnmpEntry> entries = SnmpUtil.getStringSnmpEntries(snmp, InterfaceMib.ifDesc);
            for (StringSnmpEntry entry : entries) {
                String ifDesc = entry.getValue();
                if (ifDesc.toLowerCase().equals("port-channel" + aggregationId)) {
                    return entry.getLastOIDIndex().intValue();
                }
            }
            throw new IllegalStateException("unknown aggregation-id: " + aggregationId);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
    }

    private Duplex getDuplexStatus(int value) {
        switch (value) {
            case 1:
                return Duplex.FULL;
            case 2:
                return Duplex.HALF;
            default:
                throw new IllegalStateException("unknown c2900DuplexStatus value=" + value);
        }
    }

    private PortSpeedValue.Admin getAsSpeed(int value) {
        switch (value) {
            case 1:
                return PortSpeedValue.Admin.AUTO;
            case 10000000:
                return new PortSpeedValue.Admin(10L * Constants.MEGA, "10M");
            case 100000000:
                return new PortSpeedValue.Admin(100L * Constants.MEGA, "100M");
            case 155520000:
                return new PortSpeedValue.Admin(155520L * Constants.KILO, "ATM 155.52M");
            default:
                return null;
        }
    }
}