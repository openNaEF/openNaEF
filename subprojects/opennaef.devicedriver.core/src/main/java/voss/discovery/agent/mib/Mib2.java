package voss.discovery.agent.mib;


public interface Mib2 {

    public static final String Snmpv2_MIB = ".1.3.6.1.2.1";
    public static final String sysName = Snmpv2_MIB + ".1.5";
    public static final String sysObjectID = Snmpv2_MIB + ".1.2";
    public static final String sysDescr = Snmpv2_MIB + ".1.1";
    public static final String sysUptime = Snmpv2_MIB + ".1.3";
    public static final String sysContact = Snmpv2_MIB + ".1.4";
    public static final String sysLocation = Snmpv2_MIB + ".1.6";

    public static final String ifPhysAddress = Snmpv2_MIB + ".2.2.1.6";

    public static final String ipAddressTable = Snmpv2_MIB + ".4.20.1";
    public static final String ipAdEntAddr = Snmpv2_MIB + ".4.20.1.1";
    public static final String ipAdEntIfIndex = Snmpv2_MIB + ".4.20.1.2";
    public static final String ipAdEntNetMask = Snmpv2_MIB + ".4.20.1.3";
    public static final String ipRouteDest = Snmpv2_MIB + ".4.21.1.1";
    public static final String ipNetToMediaPhysAddress = Snmpv2_MIB + ".4.22.1.2";

    public static final String netDefaultGateway = Snmpv2_MIB + ".16.19.12";
    public static final String SYMBOL_netDefaultGateway = "rmon.probeConfig.netDefaultGateway";

    public static final String trapDestAddress = Snmpv2_MIB + ".16.19.13.1.4";
    public static final String SYMBOL_trapDestAddress = "rmon.probeConfig.trapDestTable.trapDestEntry.trapDestAddress";

    public static final String snmpTargetAddrTAddress = ".1.3.6.1.6.3.12.1.2.1.3";
    public static final String SYMBOL_snmpTargetAddrTAddress =
            ".iso.org.dod.internet.snmpV2.snmpModules.snmpTargetMIB.snmpTargetObjects"
                    + ".snmpTargetAddrTable.snmpTargetAddrEntry.snmpTargetAddrTAddress";

    public static final String dot1dBaseBridgeAddress = Snmpv2_MIB + ".17.1.1";
    public static final String SYMBOL_dot1dBaseBridgeAddress = "dot1dBridge.dot1dBase.dot1dBaseBridgeAddress";

    public static final String dot1qFdbDynamicCount = Snmpv2_MIB + ".17.7.1.2.2.1.1";
    public static final String dot1qTpFdbPort = Snmpv2_MIB + ".17.7.1.2.2.1.2";

    public static final String dot1dBasePortIfIndex = Snmpv2_MIB + ".17.1.4.1.2";

    public static final String dot1dTpFdbPort = Snmpv2_MIB + ".17.4.3.1.2";

    public static final String Snmpv2_SMI_enterprises = ".1.3.6.1.4.1";
}