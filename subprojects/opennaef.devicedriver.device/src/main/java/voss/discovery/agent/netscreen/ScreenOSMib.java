package voss.discovery.agent.netscreen;

public class ScreenOSMib {
    public static final String netScreenBaseOid = ".1.3.6.1.4.1.3224";

    public static final String nsSetGenSwVer = netScreenBaseOid + ".7.1.5.0";

    public static final String netscreenInterface = netScreenBaseOid + ".9";
    public static final String nsIfTable = netscreenInterface + ".1";
    public static final String nsIfEntry = nsIfTable + ".1";
    public static final String nsIfIndex = nsIfEntry + ".1";
    public static final String nsIfName = nsIfEntry + ".2";
    public static final String nsIfVsys = nsIfEntry + ".3";
    public static final String nsIfZone = nsIfEntry + ".4";
    public static final String nsIfMode = nsIfEntry + ".10";
    public static final String nsIfMAC = nsIfEntry + ".11";

    public static final String nsSlotSN = netScreenBaseOid + ".21.5.1.4";

    public static final String netscreenZone = netScreenBaseOid + ".8";
    public static final String nsZoneCfg = netscreenZone + ".1";
    public static final String nsZoneCfgTable = nsZoneCfg + ".1";
    public static final String nsZoneCfgEntry = nsZoneCfgTable + ".1";
    public static final String nsZoneCfgId = nsZoneCfgEntry + ".1";
    public static final String nsZoneCfgName = nsZoneCfgEntry + ".2";
    public static final String nsZoneCfgType = nsZoneCfgEntry + ".3";
    public static final String nsZoneCfgVsys = nsZoneCfgEntry + ".4";
}