package voss.discovery.agent.extreme;

public interface ExtremeMib {
    public static final String extremeMasterMSMSlot_OID = ".1.3.6.1.4.1.1916.1.1.2.1";
    public static final String extremeMasterMSMSlot_SYMBOL = "enterprises.extremenetworks" +
            ".extremeAgent.extremeSystem.extremeChassisGroup.extremeMasterMSMSlot";

    public static final String extremeSlotNumber_OID = ".1.3.6.1.4.1.1916.1.1.2.2.1.1";
    public static final String extremeSlotNumber_SYMBOL =
            "enterprises.extremenetworks.extremeAgent.extremeSystem.extremeChassisGroup"
                    + ".extremeSlotTable.extremeSlotEntry.extremeSlotNumber";

    public static final String extremeSlotModuleState_OID = ".1.3.6.1.4.1.1916.1.1.2.2.1.5";
    public static final String extremeSlotModuleState_SYMBOL =
            "enterprises.extremenetworks.extremeAgent.extremeSystem.extremeChassisGroup"
                    + ".extremeSlotTable.extremeSlotEntry.extremeSlotModuleState";
    public static final int VALUE_NOT_PRESENT = 1;
    public static final int VALUE_OPERATIONAL = 5;

    public static final String extremeSlotModuleInsertedType_OID = ".1.3.6.1.4.1.1916.1.1.2.2.1.4";
    public static final String extremeSlotModuleInsertedType_SYMBOL =
            "enterprises.extremenetworks.extremeAgent.extremeSystem.extremeChassisGroup"
                    + ".extremeSlotTable.extremeSlotEntry.extremeSlotModuleInsertedType";

    public static final String extremeStackDetection_OID = ".1.3.6.1.4.1.1916.1.33.1.0";
    public static final String extremeStackDetection_SYMBOL = ".iso.org.dod.internet.private.enterprises" +
            ".extremenetworks.extremeAgent.extremeStackable" +
            ".extremeStackDetection";
    public static final int STACK_DISABLED = 0;
    public static final int STACK_ENABLED = 1;

    public static final String extremeStackMemberType_OID = ".1.3.6.1.4.1.1916.1.33.2.1.2";
    public static final String extremeStackMemberType_SYMBOL = ".iso.org.dod.internet.private.enterprises" +
            ".extremenetworks.extremeAgent.extremeStackable" +
            ".extremeStackMemberTable.extremeStackMemberEntry" +
            ".extremeStackMemberType";

    public static final String extremeVlanIfVlanId_OID = ".1.3.6.1.4.1.1916.1.2.1.2.1.10";
    public static final String extremeVlanIfVlanId_SYMBOL = "enterprises.extremenetworks.extremeAgent"
            + ".extremeVlan.extremeVlanGroup.extremeVlanIfTable.extremeVlanIfEntry"
            + ".extremeVlanIfVlanId";

    public static final String extremeVlanIfDescr_OID = ".1.3.6.1.4.1.1916.1.2.1.2.1.2";
    public static final String extremeVlanIfDescr_SYMBOL = "enterprises.extremenetworks.extremeAgent" +
            ".extremeVlan.extremeVlanGroup.extremeVlanIfTable.extremeVlanIfEntry" +
            ".extremeVlanIfDescr";

    public static final String extremeVlanStackHigherLayer_OID = ".1.3.6.1.4.1.1916.1.2.7.1.1.1";
    public static final String extremeVlanStackHigherLayer_SYMBOL = "enterprises.extremenetworks.extremeAgent" +
            ".extremeVlan.extremeVlanStackGroup.extremeVlanStackTable.extremeVlanStackEntry" +
            ".extremeVlanStackHigherLayer";

    public static final String extremeVlanStackLowerLayer_OID = ".1.3.6.1.4.1.1916.1.2.7.1.1.2";
    public static final String extremeVlanStackLowerLayer_SYMBOL = "enterprises.extremenetworks.extremeAgent" +
            ".extremeVlan.extremeVlanStackGroup.extremeVlanStackTable.extremeVlanStackEntry" +
            ".extremeVlanStackLowerLayer";

    public static final String extremeVlanEncapsIfTag_OID = ".1.3.6.1.4.1.1916.1.2.3.1.1.3";
    public static final String extremeVlanEncapsIfTag_SYMBOL = "enterprises.extremenetworks.extremeAgent" +
            ".extremeVlan.extremeEncapsulationGroup.extremeVlanEncapsIfTable.extremeVlanEncapsIfEntry" +
            ".extremeVlanEncapsIfTag";

    public static final String extremeVlanIpNetAddress_OID = ".1.3.6.1.4.1.1916.1.2.4.1.1.1";
    public static final String extremeVlanIpNetAddress_SYMBOL = "enterprises.extremenetworks.extremeAgent" +
            ".extremeVlan.extremeVlanIpGroup.extremeVlanIpTable.extremeVlanIpEntry" +
            ".extremeVlanIpNetAddress";

    public static final String extremeVlanIpNetMask_OID = ".1.3.6.1.4.1.1916.1.2.4.1.1.2";
    public static final String extremeVlanIpNetMask_SYMBOL = "enterprises.extremenetworks.extremeAgent" +
            ".extremeVlan.extremeVlanIpGroup.extremeVlanIpTable.extremeVlanIpEntry" +
            ".extremeVlanIpNetMask";

    public static final String extremeVlanOpaqueTaggedPorts_OID = ".1.3.6.1.4.1.1916.1.2.6.1.1.1";
    public static final String extremeVlanOpaqueTaggedPorts_SYMBOL = "enterprises.extremenetworks.extremeAgent." +
            "extremeVlan.extremeVlanOpaqueGroup.extremeVlanOpaqueTable." +
            "extremeVlanOpaqueEntry.extremeVlanOpaqueTaggedPorts";

    public static final String extremeVlanOpaqueUntaggedPorts_OID = ".1.3.6.1.4.1.1916.1.2.6.1.1.2";
    public static final String extremeVlanOpaqueUntaggedPorts_SYMBOL = "enterprises.extremenetworks.extremeAgent.extremeVlan.extremeVlanOpaqueGroup" +
            ".extremeVlanOpaqueTable.extremeVlanOpaqueEntry.extremeVlanOpaqueUntaggedPorts";

    public static final String extremePrimarySoftwareRev_OID = ".1.3.6.1.4.1.1916.1.1.1.13";
    public static final String extremePrimarySoftwareRev_SYMBOL =
            "enterprises.extremenetworks.extremeAgent.extremeSystem.extremeSystemCommon"
                    + ".extremePrimarySoftwareRev";

    public static final String extremePortLoadshare2Status_OID = ".1.3.6.1.4.1.1916.1.4.3.1.4";
    public static final String extremePortLoadshare2Status_SYMBOL =
            "enterprises.extremenetworks.extremeAgent.extremePort.extremePortLoadshare2Table"
                    + ".extremePortLoadshare2Entry.extremePortLoadshare2Status";

    public static final String extremeEsrpState_OID = ".1.3.6.1.4.1.1916.1.12.2.1.5";
    public static final String extremeEsrpState_SYMBOL =
            "enterprises.extremenetworks.extremeAgent.extremeEsrp.extremeEsrpTable"
                    + ".extremeEsrpEntry.extremeEsrpState";

    public static final String extremeEsrpPriority_OID = ".1.3.6.1.4.1.1916.1.12.2.1.6";
    public static final String extremeEsrpPriority_SYMBOL =
            "enterprises.extremenetworks.extremeAgent.extremeEsrp.extremeEsrpTable"
                    + ".extremeEsrpEntry.extremeEsrpPriority";
}