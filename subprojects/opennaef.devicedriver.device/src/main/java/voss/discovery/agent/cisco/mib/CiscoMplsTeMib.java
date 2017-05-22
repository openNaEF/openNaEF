package voss.discovery.agent.cisco.mib;

public class CiscoMplsTeMib {
    public static final String CISCO_TE_MIB_BASE = ".1.3.6.1.3.95.2";
    public static final String mplsTeObjects = CISCO_TE_MIB_BASE + ".2";
    public static final String mplsTunnelHopEntry = CISCO_TE_MIB_BASE + ".4";
    public static final String mplsTunnelResourceTable = CISCO_TE_MIB_BASE + ".6";
    public static final String mplsTunnelARHopTable = CISCO_TE_MIB_BASE + ".7";
    public static final String mplsTunnelCHopTable = CISCO_TE_MIB_BASE + ".8";

    public static final String mplsTunnelEntry = mplsTeObjects + ".1";

    public static final String mplsTunnelName = mplsTunnelEntry + ".5";
    public static final String mplsTunnelDescr = mplsTunnelEntry + ".6";
    public static final String mplsTunnelIsIf = mplsTunnelEntry + ".7";
    public static final String mplsTunnelIfIndex = mplsTunnelEntry + ".8";
    public static final String mplsTunnelXCPointer = mplsTunnelEntry + ".9";
    public static final String mplsTunnelSessionAttributes = mplsTunnelEntry + ".13";
    public static final String mplsTunnelResourcePointer = mplsTunnelEntry + ".16";
    public static final String mplsTunnelHopTableIndex = mplsTunnelEntry + ".18";
    public static final String mplsTunnelARHopTableIndex = mplsTunnelEntry + ".19";
    public static final String mplsTunnelCHopTableIndex = mplsTunnelEntry + ".20";
    public static final String mplsTunnelRole = mplsTunnelEntry + ".31";
    public static final String mplsTunnelAdminStatus = mplsTunnelEntry + ".34";
    public static final String mplsTunnelOperStatus = mplsTunnelEntry + ".35";

    public static final int tunnelSessionAttribute_fastReroute = 0;
    public static final int tunnelSessionAttribute_mergePermitted = 1;
    public static final int tunnelSessionAttribute_isPersistent = 2;
    public static final int tunnelSessionAttribute_isPinned = 3;
    public static final int tunnelSessionAttribute_isComputed = 4;
    public static final int tunnelSessionAttribute_recordRoute = 5;

    public static final String mplsTunnelARHopAddrType = mplsTunnelARHopTable + ".1.3";
    public static final String mplsTunnelARHopIpv4Addr = mplsTunnelARHopTable + ".1.4";
    public static final String mplsTunnelARHopIpv4PrefixLen = mplsTunnelARHopTable + ".1.5";
    public static final String mplsTunnelARHopIpv6Addr = mplsTunnelARHopTable + ".1.6";
    public static final String mplsTunnelARHopIpv6PrefixLen = mplsTunnelARHopTable + ".1.7";
    public static final String mplsTunnelARHopAsNumber = mplsTunnelARHopTable + ".1.8";
    public static final String mplsTunnelARHopType = mplsTunnelARHopTable + ".1.9";

    public static final String mplsTunnelCHopAddrType = mplsTunnelCHopTable + ".1.3";
    public static final String mplsTunnelCHopIpv4Addr = mplsTunnelCHopTable + ".1.4";
    public static final String mplsTunnelCHopIpv4PrefixLen = mplsTunnelCHopTable + ".1.5";
    public static final String mplsTunnelCHopIpv6Addr = mplsTunnelCHopTable + ".1.6";
    public static final String mplsTunnelCHopIpv6PrefixLen = mplsTunnelCHopTable + ".1.7";
    public static final String mplsTunnelCHopAsNumber = mplsTunnelCHopTable + ".1.8";
    public static final String mplsTunnelCHopType = mplsTunnelCHopTable + ".1.9";
}