package voss.discovery.agent.extreme;

public class IfNameProcessor {
    public static IfNameProcessor getProcessor(boolean isFixedChassis) {
        return new IfNameProcessor(isFixedChassis);
    }

    private static final String configDelimiter = ":";
    private static final String delimiter = "[:/]";

    private final boolean isFixedChassis;

    private IfNameProcessor(boolean fixedChassis) {
        this.isFixedChassis = fixedChassis;
    }

    public String getIfName(String ifName) {
        String[] slotport = ifName.split(delimiter);
        if (slotport.length != 2) {
            return ifName;
        }

        if (isFixedChassis) {
            return slotport[1];
        } else {
            return slotport[0] + configDelimiter + slotport[1];
        }
    }

    public String getIfName(int slotID, int portID) {
        if (isFixedChassis) {
            return String.valueOf(portID);
        } else {
            return slotID + configDelimiter + portID;
        }
    }

    public String getPortName(String ifName) {
        if (ifName == null) {
            throw new IllegalArgumentException();
        }

        if (ifName.indexOf(':') > -1) {
            return ifName.split(":")[1];
        }
        if (ifName.indexOf('/') > -1) {
            return ifName.split("/")[1];
        }
        return ifName;
    }

    public int getPortID(String ifName) {
        String portName = getPortName(ifName);
        return Integer.parseInt(portName);
    }

    public String getSlotName(String ifName) {
        if (isFixedChassis) {
            throw new IllegalStateException("no slots on fixed chassis.");
        }

        if (ifName == null) {
            throw new IllegalArgumentException();
        }

        if (ifName.indexOf(':') > -1) {
            return ifName.split(":")[0];
        }
        if (ifName.indexOf('/') > -1) {
            return ifName.split("/")[0];
        }
        return null;
    }

    public Integer getSlotID(String ifName) {
        String slotName = getSlotName(ifName);
        return Integer.parseInt(slotName);
    }

}