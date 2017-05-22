package voss.discovery.agent.atmbridge.superhub;


import voss.discovery.agent.atmbridge.EAConverterTelnetUtil;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleCommand;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.console.InvalidCommandException;
import voss.discovery.iolib.simpletelnet.GlobalMode;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.AtmQosType;
import voss.model.EAConverter;
import voss.model.EthernetPort;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuperHubTelnetUtil implements EAConverterTelnetUtil {

    private Pattern slotIndexPattern = Pattern
            .compile("\\s*([1-4][LR])\\s*([\\w-]*)\\s*(.*)\\*\\s*([1-4])\\s*\"\\s*([\\w-]*)\\s*\"\\s*\"\\s*([\\w-]*)\\s*\"\\s*(\\w*)\\s*");
    private Pattern hostnamePattern = Pattern.compile(".*=\\s*\"(.*)\"\\s*");
    private Pattern modelNamePattern = Pattern.compile("([\\w/]+)");
    private Pattern versionPattern = Pattern.compile("Release\\s*:\\s*(.*)");
    private Pattern editionPattern = Pattern
            .compile("Edition\\s*:\\s*([0-9]+)");
    private Pattern duplexPattern = Pattern
            .compile("Duplex Mode\\s*:([\\w]+)\\sDuplex\\s*\\(AutoNegotiation\\s(\\w+)\\)");
    private Pattern duplexPattern2 = Pattern
            .compile("Duplex Mode\\s*:Auto Negotiation\\s*");
    private Pattern portTypePattern = Pattern
            .compile("Port Type\\s*:([\\w-]+)\\s*\\(AutoNegotiation\\s(\\w+)\\)");
    private Pattern portTypePattern2 = Pattern
            .compile("Port Type\\s*:Auto Negotiation\\s*");
    private Pattern oprStatusPattern = Pattern
            .compile("Link Status\\s*:([\\w]+)\\s*");
    private Pattern adminStatusPattern = Pattern
            .compile("Tx/Rx\\s*:([\\w]+)\\s*");
    private Pattern pvcListPattern = Pattern
            .compile("\\s*([1-4][LR]/[1-4])\\s*([0-9]+)\\s*([0-9]+)\\s*PVC\\s*([1-4][LR]/[1-4])\\s*([0-9]+)\\s*([0-9]+)\\s*(\\w+)\\s*");
    private Pattern trapReceiverPattern = Pattern
            .compile("Snmp Manager\\s*:\\s*([0-9\\.]+)\\s*");
    @SuppressWarnings("unused")
    private Pattern syslogServerPattern = Pattern
            .compile("\\s*logging\\s*([0-9\\.]+)\\s*");
    private Pattern defaultGWPattern = Pattern
            .compile("\\s*0\\.0\\.0\\.0\\s*\\(mask\\s0\\.0\\.0\\.0\\s*\\)\\s\\[\\s*\\w+\\]\\svia\\s([0-9\\.]+).*");
    private Pattern ipInterfacePattern = Pattern
            .compile("\\s*(\\w+)\\s*IP address:([0-9\\.]+)\\s*netmask:([0-9\\.]+)");
    private Pattern svcCatPattern = Pattern
            .compile("Tx service-category:\\s*([\\w-]+)\\s*.*");
    private Pattern shapingPortPattern = Pattern
            .compile("\\s*([1-4][LR]/[1-4])\\s*(\\w+)\\s*([0-9]+)\\s*([0-9-]+)\\s*([0-9/]+)\\s*([0-9]+)\\s*([0-9]+)\\s*");
    private Pattern pcrPattern = Pattern.compile("Tx pcr-clp01:\\s*(\\w*)\\s*");
    private Pattern mcrPattern = Pattern.compile("Tx mcr-clp01:\\s*(\\w*)\\s*");
    private Pattern bridgePortPattern = Pattern
            .compile("VLAN No\\.\\s*:\\s*([0-9]*)\\s*([0-9]*)\\s*([0-9]*)\\s*([0-9]*)");
    private Pattern vlanNoPattern = Pattern
            .compile("VLAN No\\.([0-9]+)\\s*MAC Address:\\s*([0-9a-f\\.]+)\\s*VID:\\s*(\\w+)\\s*");
    private Pattern vlanStatusPattern = Pattern
            .compile(".*VLAN is (\\w+)\\..*");
    private Pattern vlanIfPattern = Pattern
            .compile("\\s*[\\w-]+\\s*([\\w/]+)\\s*[\\w\\s]{10}\\s*([\\w-\\(\\)]+).*");
    @SuppressWarnings("unused")
    private Pattern atmBoardTypePattern = Pattern
            .compile("\\s*Interface:.*Board-type:\\s*([\\w-]+)\\s*");
    private Pattern atmIfStatusPattern = Pattern
            .compile("\\s*IF Status:\\s*(\\w+).*");
    private Pattern atmLinkStatusPattern = Pattern
            .compile("\\s*LINK Status:\\s*(\\w+).*");

    public String[] getSlotIndexs(ConsoleAccess telnet) throws IOException,
            AbortedException {
        StringTokenizer st = this.getResults(telnet, "show slot");
        Matcher matcher;
        LinkedList<String> slotList = new LinkedList<String>();

        while (st.hasMoreTokens()) {
            matcher = slotIndexPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                slotList.add(matcher.group(1));
            }
        }
        if (slotList.size() == 0)
            return (new String[0]);
        return slotList.toArray(new String[0]);
    }

    public String getModuleName(ConsoleAccess telnet, String slotIndex)
            throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show slot " + slotIndex);
        Matcher matcher;
        while (st.hasMoreTokens()) {
            matcher = slotIndexPattern.matcher(st.nextToken());
            if (matcher.matches() && slotIndex.equals(matcher.group(1))) {
                return matcher.group(5);
            }
        }
        throw new IOException("parse failed");
    }

    public String getSysName(ConsoleAccess telnet) throws IOException,
            AbortedException {
        StringTokenizer st = this.getResults(telnet, "show hostname");

        if (st.hasMoreTokens()) {
            Matcher matcher = hostnamePattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        throw new IOException("parse failed");
    }

    public String getModelName(ConsoleAccess telnet) throws IOException,
            AbortedException {
        StringTokenizer st = this.getResults(telnet, "show version");

        if (st.hasMoreTokens()) {
            Matcher matcher = modelNamePattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        throw new IOException("parse failed");
    }

    public String getOSTypeName(ConsoleAccess telnet) throws IOException,
            AbortedException {
        return null;
    }

    public String getOSVersion(ConsoleAccess telnet) throws IOException,
            AbortedException {
        StringTokenizer st = this.getResults(telnet, "show version");
        Matcher matcher;
        String version = null;
        String edition = null;

        while (st.hasMoreTokens()) {
            matcher = versionPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                version = matcher.group(1);
                break;
            }
        }
        if (version == null)
            throw new IOException("parse failed");

        while (st.hasMoreTokens()) {
            matcher = editionPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                edition = matcher.group(1);
                return version + " Ed" + edition;
            }
        }
        throw new IOException("parse failed");
    }

    public String getBasePhysicalAddress(ConsoleAccess telnet)
            throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show bridge vlan");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = vlanNoPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                String temp = matcher.group(2);
                temp = temp.replaceAll("\\.", "");

                String phyAddr = "";
                for (int i = 0; i < 6; i++) {
                    phyAddr += temp.substring(2 * i, 2 * i + 2);
                    if (i < 5)
                        phyAddr += ":";
                }
                return phyAddr;
            }
        }
        throw new IOException("parse failed");
    }

    public String getGatewayAddress(ConsoleAccess telnet) throws IOException,
            AbortedException {
        StringTokenizer st = this.getResults(telnet,
                "show ip route dst 0.0.0.0");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = defaultGWPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public String[] getTrapReceiverAddresses(ConsoleAccess telnet)
            throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show snmp-server traps");
        Matcher matcher;
        LinkedList<String> list = new LinkedList<String>();

        while (st.hasMoreTokens()) {
            matcher = trapReceiverPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                list.add(matcher.group(1));
            }
        }
        if (list.size() == 0)
            return (new String[0]);
        return list.toArray(new String[0]);
    }

    public String[] getSyslogServerAddresses(ConsoleAccess telnet)
            throws IOException, AbortedException {
        return null;
    }

    public String getSysIpAddress(ConsoleAccess telnet) throws IOException,
            AbortedException {
        StringTokenizer st = this.getResults(telnet, "show ip interface");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = ipInterfacePattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(2);
            }
        }
        throw new IOException("parse failed");
    }

    public String getSysIpSubnetMask(ConsoleAccess telnet) throws IOException,
            AbortedException {
        return null;
    }

    public String getSysIpSubnetMask(ConsoleAccess telnet, String ipAddr)
            throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show ip interface");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = ipInterfacePattern.matcher(st.nextToken());
            if (matcher.matches() && ipAddr.equals(matcher.group(2))) {
                return matcher.group(3);
            }
        }
        throw new IOException("parse failed");
    }

    public int[] getChassisEthernetPortIndexs(ConsoleAccess telnet)
            throws IOException, AbortedException {
        return new int[0];
    }

    public int[] getModulePortIndexs(ConsoleAccess telnet, String slotName)
            throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show slot " + slotName);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = slotIndexPattern.matcher(st.nextToken());
            if (matcher.matches() && slotName.equals(matcher.group(1))) {
                int portNum = Integer.parseInt(matcher.group(4));
                int portIndexes[] = new int[portNum];
                for (int i = 0; i < portNum; i++) {
                    portIndexes[i] = i + 1;
                }
                return portIndexes;
            }
        }
        return new int[0];
    }

    public int[] getModuleEthernetPortIndexs(ConsoleAccess telnet,
                                             String slotName) throws IOException, AbortedException {
        if (isEthernetModule(telnet, slotName))
            return getModulePortIndexs(telnet, slotName);
        else
            return new int[0];
    }

    private boolean isEthernetModule(ConsoleAccess telnet, String slotName)
            throws IOException, AbortedException {
        String name = getModuleName(telnet, slotName);
        return name.matches("[01]+BASE[\\w-]*");
    }

    private boolean isAtmModule(ConsoleAccess telnet, String slotName)
            throws IOException, AbortedException {
        String name = getModuleName(telnet, slotName);
        return name.matches("ATM[\\w-]*");
    }

    public EthernetPort.Duplex getEthernetPortDuplex(ConsoleAccess telnet,
                                                     String ifName) throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show interface ethernet "
                + ifName);
        Matcher matcher, matcher2;

        while (st.hasMoreTokens()) {
            String newStr = st.nextToken();
            matcher = duplexPattern.matcher(newStr);
            matcher2 = duplexPattern2.matcher(newStr);
            if (matcher.matches()) {
                if (matcher.group(2).equals("Disabled")) {
                    if (matcher.group(1).matches("Full"))
                        return EthernetPort.Duplex.FULL;
                    if (matcher.group(1).matches("Half"))
                        return EthernetPort.Duplex.HALF;
                } else if (matcher.group(2).equals("Enabled")) {
                    return EthernetPort.Duplex.AUTO;
                }
            }
            if (matcher2.matches()) {
                return EthernetPort.Duplex.AUTO;
            }
        }
        throw new IOException("parse failed");
    }

    public boolean hasAggregationNameAndID(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        return false;
    }

    public int getAggregationID(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        return -1;
    }

    public String getAggregationName(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        return null;
    }

    public String getPhysicalPortType(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        if (!isIfNameString(ifName)) {
            throw new IOException("invalid args");
        }
        String ifNameParsed[] = ifName.split("/");
        StringTokenizer st = this.getResults(telnet, "show slot "
                + ifNameParsed[0]);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = slotIndexPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(5);
            }
        }
        throw new IOException("parse failed");
    }

    public String getPhysicalPortName(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        return null;
    }

    public PortSpeedValue.Admin getPortAdminSpeed(ConsoleAccess telnet,
                                                  String ifName) throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show interface ethernet "
                + ifName);
        Matcher matcher, matcher2;

        while (st.hasMoreTokens()) {
            String newStr = st.nextToken();
            matcher = portTypePattern.matcher(newStr);
            matcher2 = portTypePattern2.matcher(newStr);
            if (matcher.matches()) {
                if (matcher.group(2).equals("Disabled")) {
                    if (matcher.group(1).matches("10BASE.*")) {
                        return new PortSpeedValue.Admin(
                                10 * 1000 * 1000, "10M");
                    }
                    if (matcher.group(1).matches("100BASE.*")) {
                        return new PortSpeedValue.Admin(
                                100 * 1000 * 1000, "100M");
                    }
                    if (matcher.group(1).matches("1000BASE.*")) {
                        return new PortSpeedValue.Admin(
                                1000 * 1000 * 1000, "1000M");
                    }
                } else if (matcher.group(2).equals("Enabled")) {
                    return PortSpeedValue.Admin.AUTO;
                }
            }
            if (matcher2.matches()) {
                return PortSpeedValue.Admin.AUTO;
            }
        }
        throw new IOException("parse failed");
    }

    public String getPortAdminStatus(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show interface ethernet "
                + ifName + " detail");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = adminStatusPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        throw new IOException("parse failed");
    }

    public String getPortIfDescr(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        if (!isIfNameString(ifName)) {
            throw new IOException("invalid args");
        }
        String ifNameParsed[] = ifName.split("/");
        StringTokenizer st = this.getResults(telnet, "show slot "
                + ifNameParsed[0]);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = slotIndexPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(5);
            }
        }
        throw new IOException("parse failed");
    }

    public String getPortOperationalStatus(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show interface ethernet "
                + ifName);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = oprStatusPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        throw new IOException("parse failed");
    }

    public PortSpeedValue.Oper getPortSpeed(ConsoleAccess telnet,
                                            String ifName) throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show interface ethernet "
                + ifName);
        Matcher matcher;

        if (getPortOperationalStatus(telnet, ifName).equals("DOWN"))
            return null;

        while (st.hasMoreTokens()) {
            matcher = portTypePattern.matcher(st.nextToken());
            if (matcher.matches()) {
                if (matcher.group(1).matches("10BASE.*")) {
                    return new PortSpeedValue.Oper(10 * 1000 * 1000,
                            "10M");
                }
                if (matcher.group(1).matches("100BASE.*")) {
                    return new PortSpeedValue.Oper(100 * 1000 * 1000,
                            "100M");
                }
                if (matcher.group(1).matches("1000BASE.*")) {
                    return new PortSpeedValue.Oper(1000 * 1000 * 1000,
                            "1000M");
                }
            }
        }
        throw new IOException("parse failed");
    }

    public String getPortStatus(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        return null;
    }

    public int[] getAtmPhysicalPortIndexs(ConsoleAccess telnet, String slotName)
            throws IOException, AbortedException {
        if (isAtmModule(telnet, slotName))
            return getModulePortIndexs(telnet, slotName);
        else
            return new int[0];
    }

    public String getAtmPhysicalPortType(ConsoleAccess telnet,
                                         String atmPhysicalPortIfName) throws IOException, AbortedException {
        return getPhysicalPortType(telnet, atmPhysicalPortIfName);
    }

    public String getAtmPhysicalPortName(ConsoleAccess telnet,
                                         String atmPhysicalPortIfName) throws IOException, AbortedException {
        return null;
    }

    public String getAtmPhysicalPortAdminSpeed(ConsoleAccess telnet,
                                               String atmPhysicalPortIfName) throws IOException, AbortedException {
        if (!isIfNameString(atmPhysicalPortIfName)) {
            throw new IOException("invalid args");
        }
        String ifNameParsed[] = atmPhysicalPortIfName.split("/");
        StringTokenizer st = this.getResults(telnet, "show slot "
                + ifNameParsed[0]);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = slotIndexPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                String portSpeed[] = matcher.group(3).split(" ");
                return portSpeed[0];
            }
        }
        throw new IOException("parse failed");
    }

    public String getAtmPhysicalPortAdminStatus(ConsoleAccess telnet,
                                                String atmPhysicalPortIfName) throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show atm interface atm "
                + atmPhysicalPortIfName);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = atmIfStatusPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        throw new IOException("parse failed");
    }

    public String getAtmPhysicalPortIfDescr(ConsoleAccess telnet,
                                            String atmPhysicalPortIfName) throws IOException, AbortedException {
        return getPortIfDescr(telnet, atmPhysicalPortIfName);
    }

    public String getAtmPhysicalPortOperationalStatus(ConsoleAccess telnet,
                                                      String atmPhysicalPortIfName) throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show atm interface atm "
                + atmPhysicalPortIfName);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = atmLinkStatusPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        throw new IOException("parse failed");
    }

    public String getAtmPhysicalPortSpeed(ConsoleAccess telnet,
                                          String atmPhysicalPortIfName) throws IOException, AbortedException {
        return getAtmPhysicalPortAdminSpeed(telnet, atmPhysicalPortIfName);
    }

    public String getAtmPhysicalPortStatus(ConsoleAccess telnet,
                                           String atmPhysicalPortIfName) throws IOException, AbortedException {
        return null;
    }

    public String[] getAtmPVCs(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {
        if (!isIfNameString(ifName)) {
            throw new IOException("invalid args");
        }
        StringTokenizer st = this.getResults(telnet,
                "show atm vc interface atm " + ifName);
        Matcher matcher;
        LinkedList<String> PVCs = new LinkedList<String>();

        while (st.hasMoreTokens()) {
            matcher = pvcListPattern.matcher(st.nextToken());
            if (matcher.matches() && ifName.equals(matcher.group(1))) {
                PVCs.add(matcher.group(2) + "/" + matcher.group(3));
            }
        }
        if (PVCs.size() == 0)
            return new String[0];

        return PVCs.toArray(new String[0]);
    }

    public int getVpi(String pvc) throws IOException, AbortedException {
        if (!isPvcString(pvc)) {
            throw new IOException("invalid args");
        }
        String pvcParsed[] = pvc.split("/");
        return Integer.parseInt(pvcParsed[0]);
    }

    public int getVci(String pvc) throws IOException, AbortedException {
        if (!isPvcString(pvc)) {
            throw new IOException("invalid args");
        }
        String pvcParsed[] = pvc.split("/");
        return Integer.parseInt(pvcParsed[1]);
    }

    public String getAtmPVCAdminStatus(ConsoleAccess telnet, String pvc,
                                       String atmPhysicalPortIfName) throws IOException, AbortedException {
        return null;
    }

    public String getAtmPVCOperStatus(ConsoleAccess telnet, String pvc,
                                      String atmPhysicalPortIfName) throws IOException, AbortedException {
        if (!isPvcString(pvc) || !isIfNameString(atmPhysicalPortIfName)) {
            throw new IOException("invalid args");
        }
        String pvcParsed[] = pvc.split("/");
        StringTokenizer st = this.getResults(telnet,
                "show atm vc interface atm " + atmPhysicalPortIfName + " "
                        + pvcParsed[0] + " " + pvcParsed[1]);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = pvcListPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                if (matcher.group(1).equals(atmPhysicalPortIfName)
                        && matcher.group(2).equals(pvcParsed[0])
                        && matcher.group(3).equals(pvcParsed[1])) {
                    return matcher.group(7);
                }
            }
        }
        throw new IOException("parse failed");
    }

    public AtmQosType getQos(ConsoleAccess telnet, String pvc,
                             String atmPhysicalPortIfName) throws IOException, AbortedException {
        if (!isPvcString(pvc) || !isIfNameString(atmPhysicalPortIfName)) {
            throw new IOException("invalid args");
        }

        StringTokenizer st = this.getResults(telnet,
                "show atm vc interface atm " + atmPhysicalPortIfName + " "
                        + getVpi(pvc) + " " + getVci(pvc) + " detail");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = svcCatPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return AtmQosType.valueOf(matcher.group(1));
            }
        }
        throw new IOException("parse failed");
    }

    public long getPcr(ConsoleAccess telnet, String pvc,
                       String atmPhysicalPortIfName) throws IOException, AbortedException {
        if (!isPvcString(pvc) || !isIfNameString(atmPhysicalPortIfName)) {
            throw new IOException("invalid args");
        }
        String pvcParsed[] = pvc.split("/");
        if (isPvcForBridge(telnet, pvc, atmPhysicalPortIfName)) {
            return this.getBridgePortPcr(telnet, pvcParsed[0], pvcParsed[1],
                    atmPhysicalPortIfName);
        } else {
            return this.getAtmPcr(telnet, pvcParsed[0], pvcParsed[1],
                    atmPhysicalPortIfName);
        }
    }

    private long getBridgePortPcr(ConsoleAccess telnet, String vpi, String vci,
                                  String atmPhysicalPortIfName) throws IOException, AbortedException {
        try {
            StringTokenizer st = getResults(telnet
                    .getResponse(SuperHubCollector.show_atm_shapingport));
            Matcher matcher;

            while (st.hasMoreTokens()) {
                matcher = shapingPortPattern.matcher(st.nextToken());
                if (matcher.matches()) {
                    if (matcher.group(1).equals(atmPhysicalPortIfName)
                            && matcher.group(3).equals(vpi)
                            && matcher.group(4).equals(vci)) {
                        return convertKtoM(matcher.group(6));
                    }
                }
            }
            return -1L;
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private long getAtmPcr(ConsoleAccess telnet, String vpi, String vci,
                           String atmPhysicalPortIfName) throws IOException, AbortedException {

        StringTokenizer st = this.getResults(telnet,
                "show atm vc interface atm " + atmPhysicalPortIfName + " "
                        + vpi + " " + vci + " detail");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = pcrPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                String pcr = matcher.group(1);
                if (pcr.matches("[0-9]+"))
                    return convertKtoM(pcr);
                else
                    return -1L;
            }
        }
        throw new IOException("parse failed");
    }

    private boolean isPvcForBridge(ConsoleAccess telnet, String pvc,
                                   String atmPhysicalPortIfName) throws IOException, AbortedException {
        return getXetherPortIfName(telnet, pvc, atmPhysicalPortIfName) != null;
    }

    private long convertKtoM(String rate) {
        float pcr = Float.parseFloat(rate);
        return (long) (pcr * 1000);
    }

    public long getMcr(ConsoleAccess telnet, String pvc,
                       String atmPhysicalPortIfName) throws IOException, AbortedException {
        if (!isPvcString(pvc) || !isIfNameString(atmPhysicalPortIfName)) {
            throw new IOException("invalid args");
        }

        StringTokenizer st = this.getResults(telnet,
                "show atm vc interface atm " + atmPhysicalPortIfName + " "
                        + getVpi(pvc) + " " + getVci(pvc) + " detail");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = mcrPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                String mcr = matcher.group(1);
                if (mcr.matches("[0-9]+")) {
                    return Long.parseLong(mcr) * 1000L;
                } else {
                    return -1L;
                }
            }
        }
        throw new IOException("parse failed");
    }

    public int getBridgePortNumber(ConsoleAccess telnet, String pvc,
                                   String atmPhysicalPortIfName) throws IOException, AbortedException {
        String etherIfName = getXetherPortIfName(telnet, pvc,
                atmPhysicalPortIfName);
        if (etherIfName == null)
            return -1;
        return this.getBridgePortNumber(telnet, etherIfName);
    }

    public int getBridgePortNumber(ConsoleAccess telnet, String ifName)
            throws IOException, AbortedException {

        StringTokenizer st = this.getResults(telnet,
                "show bridge port interface ethernet " + ifName);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = bridgePortPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                int numOfBridgePorts = 0;
                for (int i = 0; i < matcher.groupCount(); i++) {
                    if (matcher.group(i + 1).matches("[0-9]+")) {
                        numOfBridgePorts++;
                    } else {
                        break;
                    }
                }
                if (numOfBridgePorts == 0)
                    return -1;
                int bridgePortList[] = new int[numOfBridgePorts];
                for (int i = 0; i < numOfBridgePorts; i++) {
                    bridgePortList[i] = Integer.parseInt(matcher.group(i + 1));
                }
                return bridgePortList[numOfBridgePorts - 1];
            }
        }
        throw new IOException("parse failed");
    }

    private String getXetherPortIfName(ConsoleAccess telnet, String pvc,
                                       String atmPhysicalPortIfName) throws IOException, AbortedException {
        if (!isPvcString(pvc)) {
            throw new IOException("invalid args");
        }
        String pvcParsed[] = pvc.split("/");
        StringTokenizer st = this.getResults(telnet, "show atm vc ethernet");
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = pvcListPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                if (matcher.group(1).equals(atmPhysicalPortIfName)
                        && matcher.group(2).equals(pvcParsed[0])
                        && matcher.group(3).equals(pvcParsed[1])) {
                    return matcher.group(4);
                }
            }
        }
        return null;
    }

    public String getBridgeAdminStatus(ConsoleAccess telnet,
                                       int bridgePortNumber) throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show bridge vlan "
                + bridgePortNumber);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = vlanNoPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return "UP";
            }
        }
        return "DOWN";
    }

    public String getBridgeOperStatus(ConsoleAccess telnet, int bridgePortNumber)
            throws IOException, AbortedException {
        StringTokenizer st = this.getResults(telnet, "show bridge vlan "
                + bridgePortNumber);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = vlanStatusPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        throw new IOException("parse failed");
    }

    public String[] getVlanIfName(ConsoleAccess telnet, int bridgePortNumber)
            throws IOException, AbortedException {
        String[] result = new String[1];
        result[0] = "vlan " + bridgePortNumber;
        return result;
    }

    public int getVlanEncapsIfTag(ConsoleAccess telnet, String vlanIfName)
            throws IOException, AbortedException {
        String vlanIfNameParsed[] = vlanIfName.split(" ");
        if (vlanIfNameParsed[0].equals("vlan") == false) {
            throw new IOException("invalid args");
        }
        StringTokenizer st = this.getResults(telnet, "show bridge vlan "
                + vlanIfNameParsed[1]);
        Matcher matcher;

        while (st.hasMoreTokens()) {
            matcher = vlanNoPattern.matcher(st.nextToken());
            if (matcher.matches()) {
                if (matcher.group(3).matches("None"))
                    return -1;
                if (matcher.group(3).matches("[0-9]+"))
                    return Integer.parseInt(matcher.group(3));
            }
        }
        throw new IOException("parse failed");
    }

    public boolean isTaggedVlan(ConsoleAccess telnet, String vlanIfName,
                                int bridgePortIndex) throws IOException, AbortedException {
        String vlanIfNameParsed[] = vlanIfName.split(" ");
        if (vlanIfNameParsed[0].equals("vlan") == false) {
            throw new IOException("invalid args");
        }
        StringTokenizer st = this.getResults(telnet, "show bridge vlan "
                + vlanIfNameParsed[1]);
        Matcher matcher;
        boolean isTagged = false;

        while (st.hasMoreTokens()) {
            if (st
                    .nextToken()
                    .matches(
                            "\\s*Interface\\(s\\)\\s*State\\s*Tagging\\s*AccessList\\s*")) {
                if (st.nextToken().matches(
                        "\\s*\\(priority\\)\\s*Input\\s*Output\\s*")) {
                    while (st.hasMoreTokens()) {
                        matcher = vlanIfPattern.matcher(st.nextToken());
                        if (matcher.matches()) {
                            if (matcher.group(2).matches("tagging\\([0-7]\\)")) {
                                isTagged = true;
                            }
                        }
                    }
                }
            }
        }
        return isTagged;
    }

    public String[] getEthernetIfNames(ConsoleAccess telnet, String vlanIfName)
            throws IOException, AbortedException {
        String vlanIfNameParsed[] = vlanIfName.split(" ");
        if (vlanIfNameParsed[0].equals("vlan") == false) {
            throw new IOException("invalid args");
        }
        StringTokenizer st = this.getResults(telnet, "show bridge vlan "
                + vlanIfNameParsed[1]);
        Matcher matcher;
        LinkedList<String> etherIfList = new LinkedList<String>();

        while (st.hasMoreTokens()) {
            if (st
                    .nextToken()
                    .matches(
                            "\\s*Interface\\(s\\)\\s*State\\s*Tagging\\s*AccessList\\s*")) {
                if (st.nextToken().matches(
                        "\\s*\\(priority\\)\\s*Input\\s*Output\\s*")) {
                    while (st.hasMoreTokens()) {
                        matcher = vlanIfPattern.matcher(st.nextToken());
                        if (matcher.matches()) {
                            etherIfList.add(matcher.group(1));
                        }
                    }
                }
            }
        }
        if (etherIfList.size() == 0)
            return new String[0];
        else
            return etherIfList.toArray(new String[0]);
    }

    public String getBridgePortName(ConsoleAccess telnet, int bridgePortNumber)
            throws IOException, AbortedException {
        return null;
    }

    private StringTokenizer getResults(ConsoleAccess telnet, String cmd)
            throws IOException, AbortedException {
        try {
            ConsoleCommand command = new ConsoleCommand(new GlobalMode(), cmd);
            return getResults(telnet.getResponse(command));
        } catch (ConsoleException e) {
            throw new IOException(e);
        }
    }

    private StringTokenizer getResults(String value) throws IOException,
            AbortedException {
        if (value.matches("# Incomplete command.*")
                || value.matches("# Command not found.*")
                || value.matches(".*Unrecognized parameter.*")) {
            throw new InvalidCommandException();
        }
        return new StringTokenizer(value, "\n");
    }

    private boolean isIfNameString(String ifName) {
        return ifName.matches("^[1-4LR]+/[1-4]$");
    }

    private boolean isPvcString(String pvc) {
        return pvc.matches("^[0-9]+/[0-9]+$");
    }

    public String getVlanIfDescr(ConsoleAccess telnet, String vlanIfName)
            throws IOException, AbortedException {
        return null;
    }

    public void supplementInterfaceAttributes(SnmpAccess snmp, EAConverter ea) throws IOException, ConsoleException, AbortedException {
    }
}