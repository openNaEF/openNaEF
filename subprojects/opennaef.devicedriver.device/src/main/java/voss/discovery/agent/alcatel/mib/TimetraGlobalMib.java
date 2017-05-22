package voss.discovery.agent.alcatel.mib;

public interface TimetraGlobalMib {
    public static final String timetra = ".1.3.6.1.4.1.6527";
    public static final String timetraProducts = timetra + ".3";
    public static final String tmnxSRMIB = timetraProducts + ".1";
    public static final String tmnxSRObjs = tmnxSRMIB + ".2";
}