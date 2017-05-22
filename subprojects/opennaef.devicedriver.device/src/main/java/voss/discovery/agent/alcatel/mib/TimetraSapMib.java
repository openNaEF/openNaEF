package voss.discovery.agent.alcatel.mib;

public interface TimetraSapMib {
    public static final String tmnxSapObjs = TimetraServMib.tmnxServObjs + ".3";

    public static final String sapBaseInfoTable = tmnxSapObjs + ".2";
    public static final String sapBaseInfoEntry = sapBaseInfoTable + ".1";
    public static final String sapPortId = sapBaseInfoEntry + ".1";
}