package voss.discovery.agent.alcatel.mib;

public interface MplsTeMib {
    public static final String mplsTeMIB = ".1.3.6.1.3.95";
    public static final String mplsTeObjects = mplsTeMIB + ".1";
    public static final String mplsTunnelTable = mplsTeObjects + ".2";
    public static final String mplsTunnelEntry = mplsTunnelTable + ".1";
    public static final String mplsTunnelName = mplsTunnelEntry + ".4";
    public static final String mplsTunnelInstancePriority = mplsTunnelEntry + ".16";
    public static final String mplsTunnelHopTableIndex = mplsTunnelEntry + ".17";

    public static final String mplsTunnelHopTable = mplsTeObjects + ".5";
    public static final String mplsTunnelHopEntry = mplsTunnelHopTable + ".1";
    public static final String mplsTunnelHopAddrType = mplsTunnelHopEntry + ".3";
    public static final String mplsTunnelHopIpv4Addr = mplsTunnelHopEntry + ".4";

    public static final String mplsTunnelARHopTable = mplsTeObjects + ".8";
    public static final String mplsTunnelARHopEntry = mplsTunnelARHopTable + ".1";
    public static final String mplsTunnelARHopAddrType = mplsTunnelARHopEntry + ".3";
    public static final String mplsTunnelARHopIpv4Addr = mplsTunnelARHopEntry + ".4";
    public static final String mplsTunnelARHopIpv6Addr = mplsTunnelARHopEntry + ".6";
    public static final String mplsTunnelARHopAsNumber = mplsTunnelARHopEntry + ".8";
}