package voss.discovery.agent.atmbridge.exatrax;


import net.snmp.SnmpResponseException;
import voss.discovery.agent.atmbridge.EAConverterTelnetUtil;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.model.*;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class EXAtraxTelnetUtilTester {
    public static void main(String args[]) throws IOException, ConsoleException, AbortedException {
        ConsoleAccess telnet = null;
        EXAtraxTelnetUtil exa = new EXAtraxTelnetUtil();

        System.out.println("getModelName() : " + exa.getModelName(telnet));

        System.out.println("getOSTypeNmae() : " + exa.getOSTypeName(telnet));

        System.out.println("getVersion() : " + exa.getOSVersion(telnet));

        System.out.println("getBasePhysicalAddress() : "
                + exa.getBasePhysicalAddress(telnet));

        System.out.println("getGatewayAddress() : "
                + exa.getGatewayAddress(telnet));

        System.out.println("getTrapReceiverAddresses() : "
                + stringsToString(exa.getTrapReceiverAddresses(telnet)));

        System.out.println("getSyslogServerAddresses() : "
                + stringsToString(exa.getSyslogServerAddresses(telnet)));

        System.out
                .println("getSysIpAddress() : " + exa.getSysIpAddress(telnet));

        System.out.println("getSysIpSubnetMask() : "
                + exa.getSysIpSubnetMask(telnet, ""));

        System.out.println("getChassisEthernetPortIndexs() : "
                + intsToString(exa.getChassisEthernetPortIndexs(telnet)));

        System.out
                .println("getModuleEthernetPortIndexs() : "
                        + intsToString(exa.getModuleEthernetPortIndexs(telnet,
                        "hoge")));

        for (int i = 1; i <= 30; i++) {
            System.out.println("getEthernetPortDuplex("
                    + i
                    + ") : "
                    + exa.getEthernetPortDuplex(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("hasAggregationNameAndID("
                    + i
                    + ") : "
                    + exa.hasAggregationNameAndID(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out
                    .println("getAggregationID("
                            + i
                            + ") : "
                            + exa.getAggregationID(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("getPhysicalPortType("
                    + i
                    + ") : "
                    + exa.getPhysicalPortType(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("getPhysicalPortName("
                    + i
                    + ") : "
                    + exa.getPhysicalPortName(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("getPortAdminSpeed("
                    + i
                    + ") : "
                    + exa
                    .getPortAdminSpeed(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("getPortAdminStatus("
                    + i
                    + ") : "
                    + exa.getPortAdminStatus(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("getPortIfDescr(" + i + ") : "
                    + exa.getPortIfDescr(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("getPortOperationalStatus("
                    + i
                    + ") : "
                    + exa.getPortOperationalStatus(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("getPortSpeed(" + i + ") : "
                    + exa.getPortSpeed(telnet, Integer.toString(i)));
        }

        for (int i = 1; i <= 30; i++) {
            System.out.println("getPortStatus(" + i + ") : "
                    + exa.getPortStatus(telnet, Integer.toString(i)));
        }

        for (int slot = 1; slot <= 2; slot++) {
            System.out.println("getAtmPhysicalPortIndexs("
                    + slot
                    + ") : "
                    + intsToString(exa.getAtmPhysicalPortIndexs(telnet,
                    Integer.toString(slot))));

        }

        for (int slot = 1; slot <= 2; slot++) {
            for (int port = 1; port <= 2; port++) {
                String ifName = slot + "/" + port;
                System.out.println("getAtmPVCs(" + ifName + ") : "
                        + stringsToString(exa.getAtmPVCs(telnet, ifName)));
            }
        }
    }

    static private String stringsToString(String strs[]) {
        if (strs == null) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < strs.length; i++) {
            str.append(strs[i]);
            if (i < strs.length - 1) {
                str.append(" ");
            }
        }

        return str.toString();
    }

    static private String intsToString(int ints[]) {
        if (ints == null) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < ints.length; i++) {
            str.append(ints[i]);
            if (i < ints.length - 1) {
                str.append(" ");
            }
        }

        return str.toString();
    }
}

public final class EXAtraxTelnetUtil implements EAConverterTelnetUtil {
    private final List<Integer> ethernetPorts = new ArrayList<Integer>();

    public String getModelName(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        switch ((new ShowEther(telnet, 1)).isGbic()) {
            case 0:
                buildEhternetPortsList(false);
                return "NS-6111";
            case 1:
                buildEhternetPortsList(true);
                return "NS-6121/6142";
            default:
        }

        return null;
    }

    private void buildEhternetPortsList(boolean gbeType) {
        if (gbeType) {
            ethernetPorts.add(1);
            ethernetPorts.add(11);
        } else {
            for (int i = 1; i <= 20; i++) {
                ethernetPorts.add(i);
            }
        }
        for (int i = 21; i <= 30; i++) {
            ethernetPorts.add(i);
        }
    }

    public String getOSTypeName(ConsoleAccess telnet) throws IOException, AbortedException {
        return null;
    }

    public String getOSVersion(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        return (new ShowVersion(telnet)).getSwVersion();
    }

    public String getBasePhysicalAddress(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        return (new ShowVersion(telnet)).getLocalMacAddr();
    }

    public String getGatewayAddress(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        return (new ShowIpRoute(telnet)).getDefaultGateway();
    }

    public String[] getTrapReceiverAddresses(ConsoleAccess telnet)
            throws IOException, ConsoleException, AbortedException {
        return null;
    }

    public String[] getSyslogServerAddresses(ConsoleAccess telnet)
            throws IOException, ConsoleException, AbortedException {
        return null;
    }

    public String getSysIpAddress(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        return ((IpAddress) ((new ShowIpAll(telnet)).getDefaultAddress(0)))
                .getAddressStr();
    }

    public String getSysIpSubnetMask(ConsoleAccess telnet, String ipAddress)
            throws IOException, ConsoleException, AbortedException {
        IpAddress ipAddress1 = (IpAddress) ((new ShowIpAll(telnet))
                .getDefaultAddress(0));
        if (ipAddress1.getAddressStr().equals(ipAddress)) {
            return ipAddress1.getNetmaskStr();
        }
        IpAddress ipAddress2 = (IpAddress) ((new ShowIpAll(telnet))
                .getDefaultAddress(1));
        if (ipAddress2 != null && ipAddress2.getAddressStr().equals(ipAddress)) {
            return ipAddress2.getNetmaskStr();
        }
        return null;
    }

    public int[] getChassisEthernetPortIndexs(ConsoleAccess telnet)
            throws IOException, ConsoleException, AbortedException {
        LinkedList<Integer> list = new LinkedList<Integer>();

        for (Integer i : ethernetPorts) {
            if ((new ShowEther(telnet, i).isPresence()) == 1) {
                list.add(i);
            }
        }

        int[] idx = new int[list.size()];

        for (int i = 0; i < list.size(); i++) {
            idx[i] = list.get(i).intValue();
        }

        return idx;
    }

    public int[] getModuleEthernetPortIndexs(ConsoleAccess telnet,
                                             String slotName) throws IOException, ConsoleException, AbortedException {
        return new int[0];
    }

    public EthernetPort.Duplex getEthernetPortDuplex(ConsoleAccess telnet,
                                                     String ifName) throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        if (ethernetPorts.contains(port)) {
            ShowEther se = new ShowEther(telnet, port);
            switch (se.getOperDuplex()) {
                case 0:
                    return EthernetPort.Duplex.HALF;
                case 1:
                    return EthernetPort.Duplex.FULL;
                default:
            }
        }

        return null;
    }

    public boolean hasAggregationNameAndID(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        LinkedList<Lag> lagList = (new ShowLagAll(telnet)).getLagList();

        if (lagList == null)
            return false;

        for (int i = 0; i < lagList.size(); i++) {
            Lag lag = (Lag) lagList.get(i);
            for (int j = 0; j < lag.ethernetPortList.size(); j++) {
                if (((Integer) (lag.ethernetPortList.get(j))).intValue() == port) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getAggregationID(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        LinkedList<Lag> lagList = (new ShowLagAll(telnet)).getLagList();

        if (lagList == null)
            return -1;

        for (int i = 0; i < lagList.size(); i++) {
            Lag lag = (Lag) lagList.get(i);
            for (int j = 0; j < lag.ethernetPortList.size(); j++) {
                if (((Integer) (lag.ethernetPortList.get(j))).intValue() == port) {
                    return lag.aggregatorPort;
                }
            }
        }

        return -1;
    }

    public String getAggregationName(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        LinkedList<Lag> lagList = (new ShowLagAll(telnet)).getLagList();

        if (lagList == null)
            return null;

        for (int i = 0; i < lagList.size(); i++) {
            Lag lag = (Lag) lagList.get(i);
            for (int j = 0; j < lag.ethernetPortList.size(); j++) {
                if (((Integer) (lag.ethernetPortList.get(j))).intValue() == port) {
                    return lag.name;
                }
            }
        }

        return null;
    }

    public String getPhysicalPortType(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        if (ethernetPorts.contains(port)) {
            return (new ShowEther(telnet, port)).getPortType();
        }

        return null;
    }

    public String getPhysicalPortName(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        if (ethernetPorts.contains(port)) {
            return ((new ShowPortSummary(telnet, port)).getName());
        }
        return null;
    }

    public PortSpeedValue.Admin getPortAdminSpeed(ConsoleAccess telnet,
                                                  String ifName) throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        if (ethernetPorts.contains(port)) {
            int speed = (new ShowEther(telnet, port)).getAdminSpeed();
            if (speed > 0) {
                return new PortSpeedValue.Admin(speed * 1000, speed
                        / 1000 + "M");
            } else if ((new ShowEther(telnet, port)).getAutoNego() == 1) {
                return PortSpeedValue.Admin.AUTO;
            }
        }
        return null;
    }

    public String getPortAdminStatus(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        if (ethernetPorts.contains(port)) {
            switch ((new ShowPortSummary(telnet, port)).getAdminState()) {
                case 0:
                    return "Disable";
                case 1:
                    return "Enable";
                default:
            }
        }
        return null;
    }

    public String getPortIfDescr(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        return null;
    }

    public String getPortOperationalStatus(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        if (ethernetPorts.contains(port)) {
            switch ((new ShowEther(telnet, port).getOperState())) {
                case 0:
                    return "down";
                case 1:
                    return "up";
                default:
            }
        }
        return null;
    }

    public PortSpeedValue.Oper getPortSpeed(ConsoleAccess telnet,
                                            String ifName) throws IOException, ConsoleException, AbortedException {
        int port = ifNameToIfIndex(ifName);

        if (ethernetPorts.contains(port)) {
            int speed = (new ShowEther(telnet, port)).getOperSpeed();
            if (speed > 0) {
                return new PortSpeedValue.Oper(speed * 1000, speed
                        / 1000 + "M");
            }
        }

        return null;
    }

    public String getPortStatus(ConsoleAccess telnet, String ifName)
            throws IOException, ConsoleException, AbortedException {
        return null;
    }

    public int[] getAtmPhysicalPortIndexs(ConsoleAccess telnet, String slotName)
            throws IOException, ConsoleException, AbortedException {
        int slot = slotNameToSlot(slotName);

        switch ((new ShowVersion(telnet)).isExtBoardPresence(slot)) {
            case 1:
                int[] idx = new int[2];
                idx[0] = 1;
                idx[1] = 2;
                return idx;

            default:
        }

        return new int[0];
    }

    public String[] getAtmPVCs(ConsoleAccess telnet, String atmPhysicalPortIfName)
            throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        int port = atmIfNameToPort(atmPhysicalPortIfName);

        ShowAtmPvc sap = new ShowAtmPvc(telnet, slot, port);
        LinkedList<Pvc> list = sap.getPvcList();

        String str[] = new String[list.size()];

        for (int i = 0; i < list.size(); i++) {
            Pvc pvc = (Pvc) list.get(i);
            str[i] = pvc.vpi + "/" + pvc.vci;
        }

        return str;
    }

    public int getVpi(String pvc) throws IOException, ConsoleException, AbortedException {
        String num[];
        num = pvc.split("/");

        if (num[0] == null) {
            return -1;
        }
        return Integer.parseInt(num[0]);
    }

    public int getVci(String pvc) throws IOException, ConsoleException, AbortedException {
        String num[];
        num = pvc.split("/");

        if (num[1] == null) {
            return -1;
        }
        return Integer.parseInt(num[1]);
    }

    public AtmQosType getQos(ConsoleAccess telnet, String pvc,
                             String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        int port = atmIfNameToPort(atmPhysicalPortIfName);
        int vpi = getVpi(pvc);
        int vci = getVci(pvc);

        ShowPvc sp = new ShowPvc(telnet, slot, port, vpi, vci);
        if (sp.getMcr() > 0) {
            return AtmQosType.GFR;
        }

        if (sp.getPcr() > 0) {
            return AtmQosType.CBR;
        }

        return null;
    }

    public long getPcr(ConsoleAccess telnet, String pvc,
                       String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        int port = atmIfNameToPort(atmPhysicalPortIfName);
        int vpi = getVpi(pvc);
        int vci = getVci(pvc);

        int pcr = (new ShowPvc(telnet, slot, port, vpi, vci)).getPcr();

        if (pcr < 0)
            return -1L;

        return (long) pcr;
    }

    public long getMcr(ConsoleAccess telnet, String pvc,
                       String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        int port = atmIfNameToPort(atmPhysicalPortIfName);
        int vpi = getVpi(pvc);
        int vci = getVci(pvc);

        int mcr = (new ShowPvc(telnet, slot, port, vpi, vci)).getMcr();

        if (mcr < 0)
            return -1L;

        return mcr;
    }

    public int getBridgePortNumber(ConsoleAccess telnet, String pvc,
                                   String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        int port = atmIfNameToPort(atmPhysicalPortIfName);
        int vpi = getVpi(pvc);
        int vci = getVci(pvc);

        int result = (new ShowPvc(telnet, slot, port, vpi, vci))
                .getAssociatePortNumber();

        if (result <= 0) {
            return -1;
        }

        return result;
    }

    public String[] getVlanIfName(ConsoleAccess telnet, int bridgePortNumber)
            throws IOException, ConsoleException, AbortedException {
        LinkedList<String> vlanList = (new ShowPort(telnet, bridgePortNumber))
                .getVlanList();
        return vlanList.toArray(new String[0]);
    }

    public int getVlanEncapsIfTag(ConsoleAccess telnet, String vlanIfName)
            throws IOException, ConsoleException, AbortedException {
        int vlanId = (new ShowVlanSummary(telnet, vlanIfName)).getVlanId();
        return vlanId;
    }

    public boolean isTaggedVlan(ConsoleAccess telnet, String vlanIfName,
                                int bridgePortIndex) throws IOException, ConsoleException, AbortedException {
        LinkedList<Integer> unTaggedPortList = (new ShowVlanSummary(telnet, vlanIfName))
                .getUnTaggedPortList();
        for (int i = 0; i < unTaggedPortList.size(); i++) {
            Integer port = (Integer) unTaggedPortList.get(i);
            if (bridgePortIndex == port.intValue()) {
                return false;
            }
        }

        LinkedList<Integer> taggedPortList = (new ShowVlanSummary(telnet, vlanIfName))
                .getTaggedPortList();
        for (int i = 0; i < taggedPortList.size(); i++) {
            Integer port = (Integer) taggedPortList.get(i);
            if (bridgePortIndex == port.intValue()) {
                return true;
            }
        }

        throw new IOException("parse failed.");
    }

    public String[] getEthernetIfNames(ConsoleAccess telnet, String vlanIfName)
            throws IOException, ConsoleException, AbortedException {
        LinkedList<Integer> unTaggedPortList = (new ShowVlanSummary(telnet,
                vlanIfName)).getUnTaggedPortList();
        LinkedList<Integer> unTaggedEtherPortList = new LinkedList<Integer>();

        for (int i = 0; i < unTaggedPortList.size(); i++) {
            Integer port = (Integer) unTaggedPortList.get(i);

            if (1 <= port.intValue() && port.intValue() <= 30) {
                unTaggedEtherPortList.add(port);
            }
        }

        String[] str = new String[unTaggedEtherPortList.size()];
        for (int i = 0; i < str.length; i++) {
            str[i] = unTaggedEtherPortList.get(i).toString();
        }

        return str;
    }

    public int[] getSlotIndexs(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException {
        ShowVersion sv = new ShowVersion(telnet);
        int numSlot = 0;

        for (int i = 1; i <= 2; i++) {
            if (sv.isExtBoardPresence(i) == 1)
                numSlot++;
        }

        int[] idx = new int[numSlot];
        for (int i = 1, j = 0; i <= 2 && j < numSlot; i++) {
            if (sv.isExtBoardPresence(i) == 1)
                idx[j++] = i;
        }

        return idx;
    }

    public String getModuleName(ConsoleAccess telnet, int slotIndex)
            throws IOException, ConsoleException, AbortedException {
        switch ((new ShowVersion(telnet)).isExtBoardPresence(slotIndex)) {
            case 1:
                return "NS-605";
            default:
        }

        return null;
    }

    public String getAtmPhysicalPortType(ConsoleAccess telnet,
                                         String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);

        switch ((new ShowVersion(telnet)).isExtBoardPresence(slot)) {
            case 1:
                return "155M/SMF";
            default:
        }

        return null;
    }

    public String getAtmPhysicalPortName(ConsoleAccess telnet,
                                         String atmPhysicalPortIfName) {
        return null;
    }

    public String getAtmPhysicalPortAdminSpeed(ConsoleAccess telnet,
                                               String atmPhysicalPortIfName) {
        return null;
    }

    public String getAtmPhysicalPortAdminStatus(ConsoleAccess telnet,
                                                String atmPhysicalPortIfName) {
        return null;
    }

    public String getAtmPhysicalPortIfDescr(ConsoleAccess telnet,
                                            String atmPhysicalPortIfName) {
        return null;
    }

    public String getAtmPhysicalPortOperationalStatus(ConsoleAccess telnet,
                                                      String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        int port = atmIfNameToPort(atmPhysicalPortIfName);

        switch ((new ShowAtmPhy(telnet, slot, port)).getOperState()) {
            case 0:
                return "Down";
            case 1:
                return "Up";
            default:
        }

        return null;
    }

    public String getAtmPhysicalPortSpeed(ConsoleAccess telnet,
                                          String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        switch ((new ShowVersion(telnet)).isExtBoardPresence(slot)) {
            case 1:
                return "148M";
            default:
        }

        return null;
    }

    public String getAtmPhysicalPortStatus(ConsoleAccess telnet,
                                           String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        return null;
    }

    public String getAtmPVCOperStatus(ConsoleAccess telnet, String pvc,
                                      String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        int port = atmIfNameToPort(atmPhysicalPortIfName);
        int vpi = getVpi(pvc);
        int vci = getVci(pvc);

        switch ((new ShowPvc(telnet, slot, port, vpi, vci))
                .getBridgeOperState()) {
            case 0:
                return "DOWN";
            case 1:
                return "UP";
            default:
        }

        return null;
    }

    public String getAtmPVCAdminStatus(ConsoleAccess telnet, String pvc,
                                       String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException {
        int slot = atmIfNameToSlot(atmPhysicalPortIfName);
        int port = atmIfNameToPort(atmPhysicalPortIfName);
        int vpi = getVpi(pvc);
        int vci = getVci(pvc);

        switch ((new ShowPvc(telnet, slot, port, vpi, vci)).getAdminState()) {
            case 0:
                return "Disable";
            case 1:
                return "Enable";
            default:
        }

        return null;
    }

    public String getBridgeOperStatus(ConsoleAccess telnet, int bridgePortNumber)
            throws IOException, ConsoleException, AbortedException {
        switch ((new ShowPortSummary(telnet, bridgePortNumber)).getOperState()) {
            case 0:
                return "down";
            case 1:
                return "up";
            default:
        }
        return null;
    }

    public String getBridgeAdminStatus(ConsoleAccess telnet, int bridgePortNumber)
            throws IOException, ConsoleException, AbortedException {
        switch ((new ShowPortSummary(telnet, bridgePortNumber)).getAdminState()) {
            case 0:
                return "Disable";
            case 1:
                return "Enable";
            default:
        }
        return null;
    }

    public String getBridgePortName(ConsoleAccess telnet, int bridgePortNumber)
            throws IOException, ConsoleException, AbortedException {
        return (new ShowPortSummary(telnet, bridgePortNumber)).getName();
    }

    private int ifNameToIfIndex(String ifName) {
        return Integer.parseInt(ifName);
    }

    private int slotNameToSlot(String slotName) {
        return Integer.parseInt(slotName);
    }

    private int atmIfNameToSlot(String str) throws IOException, ConsoleException, AbortedException {
        return getVpi(str);
    }

    private int atmIfNameToPort(String str) throws IOException, ConsoleException, AbortedException {
        return getVci(str);
    }

    public int getBridgePortNumber(ConsoleAccess telnet, String ethernetIfName)
            throws IOException, ConsoleException, AbortedException {
        int i = Integer.parseInt(ethernetIfName);
        if ((new ShowEther(telnet, i).isPresence()) == 1) {
            return i;
        }
        return -1;
    }

    public String getVlanIfDescr(ConsoleAccess telnet, String vlanIfName)
            throws IOException, ConsoleException, AbortedException {
        if ((new ShowVlanSummary(telnet, vlanIfName)).getVlanId() > -1) {
            return vlanIfName;
        }
        return null;
    }


    public static final String nsVlanBridgePortIfIndex = ".1.3.6.1.4.1.263.2.1.15.1.1.2.";

    @Override
    public void supplementInterfaceAttributes(SnmpAccess snmp, EAConverter ea) throws IOException, AbortedException {
        InterfaceMibImpl ifmib = new InterfaceMibImpl(snmp);

        for (EthernetPort ether : ea.getEthernetPorts()) {
            int ifIndex = getIfIndexByBridgePortNumber(snmp, ether.getPortIndex());
            ether.initIfIndex(ifIndex);
        }

        for (LogicalEthernetPort logical : ea.getLogicalEthernetPorts()) {
            if (logical.isAggregated()) {
                int ifIndex = getIfIndexByBridgePortNumber(snmp, ((EthernetPortsAggregator) logical).getAggregationGroupId());
                logical.initIfIndex(ifIndex);
            }
        }

        for (AtmPhysicalPort atm : ea.getAtmPhysicalPorts()) {
            int ifIndex = 100 + (atm.getModule().getSlot().getSlotIndex() - 1) * 2 + atm.getPortIndex();
            atm.initIfIndex(ifIndex);
            ifmib.setIfDescription(atm);
        }

        for (AtmVlanBridge bridge : ea.getAtmVlanBridges()) {
            int ifIndex = getIfIndexByBridgePortNumber(snmp, bridge.getBridgePortNumber());
            bridge.initIfIndex(ifIndex);
        }
    }

    private int getIfIndexByBridgePortNumber(SnmpAccess snmp, int bridge) throws IOException, AbortedException {
        try {
            int ifIndex = SnmpUtil.getInteger(snmp, nsVlanBridgePortIfIndex + Integer.toString(bridge));
            return ifIndex;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }
}