package voss.discovery.agent.util;

import voss.model.DefaultLogicalEthernetPort;
import voss.model.LogicalEthernetPort;
import voss.model.NotInitializedException;
import voss.model.PhysicalPort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private Utils() {
    }

    public static int getIfIndex(LogicalEthernetPort le) {
        if (le instanceof DefaultLogicalEthernetPort) {
            DefaultLogicalEthernetPort dle = (DefaultLogicalEthernetPort) le;
            try {
                return dle.getIfIndex();
            } catch (NotInitializedException e) {
                return dle.getPhysicalPort().getIfIndex();
            }
        }
        return le.getIfIndex();
    }

    public static String getSlotPart(String ifName, char delim) {
        if (ifName == null) {
            return null;
        }
        int index = ifName.lastIndexOf(delim);
        if (index == -1) {
            return ifName;
        }
        return ifName.substring(0, index);
    }

    public static String getPortPart(String ifName, char delim) {
        if (ifName == null) {
            return null;
        }
        int index = ifName.lastIndexOf(delim);
        if (index == -1) {
            return ifName;
        }
        return ifName.substring(index + 1);
    }

    private static final Pattern slotIDPattern = Pattern.compile("[0-9]+");

    public static int getSlotID(PhysicalPort port) {
        if (port.getModule() == null || port.getModule().getSlot() == null) {
            return 0;
        }
        try {
            int slotID = port.getModule().getSlot().getSlotIndex();
            return slotID;
        } catch (RuntimeException e) {
            Matcher matcher = slotIDPattern.matcher(port.getModule().getSlot().getSlotId());
            if (matcher.matches()) {
                return Integer.parseInt(port.getModule().getSlot().getSlotId());
            } else {
                return -1;
            }
        }
    }

    private static final Pattern portIDPattern = Pattern.compile(".*([0-9]+)");

    public static int getPortID(PhysicalPort port) {
        try {
            int portID = port.getPortIndex();
            return portID;
        } catch (RuntimeException e) {
            Matcher matcher = portIDPattern.matcher(port.getIfName());
            if (matcher.matches()) {
                return Integer.parseInt(matcher.group(1));
            } else {
                return -1;
            }
        }
    }

    public static boolean isNull(Object... args) {
        for (Object o : args) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    public static <T> T getInstanceOf(Class<T> cls, Object... args) {
        if (cls == null) {
            throw new IllegalArgumentException("no class.");
        } else if (args.length == 0) {
            return null;
        }
        for (Object arg : args) {
            if (cls.isInstance(arg)) {
                return cls.cast(arg);
            }
        }
        return null;
    }

    public static <T> List<T> getInstancesOf(Class<T> cls, Object... args) {
        List<T> result = new ArrayList<T>();
        if (cls == null) {
            throw new IllegalArgumentException("no class.");
        } else if (args.length == 0) {
            return result;
        }
        for (Object arg : args) {
            if (cls.isInstance(arg)) {
                result.add(cls.cast(arg));
            }
        }
        return result;
    }

    public static List<String> filter(List<String> contents, Collection<String> filter) {
        List<String> result = new ArrayList<String>();
        for (String line : contents) {
            if (match(line, filter)) {
                continue;
            }
            result.add(line);
        }
        return result;
    }

    private static boolean match(String line, Collection<String> keywords) {
        for (String keyword : keywords) {
            if (line.matches(keyword)) {
                return true;
            }
        }
        return false;
    }
}