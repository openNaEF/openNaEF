package voss.discovery.agent.alcatel.mib;

public interface TimetraSystemMib {
    public static final String tmnxSysObjs = TimetraGlobalMib.tmnxSRObjs + ".1";

    public static final String sysGenInfo = tmnxSysObjs + ".1";
    public static final String sgiSwMajorVersion = sysGenInfo + ".5";
    public static final String sgiSwMinorVersion = sysGenInfo + ".6";
    public static final String sgiSwVersionModifier = sysGenInfo + ".7";
}