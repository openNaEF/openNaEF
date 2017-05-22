package voss.discovery.agent.common;

import voss.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceInfoUtil {

    public static Slot getSlot(EthernetSwitch node, int slotIndex) {
        for (int i = 0; i < node.getSlots().length; i++) {
            if (node.getSlots()[i].getSlotIndex() == slotIndex) {
                return node.getSlots()[i];
            }
        }
        return null;
    }

    public static String[] addNoDuplicate(String[] array, String target) {
        List<String> list = Arrays.asList(array);
        if (!list.contains(target)) {
            list.add(target);
        }
        String[] result = list.toArray(new String[0]);
        return result;
    }

    public static void addTrapReceiverAddress(VlanDevice node,
                                              String trapReceiverAddress) {
        node.setTrapReceiverAddresses(addNoDuplicate(node
                .getTrapReceiverAddresses(), trapReceiverAddress));
    }

    public static void addSyslogServerAddress(EthernetSwitch node,
                                              String syslogServerAddress) {
        node.setSyslogServerAddresses(addNoDuplicate(node
                .getSyslogServerAddresses(), syslogServerAddress));
    }

    public static void addPhysicalAddress(EthernetSwitch node,
                                          String physicalAddress) {
        node.setPhysicalAddresses(addNoDuplicate(node.getPhysicalAddresses(),
                physicalAddress));
    }

    public static void addIpAddress(EthernetSwitch node, String ipAddress) {
        node.setIpAddresses(addNoDuplicate(node.getIpAddresses(), ipAddress));
    }

    public static void addTaggedPort(VlanIf vlanIf, EthernetPort ethernetPort) {
        assert vlanIf.getDevice() == ethernetPort.getDevice();
        vlanIf.addTaggedPort(((VlanDevice) vlanIf.getDevice())
                .getLogicalEthernetPort(ethernetPort));
    }

    public static void addUntaggedPort(VlanIf vlanIf, EthernetPort ethernetPort) {
        assert vlanIf.getDevice() == ethernetPort.getDevice();
        vlanIf.addUntaggedPort(((VlanDevice) vlanIf.getDevice())
                .getLogicalEthernetPort(ethernetPort));
    }

    public static EthernetPort[] getChassisEthernetPorts(Device device) {
        List<EthernetPort> result = new ArrayList<EthernetPort>();
        PhysicalPort[] physicalPorts = device.getFixedChassisPhysicalPorts();
        for (int i = 0; i < physicalPorts.length; i++) {
            if (physicalPorts[i] instanceof EthernetPort) {
                result.add((EthernetPort) physicalPorts[i]);
            }
        }
        return result.toArray(new EthernetPort[0]);
    }

    public static EthernetPort[] getAllEthernetPorts(Device device) {
        List<EthernetPort> result = new ArrayList<EthernetPort>();
        PhysicalPort[] physicalPorts = device.getPhysicalPorts();
        for (int i = 0; i < physicalPorts.length; i++) {
            if (physicalPorts[i] instanceof EthernetPort) {
                result.add((EthernetPort) physicalPorts[i]);
            }
        }
        return result.toArray(new EthernetPort[0]);
    }

    public static EthernetPort getEthernetPortByIfIndex(Device device,
                                                        int ifIndex) {
        assert device != null;
        assert ifIndex >= 0;
        Port port = device.getPortByIfIndex(ifIndex);
        if (port instanceof EthernetPort) {
            return (EthernetPort) port;
        } else {
            return null;
        }
    }

    public static EthernetPort getEthernetPortByIfName(Device device,
                                                       String ifName) {
        assert device != null;
        assert ifName != null;
        Port port = device.getPortByIfName(ifName);
        if (port instanceof EthernetPort) {
            return (EthernetPort) port;
        } else {
            return null;
        }
    }

    public static VlanIf getVlanIfByIfName(VlanDevice device, String ifName) {
        if (ifName == null) {
            return null;
        }
        VlanIf[] source = device.getVlanIfs();
        for (int i = 0; i < source.length; i++) {
            if (ifName.equals(source[i].getIfName())) {
                return source[i];
            }
        }
        return null;
    }

    public static EthernetPort[] getEthernetPorts(VlanIf vlanIf) {
        EthernetPort[] ethernetPort = ((EthernetSwitch) vlanIf.getDevice())
                .getEthernetPorts();
        ArrayList<EthernetPort> result = new ArrayList<EthernetPort>();
        for (int i = 0; i < ethernetPort.length; i++) {
            if (isVlanEnable(ethernetPort[i], vlanIf)) {
                result.add(ethernetPort[i]);
            }
        }
        return result.toArray(new EthernetPort[0]);
    }

    public static boolean isVlanEnable(EthernetPort ethernetPort, VlanIf vlanIf) {
        List<VlanIf> allVlanIf = new ArrayList<VlanIf>();
        LogicalEthernetPort logicalEthernet = ((VlanDevice) ethernetPort
                .getDevice()).getLogicalEthernetPort(ethernetPort);
        allVlanIf.addAll(Arrays.asList(logicalEthernet.getUntaggedVlanIfs()));
        allVlanIf.addAll(Arrays.asList(logicalEthernet.getTaggedVlanIfs()));
        return allVlanIf.contains(vlanIf);
    }

    public static LogicalEthernetPort getLogicalEthernetPortByEthernetPortIfIndexOrEthernetPortsAggregatorIfIndex(
            VlanDevice node, int ifIndex) {
        EthernetPort ethernetPort = getEthernetPortByIfIndex(node, ifIndex);
        if (ethernetPort != null) {
            return node.getLogicalEthernetPort(ethernetPort);
        }
        return getEthernetPortsAggregatorInfoByIfIndex(node, ifIndex);
    }

    public static void supplementDefaultLogicalEthernetPortInfo(
            VlanDevice device) {
        EthernetPort[] ethernet = getAllEthernetPorts(device);
        for (int i = 0; i < ethernet.length; i++) {
            if (device.getLogicalEthernetPort(ethernet[i]) != null) {
                continue;
            }
            DefaultLogicalEthernetPort logicalEthernet = new DefaultLogicalEthernetPortImpl();
            device.addPort(logicalEthernet);
            logicalEthernet.initPhysicalPort(ethernet[i]);
            logicalEthernet.initIfName("default:" + ethernet[i].getIfName());
        }
    }

    public static EthernetPortsAggregator getOrCreateEthernetPortsAggregatorInfoByIfIndex(
            VlanDevice device, int ifIndex) {
        EthernetPortsAggregator gotten = getEthernetPortsAggregatorInfoByIfIndex(
                device, ifIndex);
        if (gotten != null) {
            return gotten;
        }
        EthernetPortsAggregator created = new EthernetPortsAggregatorImpl();
        device.addPort(created);
        created.initIfIndex(ifIndex);
        return created;
    }

    public static EthernetPortsAggregator getEthernetPortsAggregatorInfoByIfIndex(
            VlanDevice device, int ifIndex) {
        LogicalPort[] logical = device.getLogicalPorts();
        for (int i = 0; i < logical.length; i++) {
            if (!(logical[i] instanceof EthernetPortsAggregator)) {
                continue;
            }
            EthernetPortsAggregator eachLag = (EthernetPortsAggregator) logical[i];
            if (eachLag.getIfIndex() == ifIndex) {
                return eachLag;
            }
        }
        return null;
    }

    public static EthernetPortsAggregator getOrCreateLogicalEthernetPortByAggregationId(
            VlanDevice device, int aggregationId) {
        EthernetPortsAggregator got = device
                .getEthernetPortsAggregatorByAggregationGroupId(aggregationId);
        if (got != null) {
            return got;
        }
        EthernetPortsAggregator created = new EthernetPortsAggregatorImpl();
        device.addPort(created);
        created.initAggregationGroupId(aggregationId);
        created.setAggregationName(Integer.toString(aggregationId));
        return created;
    }

    public static EthernetPortsAggregator getOrCreateLogicalEthernetPortByAggregationId(
            VlanDevice device, int aggregationId, String ifName) {
        return getOrCreateLogicalEthernetPortByAggregationId(device, aggregationId, ifName, ifName);
    }


    public static EthernetPortsAggregator getOrCreateLogicalEthernetPortByAggregationId(
            VlanDevice device, int aggregationId, String ifName, String aggregationName) {
        EthernetPortsAggregator got = device
                .getEthernetPortsAggregatorByAggregationGroupId(aggregationId);
        if (got != null) {
            return got;
        }
        EthernetPortsAggregator created = new EthernetPortsAggregatorImpl();
        device.addPort(created);
        created.initIfName(ifName);
        created.initAggregationGroupId(aggregationId);
        created.setAggregationName(aggregationName);
        created.setConfigName(aggregationName);
        return created;
    }

    public static EthernetPort[] getBoundEthernetPorts(VlanIf vlanIf) {
        LogicalEthernetPort[] logical = vlanIf.getBindedPorts();
        ArrayList<EthernetPort> result = new ArrayList<EthernetPort>();
        for (int i = 0; i < logical.length; i++) {
            result.addAll(Arrays.asList(logical[i].getPhysicalPorts()));
        }
        return result.toArray(new EthernetPort[0]);
    }

    public static EthernetPort[] getEthernetPorts(Module module) {
        List<EthernetPort> result = new ArrayList<EthernetPort>();
        PhysicalPort[] ports = module.getPhysicalPorts();
        for (int i = 0; i < ports.length; i++) {
            if (ports[i] instanceof EthernetPort) {
                result.add((EthernetPort) ports[i]);
            }
        }
        return result.toArray(new EthernetPort[0]);
    }

    public static AtmPort[] getAtmPhysicalPorts(Module module) {
        List<AtmPhysicalPort> result = new ArrayList<AtmPhysicalPort>();
        PhysicalPort[] ports = module.getPhysicalPorts();
        for (int i = 0; i < ports.length; i++) {
            if (ports[i] instanceof AtmPhysicalPort) {
                result.add((AtmPhysicalPort) ports[i]);
            }
        }
        return result.toArray(new AtmPhysicalPort[0]);
    }

    public static List<AtmPort> getAtmPorts(Device device) {
        List<AtmPort> atmPorts = new ArrayList<AtmPort>();
        for (PhysicalPort phy : device.getPhysicalPorts()) {
            if (phy instanceof AtmPort) {
                atmPorts.add((AtmPort) phy);
            } else if (phy instanceof SerialPort) {
                LogicalPort feature = ((SerialPort) phy).getLogicalFeature();
                if (feature != null && feature instanceof AtmPort) {
                    atmPorts.add((AtmPort) feature);
                }
            }
        }
        return atmPorts;
    }

    public static PhysicalPort getPhysicalPortByPortIndex(Module module,
                                                          int portIndex) {
        PhysicalPort[] ports = module.getPhysicalPorts();
        for (int i = 0; i < ports.length; i++) {
            if (ports[i].getPortIndex() == portIndex) {
                return ports[i];
            }
        }
        return null;
    }

    public static void supplementEthernetPortsAggregatorName(VlanDevice device) {
        for (EthernetPortsAggregator lag : device.getEthernetPortsAggregators()) {
            if (lag.getAggregationName() == null) {
                lag.setAggregationName("[LAG]" + lag.getIfName());
            }
            if (lag.getAggregationGroupId() == null) {
                lag.initAggregationGroupId(lag.getIfIndex());
            }
        }
    }

}