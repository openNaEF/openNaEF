package voss.discovery.agent.alcatel.mib;

public interface TimetraVrtrMib {
    public static final String tmnxVRtrObjs = TimetraGlobalMib.tmnxSRObjs + ".3";

    public static final String vRtrIfTable = tmnxVRtrObjs + ".4";
    public static final String vRtrIfEntry = vRtrIfTable + ".1";
    public static final String vRtrIfName = vRtrIfEntry + ".4";
    public static final String vRtrIfPortID = vRtrIfEntry + ".5";
    public static final String vRtrIfEncapValue = vRtrIfEntry + ".7";
    public static final String vRtrIfOperState = vRtrIfEntry + ".9";
}