package voss.discovery.agent.cisconexus;


import voss.discovery.agent.mib.Mib2Impl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CiscoNxOsCommandParseUtil {
    private final static Map<String, String> shortToLong = new HashMap<String, String>();
    private final static Map<String, String> longToShort = new HashMap<String, String>();
    private final static Pattern ipv4MaskPattern = Pattern.compile("ip address ([0-9.]+)/([0-9]+) ?(secondary)?");

    static {
        shortToLong.put("Eth", "Ethernet");
        longToShort.put("Ethernet", "Eth");

        shortToLong.put("po", "port-channel");
        longToShort.put("port-channel", "po");

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
        throw new IllegalArgumentException("invalid arg: [" + line + "]");
    }

    public static int getIpAddressMaskLength(String line) {
        line = line.toLowerCase();
        Matcher matcher = ipv4MaskPattern.matcher(line);
        if (matcher.matches()) {
            String subnetMaskLengthPart = matcher.group(2);
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

    public static String getIfName(String longName) {
        Pattern pattern0 = Pattern.compile("Fex-([0-9]+) FixedModule-([0-9]+) Uplink Port-([0-9]+)(.*)?");
        Pattern pattern1 = Pattern.compile("Fex-([0-9]+) FixedModule-([0-9]+) Downlink Port-([0-9]+)(.*)?");
        Pattern pattern2 = Pattern.compile("FixedModule-([0-9]+) Port-([0-9]+)(.*)?");
        Matcher matcher0 = pattern0.matcher(longName);
        Matcher matcher1 = pattern1.matcher(longName);
        Matcher matcher2 = pattern2.matcher(longName);
        if (matcher0.matches()) {
            return "Uplink" + matcher0.group(1) + "/" + matcher0.group(2) + "/" + matcher0.group(3);
        } else if (matcher1.matches()) {
            return "Ethernet" + matcher1.group(1) + "/" + matcher1.group(2) + "/" + matcher1.group(3);
        } else if (matcher2.matches()) {
            return "Ethernet" + matcher2.group(1) + "/" + matcher2.group(2);
        } else {
            throw new IllegalArgumentException("unknown interface syntax: " + longName);
        }
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


    public static PhysicalPort createCiscoPhysicalPort(SnmpAccess snmp, Device device, String ifname)
            throws IOException, AbortedException {
        if (ifname == null) {
            throw new IllegalArgumentException("ifname is null");
        }
        PhysicalPort result;
        String _ifname = ifname.toLowerCase();
        if (startsWith(_ifname, true, "ethernet", "uplink")) {
            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initIfName(ifname);
            if (ifname.toLowerCase().startsWith("uplink")) {
                port.setPortTypeName("Uplink(FEX)");
            }
            result = port;
        } else {
            throw new IllegalStateException("unsupported port type: " + ifname);
        }
        Mib2Impl.setIfIndex(snmp, result, ifname);
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