package voss.discovery.agent.apresia.apware;

public class ApresiaVdrMib {
    public static final String hclVdrTable = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1";
    public static final String hclVdrTable_lockStatus = "4";
    public static final String hclVdrTable_adminStatus = "7";
    public static final String hclVdrTable_name = "8";

    public static final String hclVdrIndex = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1.1";

    public static final String hclVdrLockStatus = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1.4";
    public static final String hclVdrAdminStatus = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1.7";
    public static final String hclVdrName = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.1.1.8";
    public static final String hclVdrUplinkTable = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.2.1";
    public static final String hclVdrUplinkTable_interface = "4";
    public static final String hclVdrUplinkTable_linkStatus = "5";
    public static final String hclVdrUplinkTable_failureStatus = "6";

    public static final String hclVdrUplinkInterface = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.2.1.4";
    public static final String hclVdrUplinkIfLinkStatus = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.2.1.5";
    public static final String hclVdrUplinkIfFailureStatus = ApresiaApwareMib.Snmpv2_SMI_hitachiCable_mibs + ".30.9.2.1.6";
}