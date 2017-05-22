package voss.discovery.agent.cisco.mib;


public interface CiscoVtpMib {
    public final static String vtpVlanName = ".1.3.6.1.4.1.9.9.46.1.3.1.1.4";
    public final static String symbol_vtpVlanName = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanInfo.vtpVlanTable"
            + ".vtpVlanEntry.vtpVlanName";
    public final static String vtpVlanIfIndex = ".1.3.6.1.4.1.9.9.46.1.3.1.1.18";
    public final static String symbol_vtpVlanIfIndex = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanInfo.vtpVlanTable"
            + ".vtpVlanEntry.vtpVlanIfIndex";

    public static final String vlanTrunkPortDynamicStatus = ".1.3.6.1.4.1.9.9.46.1.6.1.1.14";
    public static final String SYMBOL_vlanTrunkPortDynamicStatus = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortDynamicStatus";

    public static final int VALUE_TRUNKING = 1;
    public static final int VALUE_NOT_TRUNKING = 2;

    public static final String vlanTrunkPortNativeVlan = ".1.3.6.1.4.1.9.9.46.1.6.1.1.5";
    public static final String SYMBOL_vlanTrunkPortNativeVlan = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortNativeVlan";

    public static final String vlanTrunkPortVlansEnabled = ".1.3.6.1.4.1.9.9.46.1.6.1.1.4";
    public static final String SYMBOL_vlanTrunkPortVlansEnabled = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortVlansEnabled";

    public static final String vlanTrunkPortVlansEnabled2k = ".1.3.6.1.4.1.9.9.46.1.6.1.1.17";
    public static final String SYMBOL_vlanTrunkPortVlansEnabled2k = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortVlansEnabled2k";

    public static final String vlanTrunkPortVlansEnabled3k = ".1.3.6.1.4.1.9.9.46.1.6.1.1.18";
    public static final String SYMBOL_vlanTrunkPortVlansEnabled3k = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortVlansEnabled3k";

    public static final String vlanTrunkPortVlansEnabled4k = ".1.3.6.1.4.1.9.9.46.1.6.1.1.19";
    public static final String SYMBOL_vlanTrunkPortVlansEnabled4k = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortVlansEnabled4k";

    public static final String vlanTrunkPortVlansXmitJoined = ".1.3.6.1.4.1.9.9.46.1.6.1.1.11";
    public static final String SYMBOL_vlanTrunkPortVlansXmitJoined = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortVlansXmitJoined";

    public static final String vlanTrunkPortVlansXmitJoined2k = ".1.3.6.1.4.1.9.9.46.1.6.1.1.23";
    public static final String SYMBOL_vlanTrunkPortVlansXmitJoined2k = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortVlansXmitJoined2k";

    public static final String vlanTrunkPortVlansXmitJoined3k = ".1.3.6.1.4.1.9.9.46.1.6.1.1.24";
    public static final String SYMBOL_vlanTrunkPortVlansXmitJoined3k = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortVlansXmitJoined3k";

    public static final String vlanTrunkPortVlansXmitJoined4k = ".1.3.6.1.4.1.9.9.46.1.6.1.1.25";
    public static final String SYMBOL_vlanTrunkPortVlansXmitJoined4k = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanTrunkPorts"
            + ".vlanTrunkPortTable.vlanTrunkPortEntry.vlanTrunkPortVlansXmitJoined4k";


    public static final String managementDomainName = ".1.3.6.1.4.1.9.9.46.1.2.1.1.2";
    public static final String SYMBOL_managementDomainName = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanManagementDomains"
            + ".managementDomainTable.managementDomainEntry.managementDomainName";

    public static final String managementDomainLocalMode = ".1.3.6.1.4.1.9.9.46.1.2.1.1.3";
    public static final String SYMBOL_managementDomainLocalMode = "enterprises.cisco.ciscoMgmt.ciscoVtpMIB.vtpMIBObjects.vlanManagementDomains"
            + ".managementDomainTable.managementDomainEntry.managementDomainLocalMode";
}