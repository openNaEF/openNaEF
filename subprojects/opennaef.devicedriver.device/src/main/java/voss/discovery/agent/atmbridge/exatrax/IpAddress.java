package voss.discovery.agent.atmbridge.exatrax;

public class IpAddress {
    int address[];
    int mask[];

    public IpAddress(String addrStr, String maskStr) {
        address = strToInts(addrStr);
        mask = strToInts(maskStr);
    }

    public IpAddress(String addrStr) {
        this(addrStr, "255.255.255.255");
    }

    public IpAddress() {
        address = new int[4];
        mask = new int[4];
    }

    public String toString() {
        return intsToStr(address) + "/" + intsToStr(mask);
    }

    public String getAddressStr() {
        return intsToStr(address);
    }

    public String getNetmaskStr() {
        return intsToStr(mask);
    }

    private String intsToStr(int octs[]) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < octs.length; i++) {
            str.append(octs[i]);
            if (i < octs.length - 1) {
                str.append(".");
            }
        }
        return str.toString();
    }

    private int[] strToInts(String addrStr) {
        String[] octStr = addrStr.split("\\.");
        int[] octs = new int[4];

        int i;
        try {
            for (i = 0; i < octs.length; i++) {
                octs[i] = Integer.parseInt(octStr[i]);
            }
        } catch (NumberFormatException e) {
        }

        return octs;
    }
}