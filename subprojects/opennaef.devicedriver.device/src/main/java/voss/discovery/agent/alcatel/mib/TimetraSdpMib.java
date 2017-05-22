package voss.discovery.agent.alcatel.mib;

public interface TimetraSdpMib {
    public static final String tmnxSdpObjs = TimetraServMib.tmnxServObjs + ".4";

    public static final String sdpInfoTable = tmnxSdpObjs + ".3";
    public static final String sdpInfoEntry = sdpInfoTable + ".1";
    public static final String sdpFarEndIpAddress = sdpInfoEntry + ".4";
    public static final String sdpLspList = sdpInfoEntry + ".5";
    public static final String sdpBindTable = tmnxSdpObjs + ".4";
    public static final String sdpBindEntry = sdpBindTable + ".1";
    public static final String sdpBindId = sdpBindEntry + ".1";
    public static final String sdpBindPwPeerStatusBits = sdpBindEntry + ".28";
    public static final String sdpBindOperControlWord = sdpBindEntry + ".32";
    public static final String sdpBindOperBandwidth = sdpBindEntry + ".41";
}