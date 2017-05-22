package voss.discovery.agent.cisco.mib;

public interface CiscoStackMib {

    public static final String sysTrapReceiverAddr = ".1.3.6.1.4.1.9.5.1.1.5.1.2";
    public static final String SYMBOL_sysTrapReceiverAddr = "enterprises.cisco.workgroup.ciscoStackMIB.systemGrp.sysTrapReceiverTable"
            + ".sysTrapReceiverEntry.sysTrapReceiverAddr";

    public static final String syslogServerAddr = ".1.3.6.1.4.1.9.5.1.14.1.1.1";
    public static final String SYMBOL_syslogServerAddr = "enterprises.cisco.workgroup.ciscoStackMIB.syslogGrp.syslogServerTable"
            + ".syslogServerEntry.syslogServerAddr";

    public static final String sysIpAddr = ".1.3.6.1.4.1.9.5.1.1.2";
    public static final String SYMBOL_sysIpAddr = "enterprises.cisco.workgroup.ciscoStackMIB.systemGrp.sysIpAddr";

    public static final String sysNetMask = ".1.3.6.1.4.1.9.5.1.1.3";
    public static final String SYMBOL_sysNetMask = "enterprises.cisco.workgroup.ciscoStackMIB.systemGrp.sysNetMask";

    public static final String mdgGatewayAddr = ".1.3.6.1.4.1.9.5.1.21.1.1.1";
    public static final String SYMBOL_mdgGatewayAddr = "enterprises.cisco.workgroup.ciscoStackMIB.mdgGrp.mdgGatewayTable.mdgGatewayEntry"
            + ".mdgGatewayAddr";

    public static final String mdgGatewayType = ".1.3.6.1.4.1.9.5.1.21.1.1.2";
    public static final String SYMBOL_mdgGatewayType = "enterprises.cisco.workgroup.ciscoStackMIB.mdgGrp.mdgGatewayTable.mdgGatewayEntry"
            + ".mdgGatewayType";

    public static final String chassisModel = ".1.3.6.1.4.1.9.5.1.2.16.0";
    public static final String SYMBOL_chassisModel = "enterprises.cisco.workgroup.ciscoStackMIB.chassisGrp.chassisModel.0";

    public static final String chassisSerialNumberString = ".1.3.6.1.4.1.9.5.1.2.19.0";
    public static final String SYMBOL_chassisSerialNumberString = "enterprises.cisco.workgroup.ciscoStackMIB.chassisGrp.chassisSerialNumberString.0";

    public static final String chassisNumSlots = ".1.3.6.1.4.1.9.5.1.2.14.0";
    public static final String SYMBOL_chassisNumSlots = "enterprises.cisco.workgroup.ciscoStackMIB.chassisGrp.chassisNumSlots.0";


    public static final String moduleIndex = ".1.3.6.1.4.1.9.5.1.3.1.1.1";
    public static final String SYMBOL_moduleIndex = "enterprises.cisco.workgroup.ciscoStackMIB.moduleGrp.moduleTable.moduleEntry"
            + ".moduleIndex";
    public static final String moduleNumPorts = ".1.3.6.1.4.1.9.5.1.3.1.1.14";
    public static final String SYMBOL_moduleNumPorts = "enterprises.cisco.workgroup.ciscoStackMIB.moduleGrp.moduleTable.moduleEntry"
            + ".moduleNumPorts";
    public static final String moduleModel = ".1.3.6.1.4.1.9.5.1.3.1.1.17";
    public static final String SYMBOL_moduleModel = "enterprises.cisco.workgroup.ciscoStackMIB.moduleGrp.moduleTable.moduleEntry"
            + ".moduleModel";

    public static final String portCrossIndex = ".1.3.6.1.4.1.9.5.1.4.1.1.3";
    public static final String SYMBOL_portCrossIndex = "enterprises.cisco.workgroup.ciscoStackMIB.portGrp.portTable.portEntry"
            + ".portCrossIndex";

    public static final String portName = ".1.3.6.1.4.1.9.5.1.4.1.1.4";
    public static final String SYMBOL_portName = "enterprises.cisco.workgroup.ciscoStackMIB.portGrp.portTable.portEntry.portName";

    public static final String portType = ".1.3.6.1.4.1.9.5.1.4.1.1.5";
    public static final String SYMBOL_portType = "enterprises.cisco.workgroup.ciscoStackMIB.portGrp.portTable.portEntry.portType";

    public static final String portAdminSpeed = ".1.3.6.1.4.1.9.5.1.4.1.1.9";
    public static final String SYMBOL_portAdminSpeed = "enterprises.cisco.workgroup.ciscoStackMIB.portGrp.portTable.portEntry"
            + ".portAdminSpeed";
    public static final String portDuplex = ".1.3.6.1.4.1.9.5.1.4.1.1.10";
    public static final String SYMBOL_portDuplex = "enterprises.cisco.workgroup.ciscoStackMIB.portGrp.portTable.portEntry.portDuplex";

    public static final String portIfIndex = ".1.3.6.1.4.1.9.5.1.4.1.1.11";
    public static final String SYMBOL_portIfIndex = "enterprises.cisco.workgroup.ciscoStackMIB.portGrp.portTable.portEntry.portIfIndex";

    public static final String portAdditionalOperStatus = ".1.3.6.1.4.1.9.5.1.4.1.1.23";
    public static final String SYMBOL_portAdditionalOperStatus = "enterprises.cisco.workgroup.ciscoStackMIB.portGrp.portTable.portEntry"
            + ".portAdditionalOperStatus";

    public static final String vlanIndex = ".1.3.6.1.4.1.9.5.1.9.2.1.1";
    public static final String SYMBOL_vlanIndex = "enterprises.cisco.workgroup.ciscoStackMIB.vlanGrp.vlanTable.vlanEntry.vlanIndex";

    public static final String vlanIfIndex = ".1.3.6.1.4.1.9.5.1.9.2.1.3";
    public static final String SYMBOL_vlanIfIndex = "enterprises.cisco.workgroup.ciscoStackMIB.vlanGrp.vlanTable.vlanEntry.vlanIfIndex";
    public static final String vlanPortVlan = ".1.3.6.1.4.1.9.5.1.9.3.1.3";
    public static final String SYMBOL_vlanPortVlan = "enterprises.cisco.workgroup.ciscoStackMIB.vlanGrp.vlanPortTable.vlanPortEntry"
            + ".vlanPortVlan";

    public static final String vlanPortIslAdminStatus = ".1.3.6.1.4.1.9.5.1.9.3.1.7";

}