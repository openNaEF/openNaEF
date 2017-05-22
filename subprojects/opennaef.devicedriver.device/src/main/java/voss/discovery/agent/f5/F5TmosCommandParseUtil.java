package voss.discovery.agent.f5;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.*;
import voss.util.VossMiscUtility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class F5TmosCommandParseUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(F5TmosCommandParseUtil.class);
    private static final Pattern ipv4MaskPattern = Pattern.compile("ip address ([0-9.]+) ([0-9.]+) ?(secondary)?");

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
            String subnetMaskPart = matcher.group(2);
            byte[] bytes = VossMiscUtility.getByteFormIpAddress(subnetMaskPart);
            int length = 0;
            for (byte b : bytes) {
                length = length + Integer.bitCount(b & 0xff);
            }
            return length;
        }
        throw new IllegalArgumentException("invalid arg: [" + line + "]");
    }

    public static boolean isSubInterface(String command) {
        return command.lastIndexOf('.') > -1;
    }

    public static String getParentInterfaceName(String subInterfaceName) {
        if (!isSubInterface(subInterfaceName)) {
            return subInterfaceName;
        }
        return subInterfaceName.substring(0, subInterfaceName.lastIndexOf('.'));
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

    public static PhysicalPort createJuniperPhysicalPort(Device device, String ifname) {
        if (ifname == null) {
            throw new IllegalArgumentException("ifname is null");
        }
        if (startsWith(ifname, true, "te", "ge", "fe", "et")) {
            EthernetPort port = new EthernetPortImpl();
            port.initDevice(device);
            port.initIfName(ifname);
            return port;
        } else if (startsWith(ifname, true, "se")) {
            SerialPortImpl serial = new SerialPortImpl();
            serial.initDevice(device);
            serial.initIfName(ifname);
            return serial;
        } else if (startsWith(ifname, true, "at")) {
            AtmPortImpl atm = new AtmPortImpl();
            atm.initDevice(device);
            atm.initIfName(ifname);
            return atm;
        } else if (startsWith(ifname, true, "so")) {
            POSImpl pos = new POSImpl();
            pos.initDevice(device);
            pos.initIfName(ifname);
            return pos;
        }
        throw new IllegalStateException("unsupported port type: " + ifname);
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