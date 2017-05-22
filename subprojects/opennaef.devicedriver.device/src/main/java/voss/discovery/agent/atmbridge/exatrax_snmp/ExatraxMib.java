package voss.discovery.agent.atmbridge.exatrax_snmp;

public class ExatraxMib {

    public static final String nsSysSerialNumber = ".1.3.6.1.4.1.263.2.1.11.1.0";

    public static final String nsIfXTable = ".1.3.6.1.4.1.263.2.1.12.1.1";

    public static final String nsIfName_suffix = "1";
    public static final String nsIfName = nsIfXTable + "." + nsIfName_suffix;

    public static final String nsIfConnectorPresent_suffix = "17";
    public static final String nsIfConnectorPresent = nsIfXTable + "." + nsIfConnectorPresent_suffix;

    public static final String nsEtherPhyEntry = ".1.3.6.1.4.1.263.2.1.13.1.1";

    public static final String nsEtherStatsDuplexMode_suffix = "8";
    public static final String nsEhterStatsDuplexMode = nsEtherPhyEntry + "." + nsEtherStatsDuplexMode_suffix;

    public static final String nsEtherConfigAutoNego_suffix = "9";
    public static final String nsEtherConfigAutoNego = nsEtherPhyEntry + "." + nsEtherConfigAutoNego_suffix;

    public static final String nsAtmInterfaceTable = ".1.3.6.1.4.1.263.2.1.14.1.1";

    public static final String nsAtmIfInMissInsert_suffix = "1";
    public static final String nsAtmIfInMissInsert = nsAtmInterfaceTable + "." + nsAtmIfInMissInsert_suffix;

    public static final String nsAtmPvcTable = ".1.3.6.1.4.1.263.2.1.14.2.1";

    public static final String nsAtmPvcPcr_suffix = "3";
    public static final String nsAtmPvcPcr = nsAtmPvcTable + "." + nsAtmPvcPcr_suffix;

    public static final String nsAtmPvcAdminStatus_suffix = "6";
    public static final String nsAtmPvcAdminStatus = nsAtmPvcTable + "." + nsAtmPvcAdminStatus_suffix;

    public static final String nsAtmAal5Table = ".1.3.6.1.4.1.263.2.1.14.3.1";

    public static final String nsAtmAal5AtmIfIndex_suffix = "1";
    public static final String nsAtmAal5AtmIfIndex = nsAtmAal5Table + "." + nsAtmAal5AtmIfIndex_suffix;

    public static final String nsAtmAal5Vpi_suffix = "2";
    public static final String nsAtmAal5Vpi = nsAtmAal5Table + "." + nsAtmAal5Vpi_suffix;

    public static final String nsAtmAal5Vci_suffix = "3";
    public static final String nsAtmAal5Vci = nsAtmAal5Table + "." + nsAtmAal5Vci_suffix;

    public static final String nsVlanBridgePortMapTable = ".1.3.6.1.4.1.263.2.1.15.1.1";

    public static final String nsVlanBridgePortIfIndex_suffix = "2";
    public static final String nsVlanBridgePortIfIndex = nsVlanBridgePortMapTable + "." + nsVlanBridgePortIfIndex_suffix;

    public static final String nsVlanConfigTable = ".1.3.6.1.4.1.263.2.1.15.6.1";

    public static final String nsVlanConfigName_suffix = "3";
    public static final String nsVlanConfigName = nsVlanConfigTable + "." + nsVlanConfigName_suffix;

    public static final String nsVlanConfigEgressPorts_suffix = "4";
    public static final String nsVlanConfigEgressPorts = nsVlanConfigTable + "." + nsVlanConfigEgressPorts_suffix;

    public static final String nsVlanConfigEgressUntaggedPorts_suffix = "5";
    public static final String nsVlanConfigEgressUntaggedPorts = nsVlanConfigTable + "." + nsVlanConfigEgressUntaggedPorts_suffix;

    public static final String nsDot3adAggEntry = ".1.3.6.1.4.1.263.2.1.16.1.1";

    public static final String nsDot3adAggGroupName_suffix = "2";
    public static final String nsDot3adAggGroupName = nsDot3adAggEntry + "." + nsDot3adAggGroupName_suffix;

    public static final String nsDot3AdAggPortListPorts_suffix = "4";

    public static final String nsDot3AdAggPortListPorts = nsDot3adAggEntry + "." + nsDot3AdAggPortListPorts_suffix;
}