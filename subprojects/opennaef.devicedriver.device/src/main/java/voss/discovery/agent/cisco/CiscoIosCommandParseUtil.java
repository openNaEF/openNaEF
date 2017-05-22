package voss.discovery.agent.cisco;


import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.*;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CiscoIosCommandParseUtil {
    private final static Map<String, String> shortToLong = new HashMap<String, String>();
    private final static Map<String, String> longToShort = new HashMap<String, String>();
    private final static Pattern ipv4MaskPattern = Pattern.compile("ip address ([0-9.]+) ([0-9.]+) ?(secondary)?");
    private final static Pattern ipv4MaskPattern2 = Pattern.compile("ip address ([0-9.]+)/([0-9]+) ?(secondary)?");

    static {
        shortToLong.put("Et", "Ethernet");
        longToShort.put("Ethernet", "Et");

        shortToLong.put("Fa", "FastEthernet");
        longToShort.put("FastEthernet", "Fa");

        shortToLong.put("Gi", "GigabitEthernet");
        longToShort.put("GigabitEthernet", "Gi");

        shortToLong.put("Te", "TenGigabitEthernet");
        longToShort.put("TenGigabitEthernet", "Te");

        shortToLong.put("Po", "Port-channel");
        longToShort.put("Port-channel", "Po");

        shortToLong.put("po", "port-channel");
        longToShort.put("port-channel", "po");

        shortToLong.put("PO", "POS");
        longToShort.put("POS", "PO");

        shortToLong.put("SO", "SONET");
        longToShort.put("SONET", "SO");

        shortToLong.put("AT", "ATM");
        longToShort.put("ATM", "AT");

        shortToLong.put("Se", "Serial");
        longToShort.put("Serial", "Se");

        shortToLong.put("Lo", "Loopback");
        longToShort.put("Loopback", "Lo");

        shortToLong.put("Tu", "Tunnel");
        longToShort.put("Tunnel", "Tu");

        shortToLong.put("Nu", "Null");
        longToShort.put("Null", "Nu");

        shortToLong.put("Vl", "Vlan");
        longToShort.put("Vlan", "Vl");

        shortToLong.put("EO", "EOBC");
        longToShort.put("EOBC", "EO");

        shortToLong.put("Cc", "Container");
        longToShort.put("Container", "Cc");

        shortToLong.put("T1", "T1");
        longToShort.put("T1", "T1");

        shortToLong.put("T3", "T3");
        longToShort.put("T3", "T3");

        shortToLong.put("E1", "E1");
        longToShort.put("E1", "E1");

        shortToLong.put("E3", "E3");
        longToShort.put("E3", "E3");

        shortToLong.put("Tu", "Tunnel");
        longToShort.put("Tunnel", "Tu");

        shortToLong.put("Pa", "Path");
        longToShort.put("Path", "Pa");

        shortToLong.put("Mu", "Multilink");
        longToShort.put("Multilink", "Mu");

        shortToLong.put("CB", "CBR");
        longToShort.put("CBR", "CB");

        shortToLong.put("Vi", "Virtual-Template");
        longToShort.put("Virtual-Template", "Vi");

        shortToLong.put("mgmt", "mgmt");
        longToShort.put("mgmt", "mgmt");

    }

    public static String getIpAddress(String line) {
        line = line.toLowerCase();
        Matcher matcher = ipv4MaskPattern.matcher(line);
        if (matcher.matches()) {
            String ipAddressPart = matcher.group(1);
            return ipAddressPart;
        }
        Matcher matcher2 = ipv4MaskPattern2.matcher(line);
        if (matcher2.matches()) {
            String ipAddressPart = matcher2.group(1);
            return ipAddressPart;
        }
        throw new IllegalArgumentException("invalid arg: [" + line + "]");
    }

    public static int getIpAddressMaskLength(String line) {
        line = line.toLowerCase();
        Matcher matcher = ipv4MaskPattern.matcher(line);
        if (matcher.matches()) {
            String subnetMaskPart = matcher.group(2);
            byte[] bytes = VossMiscUtility.getByteFormIpAddress(subnetMaskPart);
            int length = 0;
            for (byte b : bytes) {
                length = length + Integer.bitCount(b & 0xff);
            }
            return length;
        }
        Matcher matcher2 = ipv4MaskPattern2.matcher(line);
        if (matcher2.matches()) {
            String subnetMaskLengthPart = matcher2.group(2);
            int length = Integer.parseInt(subnetMaskLengthPart);
            return length;
        }
        throw new IllegalArgumentException("invalid arg: [" + line + "]");
    }

    private final static Pattern interfaceNamePattern1 = Pattern.compile("([A-Za-z]+[0-9/:.]+)( .*)?");
    private final static Pattern interfaceNamePattern2 = Pattern.compile("([A-Za-z0-9]+ [0-9/:.]+)( .*)?");

    public static String getInterfaceName(String command) {
        assertArgument(command);
        if (command.startsWith("interface ")) {
            command = command.substring(10);
        }
        Matcher matcher1 = interfaceNamePattern1.matcher(command);
        if (matcher1.matches()) {
            return matcher1.group(1);
        }
        Matcher matcher2 = interfaceNamePattern2.matcher(command);
        if (matcher2.matches()) {
            return matcher2.group(1);
        }
        throw new IllegalArgumentException("invalid arg: [" + command + "]");
    }

    public static boolean isSubInterface(String command) {
        String s = getInterfaceName(command);
        return s.lastIndexOf('.') > -1;
    }

    public static String getParentInterfaceName(String subInterfaceName) {
        if (!isSubInterface(subInterfaceName)) {
            return getInterfaceName(subInterfaceName);
        }

        return getInterfaceName(subInterfaceName.substring(0, subInterfaceName.lastIndexOf('.')));
    }

    public static String getSubInterfaceId(String interfaceName) {
        if (!isSubInterface(interfaceName)) {
            return null;
        }

        return interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
    }

    private static final Pattern slotModulePortPattern = Pattern.compile("[A-Za-z]* ?([0-9/]+) ?.*");

    public static Container getParentContainerByIfName(Device device, String ifName) {
        if (device == null || ifName == null) {
            return null;
        }

        if (ifName.indexOf('/') == -1) {
            return device;
        }

        Matcher matcher = slotModulePortPattern.matcher(ifName);
        if (matcher.matches()) {
            String s = matcher.group(1);
            String slotStructure = s.substring(0, s.lastIndexOf("/"));
            Slot slot = device.getSlotBySlotId(slotStructure);
            if (slot != null) {
                return slot.getModule();
            }
        }
        return null;
    }

    public static String getFullyQualifiedInterfaceName(String ifname) {
        Pattern pattern = Pattern.compile("([A-Za-z-]+)( ?[0-9/.]+)");
        Matcher matcher = pattern.matcher(ifname);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(2);
            String longPrefix = shortToLong.get(prefix);

            if (longPrefix == null) {
                if (longToShort.get(prefix) != null) {
                    return ifname;
                }
                return ifname;
            }

            return longPrefix + suffix;
        } else {
            return ifname;
        }
    }

    public static String getShortIfName(String longName) {
        Pattern pattern0 = Pattern.compile("SONET ([0-9/.:]+) ?(.*)?");
        Pattern pattern1 = Pattern.compile("([A-Za-z0-9-]+) ([0-9/.:]+) ?(.*)?");
        Pattern pattern2 = Pattern.compile("([A-Za-z-]+)([0-9/.:]+) ?(.*)?");
        Matcher matcher0 = pattern0.matcher(longName);
        Matcher matcher1 = pattern1.matcher(longName);
        Matcher matcher2 = pattern2.matcher(longName);
        if (matcher0.matches()) {
            return getShortIfName(longName, "SO", matcher0.group(1), "");
        } else if (matcher1.matches()) {
            return getShortIfName(longName, matcher1.group(1), matcher1.group(2), " ");
        } else if (matcher2.matches()) {
            return getShortIfName(longName, matcher2.group(1), matcher2.group(2), "");
        } else {
            throw new IllegalArgumentException("unknown interface syntax: " + longName);
        }
    }

    private static String getShortIfName(String longName, String prefix, String suffix, String joint) {
        String shortPrefix = longToShort.get(prefix);
        if (shortPrefix == null) {
            if (shortToLong.get(prefix) != null) {
                return longName;
            }
            throw new IllegalArgumentException("unknown interface name type: " + prefix + ", " + suffix);
        }

        return shortPrefix + joint + suffix;
    }

    private static void assertArgument(String line) {
        if (line == null) {
            throw new IllegalArgumentException();
        }
    }

    public static final String XCONNECT_COMMAND = "xconnect";
    public static final String PW_PEER_IP_ADDRESS = "peer-ip-address";
    public static final String PW_PEER_PW_ID = "vc-id";
    public static final String PW_ENCAPSULATION = "encapsulation";
    public static final String PW_CLASS = "pw-class";

    public static Map<String, String> getXconnectOptions(String line) {
        if (!line.startsWith(XCONNECT_COMMAND)) {
            throw new IllegalArgumentException();
        }

        String[] elements = line.split(" ");
        Map<String, String> result = new HashMap<String, String>();
        int pos = 0;
        while (pos < elements.length) {
            if (elements[pos].equals(XCONNECT_COMMAND)) {
                result.put(PW_PEER_IP_ADDRESS, elements[++pos]);
                result.put(PW_PEER_PW_ID, elements[++pos]);
            } else if (elements[pos].equals("encapsulation")) {
                if (pos + 2 < elements.length && elements[pos + 2].equals("manual")) {
                    result.put(PW_ENCAPSULATION, elements[++pos] + "-manual");
                    pos++;
                } else {
                    result.put(PW_ENCAPSULATION, elements[++pos]);
                }
            } else if (elements[pos].equals(PW_CLASS)) {
                result.put(PW_CLASS, elements[++pos]);
            } else {
                throw new IllegalStateException("unknown option: [" + elements[pos] + "] in [" + line + "]");
            }
            pos++;
        }
        return result;
    }

    public static final String IP_EXPLICIT_PATH_COMMAND = "ip explicit-path";
    public static final String IP_EXPLICIT_PATH_NAME = "name";
    public static final String IP_EXPLICIT_PATH_IDENTIFIER = "identifier";
    public static final String IP_EXPLICIT_PATH_STATUS = "ip explicit-path status";
    public static final String IP_EXPLICIT_PATH_ENABLE = "enable";
    public static final String IP_EXPLICIT_PATH_DISABLE = "disable";

    public static Map<String, String> getIpExplicitPathOptions(String line) {
        if (!line.startsWith(IP_EXPLICIT_PATH_COMMAND)) {
            throw new IllegalArgumentException();
        }

        line = line.replace(IP_EXPLICIT_PATH_COMMAND, "").trim();
        String[] elements = line.split(" ");
        Map<String, String> result = new HashMap<String, String>();
        int pos = 0;
        while (pos < elements.length) {
            if (elements[pos].equals(IP_EXPLICIT_PATH_NAME)) {
                result.put(IP_EXPLICIT_PATH_NAME, elements[++pos]);
            } else if (elements[pos].equals(IP_EXPLICIT_PATH_IDENTIFIER)) {
                result.put(IP_EXPLICIT_PATH_IDENTIFIER, elements[++pos]);
            } else if (elements[pos].equals(IP_EXPLICIT_PATH_ENABLE)) {
                result.put(IP_EXPLICIT_PATH_STATUS, IP_EXPLICIT_PATH_ENABLE);
            } else if (elements[pos].equals(IP_EXPLICIT_PATH_DISABLE)) {
                result.put(IP_EXPLICIT_PATH_STATUS, IP_EXPLICIT_PATH_DISABLE);
            } else {
                throw new IllegalStateException("unknown option: [" + elements[pos] + "] in [" + line + "]");
            }
            pos++;
        }
        return result;
    }

    public static final String vlanIDRegex = "encapsulation dot1Q ([0-9]+) ?.*";
    private static final Pattern vlanIDPattern = Pattern.compile(vlanIDRegex);

    public static String getVlanID(String line) {
        Matcher matcher = vlanIDPattern.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    public static final String NEXT_ADDRESS = "next-address";

    public static String getNextAddress(String line) {
        if (!line.startsWith(NEXT_ADDRESS)) {
            throw new IllegalArgumentException();
        }

        line = line.replace(NEXT_ADDRESS, "").trim();
        return line;
    }

    static String guessIfName(String portName) {
        Pattern regularName = Pattern.compile("[A-Za-z]+ ?[0-9/.]+");
        if (regularName.matcher(portName).matches()) {
            return portName;
        }

        portName = portName.replace("Port Container ", "");
        String[] e = portName.split(" ");
        String lastPart = e[e.length - 1];
        if (portName.startsWith("Gi")) {
            return "Gi" + lastPart;
        } else if (portName.startsWith("Te")) {
            return "Te" + lastPart;
        } else if (portName.startsWith("Fa")) {
            return "Fa" + lastPart;
        } else if (portName.startsWith("Et")) {
            return "Et" + lastPart;
        } else if (portName.startsWith("AT")) {
            return "AT" + lastPart;
        } else if (portName.startsWith("POS")) {
            return "POS" + lastPart;
        } else if (portName.startsWith("Se")) {
            return "Se" + lastPart;
        }
        return portName;
    }


    public static PhysicalPort createCiscoPhysicalPort(SnmpAccess snmp, Device device, String ifname, int ifIndex)
            throws IOException, AbortedException {
        if (ifname == null) {
            throw new IllegalArgumentException("ifname is null");
        }
        PhysicalPort result;
        if (startsWith(ifname, true, "te", "gi", "fa", "et")) {
            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            result = port;
        } else if (startsWith(ifname, true, "se", "t1", "t3", "e1", "e3", "so")) {
            SerialPortImpl serial = new SerialPortImpl();
            serial.initDevice(device);
            result = serial;
        } else if (startsWith(ifname, true, "at", "cb")) {
            AtmPortImpl atm = new AtmPortImpl();
            atm.initDevice(device);
            result = atm;
        } else if (startsWith(ifname, true, "po")) {
            POSImpl pos = new POSImpl();
            pos.initDevice(device);
            result = pos;
        } else {
            throw new IllegalStateException("unsupported port type: " + ifname);
        }
        if (ifIndex == -1) {
            Mib2Impl.setIfIndex(snmp, result, ifname);
        } else {
            result.initIfIndex(ifIndex);
        }
        return result;
    }

    public static boolean startsWith(String s, boolean caseIgnore, String... args) {
        if (s == null) {
            return false;
        }
        for (String arg : args) {
            if (caseIgnore) {
                s = s.toLowerCase();
                arg = arg.toLowerCase();
            }
            if (s.startsWith(arg)) {
                return true;
            }
        }
        return false;
    }
}