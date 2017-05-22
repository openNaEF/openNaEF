package voss.discovery.agent.vmware.collector;

import com.vmware.vim25.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.vmware.collector.traverser.GuestInfoTraverser;
import voss.discovery.agent.vmware.collector.traverser.HostConfigInfoTraverser;
import voss.discovery.agent.vmware.collector.traverser.VMListTraverser;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.discovery.iolib.RealDeviceAccessFactory;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.UnexpectedVarBindException;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.CidrAddress;
import voss.model.NodeInfo;
import voss.model.ProtocolPort;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectorService {

    public class ConcurrentMaskGetThread extends Thread {
        private ResponseQueue queue;
        private String ip;

        public ConcurrentMaskGetThread(ResponseQueue queue, String ip) {
            this.queue = queue;
            this.ip = ip;
        }

        @Override
        public void run() {
            try {
                NodeInfo nodeInfo = new NodeInfo();
                nodeInfo.addIpAddress(InetAddress.getByName(ip));
                nodeInfo.setCommunityStringRO("public");
                nodeInfo.addSupportedProtocol(ProtocolPort.SNMP_V2C_GETBULK);

                RealDeviceAccessFactory factory = new RealDeviceAccessFactory();
                DeviceAccess deviceAccess = factory.getDeviceAccess(nodeInfo);
                SnmpAccess snmpAccess = deviceAccess.getSnmpAccess();

                MibTable maskTable = new MibTable(snmpAccess, "", ipAddrEntry);
                maskTable.addColumn(ipAdEntNetMask_SUFFIX, "ipAdEntNetMask");
                maskTable.walk();

                Map<String, Integer> prefixLengths = new HashMap<String, Integer>();
                for (TableRow row : maskTable.getRows()) {
                    StringSnmpEntry entry = row.getColumnValue(ipAdEntNetMask_SUFFIX, SnmpHelper.stringEntryBuilder);
                    String ip = entry.getOIDSuffix(0) + "." +
                            entry.getOIDSuffix(1) + "." +
                            entry.getOIDSuffix(2) + "." +
                            entry.getOIDSuffix(3);
                    String mask = entry.getVarBind().getValueAsString();

                    prefixLengths.put(ip, SnmpUtil.getMaskLength(mask));
                }

                MIBResponse response = new MIBResponse();
                response.setPrefixLengths(prefixLengths);

                queue.putResponse(response);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AbortedException e) {
                e.printStackTrace();
            } catch (UnexpectedVarBindException e) {
                e.printStackTrace();
            } finally {
                queue.release();
            }
        }
    }

    private final static Logger log = LoggerFactory.getLogger(CollectorService.class);
    private static final String ipAddrEntry = ".1.3.6.1.2.1.4.20.1";
    private static final String ipAdEntNetMask_SUFFIX = "3";
    private String serverName;
    private String userName;
    private String password;

    private ArrayList<VirtualMachineConfigInfo> vmLists;
    private HostConfigInfo hostConfigInfo;
    private ArrayList<HostVirtualSwitch> vSwitches;
    private List<GuestInfo> guestInfos;
    private Map<String, String> macIpAddrMap;

    public CollectorService(String serverName, String userName, String password) {
        this.serverName = serverName;
        this.userName = userName;
        this.password = password;
    }

    public ArrayList<HostVirtualSwitch> getVSwitches() {
        if (vSwitches == null) {
            vSwitches = new ArrayList<HostVirtualSwitch>();
            try {
                if (getHostConfigInfo() != null) {
                    log.debug("get VSwitches = " + getHostConfigInfo().getNetwork().getVswitch());
                }
            } catch (IOException e) {
                log.debug("get VSwitches Error = " + e);
            } catch (RuntimeException e) {
                log.debug("get VSwitches Error = " + e);
            }
            try {
                if (getHostConfigInfo() != null) {
                    for (HostVirtualSwitch vsw : getHostConfigInfo().getNetwork().getVswitch()) {
                        log.debug("get VSwitches = " + vsw + " name = " + vsw.getName());
                        vSwitches.add(vsw);
                    }
                }
            } catch (IOException e) {
                log.debug("failed to get VSwitches " + e);
            } catch (RuntimeException e) {
                log.debug("failed to get VSwitches " + e);
            }
        }
        return vSwitches;
    }

    public ArrayList<HostVirtualSwitch> getVSwitch0() {
        ArrayList<HostVirtualSwitch> vswitch = new ArrayList<HostVirtualSwitch>();
        for (HostVirtualSwitch e : getVSwitches()) {
            if (e.getName().equals("vSwitch0")) {
                vswitch.add(e);
                break;
            }
        }
        return vswitch;
    }

    public ArrayList<HostVirtualSwitch> getVSwitch1() {
        ArrayList<HostVirtualSwitch> vswitch = new ArrayList<HostVirtualSwitch>();
        for (HostVirtualSwitch e : getVSwitches()) {
            if (e.getName().equals("vSwitch1")) {
                vswitch.add(e);
                break;
            }
        }
        return vswitch;
    }

    public ArrayList<HostVirtualSwitch> getAllVSwitch() {
        ArrayList<HostVirtualSwitch> vswitch = new ArrayList<HostVirtualSwitch>();
        for (HostVirtualSwitch e : getVSwitches()) {
            vswitch.add(e);
        }
        return vswitch;
    }


    public HostVirtualSwitch getVSwitch(String name) {
        for (HostVirtualSwitch e : vSwitches) {
            if (e.getName() == name) {
                return e;
            }
        }
        return null;
    }

    public HostPortGroup getPortGroup(String key) {
        try {
            for (HostPortGroup portGroup : getHostConfigInfo().getNetwork().getPortgroup()) {
                if (portGroup.getKey().equals(key)) {
                    return portGroup;
                }
            }
        } catch (IOException e) {
            log.debug("failed to get PortGroup" + e);
        }

        return null;
    }

    public List<HostPortGroup> getPortGroups(String vSwitchKey) {
        List<HostPortGroup> portGroups = new ArrayList<HostPortGroup>();
        for (String portGroupKey : getVSwitch(vSwitchKey).getPortgroup()) {
            HostPortGroup portGroup = getPortGroup(portGroupKey);
            if (portGroup != null) {
                portGroups.add(portGroup);
            }
        }

        return portGroups;
    }

    public PhysicalNic[] getPnic() {
        return this.hostConfigInfo.getNetwork().getPnic();
    }

    public HostConfigInfo getHostConfigInfo() throws IOException {
        if (hostConfigInfo == null) {
            Collector vSwitchListCollector = new Collector(serverName, userName, password, new HostConfigInfoTraverser());
            ObjectContent[] objectContents = vSwitchListCollector.getProperties();
            if (objectContents != null) {
                for (ObjectContent oc : objectContents) {
                    for (DynamicProperty dp : oc.getPropSet()) {
                        Object val = dp.getVal();
                        if (val instanceof HostConfigInfo) {
                            hostConfigInfo = (HostConfigInfo) val;
                        }
                    }
                }
            } else {
                log.warn("ESX " + serverName + " has no vHost");
            }
        }

        return hostConfigInfo;
    }

    public ArrayList<VirtualMachineConfigInfo> getVirtualMachines() {
        if (vmLists == null) {
            Collector vmListCollector = new Collector(serverName, userName, password, new VMListTraverser());
            vmLists = new ArrayList<VirtualMachineConfigInfo>();
            ObjectContent[] objectContents;
            try {
                objectContents = vmListCollector.getProperties();
                if (objectContents != null) {
                    for (ObjectContent oc : objectContents) {
                        if (oc.getPropSet() != null) {
                            for (DynamicProperty dp : oc.getPropSet()) {
                                Object val = dp.getVal();
                                if (val instanceof VirtualMachineConfigInfo) {
                                    vmLists.add((VirtualMachineConfigInfo) val);
                                }
                            }
                        }
                    }
                } else {
                    log.warn("ESX " + serverName + " has no vHost");
                }
            } catch (IOException e) {
                log.debug("failed to get VirtualMachines" + e);
            }
        }
        return vmLists;
    }

    public ArrayList<VirtualEthernetCard> getVMPorts(String vmName) {
        ArrayList<VirtualEthernetCard> ports = new ArrayList<VirtualEthernetCard>();

        VirtualMachineConfigInfo vm = getVirtualMachine(vmName);
        if (vm != null) {
            for (VirtualDevice vd : vm.getHardware().getDevice()) {
                if (vd instanceof VirtualEthernetCard) {
                    ports.add((VirtualEthernetCard) vd);
                }
            }
        }

        return ports;
    }

    private VirtualMachineConfigInfo getVirtualMachine(String vmName) {
        for (VirtualMachineConfigInfo e : vmLists) {
            if (e.getName() == vmName) {
                return e;
            }
        }

        return null;
    }

    public List<VirtualMachineConfigInfo> getVirtualMachineFromVSwitchName(String vSwitch) {
        List<String> portGroups = new ArrayList<String>();
        for (HostPortGroup group : getPortGroups(vSwitch)) {
            portGroups.add(group.getSpec().getName());
        }

        List<VirtualMachineConfigInfo> vms = new ArrayList<VirtualMachineConfigInfo>();
        for (VirtualMachineConfigInfo vm : getVirtualMachines()) {
            List<VirtualEthernetCard> vmPorts = getVMPorts(vm.getName());
            if (vmPorts.size() == 0) {
                continue;
            }
            if (portGroups.contains(vmPorts.get(0).getDeviceInfo().getSummary())) {
                vms.add(vm);
            }
        }

        return vms;
    }

    public List<VirtualMachineConfigInfo> getVirtualMachineFromNetworkName(String networkName) {
        List<VirtualMachineConfigInfo> vms = new ArrayList<VirtualMachineConfigInfo>();
        for (VirtualMachineConfigInfo vm : getVirtualMachines()) {
            if (getVMPorts(vm.getName()) != null) {
                if (!getVMPorts(vm.getName()).isEmpty()) {
                    if (getVMPorts(vm.getName()).get(0).getDeviceInfo().getSummary().equals(networkName)) {
                        vms.add(vm);
                    }
                }
            }
        }
        return vms;
    }

    public List<GuestInfo> getGuestInfos() {
        if (guestInfos == null) {
            guestInfos = new ArrayList<GuestInfo>();
            Collector guestInfoCollector = new Collector(serverName, userName, password, new GuestInfoTraverser());
            ObjectContent[] objectContents;
            try {
                objectContents = guestInfoCollector.getProperties();
                for (ObjectContent oc : objectContents) {
                    for (DynamicProperty dp : oc.getPropSet()) {
                        Object val = dp.getVal();
                        if (val instanceof GuestInfo) {
                            guestInfos.add((GuestInfo) val);
                        }
                    }
                }
            } catch (IOException e) {
                log.debug("failed to get GuestInfo" + e);
            }
        }
        return guestInfos;
    }

    public CidrAddress getIpAddress(String macAddress) {
        Map<String, String> macIpAddrMap;
        try {
            macIpAddrMap = getMacIpAddrMap();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return null;
        }
        if (!macIpAddrMap.containsKey(macAddress)) {
            return null;
        }

        String cidrStr = macIpAddrMap.get(macAddress);
        String[] tmp = cidrStr.split("/");
        String ipAddr = tmp[0];
        int masklen = Integer.parseInt(tmp[1]);
        try {
            return new CidrAddress(InetAddress.getByName(ipAddr), masklen);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private Map<String, String> getMacIpAddrMap() throws InterruptedException {
        if (macIpAddrMap != null) return macIpAddrMap;

        macIpAddrMap = new HashMap<String, String>();
        List<GuestInfo> guestInfos = getGuestInfos();

        for (GuestInfo guestInfo : guestInfos) {
            if (guestInfo.getNet() == null) continue;
            for (GuestNicInfo nic : guestInfo.getNet()) {
                if (nic.getIpConfig() != null) {
                    handleLaterESX41(nic);
                } else {
                    handleEarlierESX4(guestInfo, nic);
                }
            }
        }

        return macIpAddrMap;
    }

    private void handleLaterESX41(GuestNicInfo nic) {
        NetIpConfigInfoIpAddress[] ipAddresses = nic.getIpConfig().getIpAddress();
        if (ipAddresses != null) {
            for (NetIpConfigInfoIpAddress ipAddr : ipAddresses) {
                if (isIPv4Address(ipAddr.getIpAddress())) {
                    macIpAddrMap.put(nic.getMacAddress(), ipAddr.getIpAddress() + "/" + ipAddr.getPrefixLength());
                }
            }
        }
    }

    private void handleEarlierESX4(GuestInfo guestInfo, GuestNicInfo nic) {
        Map<String, Integer> prefixLengths = getPrefixLengths(guestInfo);
        if (prefixLengths == null) return;
        for (String ipAddress : nic.getIpAddress()) {
            if (isIPv4Address(ipAddress)) {
                macIpAddrMap.put(nic.getMacAddress(), ipAddress + "/" + prefixLengths.get(ipAddress));
            }
        }
    }

    private Map<String, Integer> getPrefixLengths(GuestInfo guestInfo) {
        List<String> candidates = getGuestIpList(guestInfo);
        if (candidates.size() == 0) {
            return null;
        }

        ResponseQueue queue = new ResponseQueue();
        for (String ip : candidates) {
            new ConcurrentMaskGetThread(queue, ip).start();
        }

        MIBResponse response;
        try {
            response = queue.getResponse();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        return response.getPrefixLengths();
    }

    private List<String> getGuestIpList(GuestInfo guestInfo) {
        List<String> ips = new ArrayList<String>();
        if (guestInfo.getNet() != null) {
            for (GuestNicInfo nic : guestInfo.getNet()) {
                for (String ipAddress : nic.getIpAddress()) {
                    if (isIPv4Address(ipAddress)) {
                        ips.add(ipAddress);
                    }
                }
            }
        }
        return ips;
    }

    private boolean isIPv4Address(String ipAddress) {
        return ipAddress.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }
}