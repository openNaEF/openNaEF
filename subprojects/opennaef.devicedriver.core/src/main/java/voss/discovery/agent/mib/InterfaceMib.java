package voss.discovery.agent.mib;


public interface InterfaceMib {
    public static final String Snmpv2_MIB = ".1.3.6.1.2.1";

    public static final String ifTable = Snmpv2_MIB + ".2.2.1";

    public static final String ifDesc = Snmpv2_MIB + ".2.2.1.2";
    public static final String ifDesc_SUFFIX = "2";

    public static final String ifType = Snmpv2_MIB + ".2.2.1.3";
    public static final String ifType_SUFFIX = "3";

    public static final String ifSpeed = Snmpv2_MIB + ".2.2.1.5";
    public static final String ifSpeed_SUFFIX = "5";

    public static final String ifPhysAddress = Snmpv2_MIB + ".2.2.1.6";

    public static final String ifAdminStatus = Snmpv2_MIB + ".2.2.1.7";
    public static final String ifAdminStatus_SUFFIX = "7";

    public static final int IF_ADMIN_STATUS_UP = 1;
    public static final int IF_ADMIN_STATUS_DOWN = 2;
    public static final int IF_ADMIN_STATUS_TESTING = 3;

    public static final String ifOperStatus = Snmpv2_MIB + ".2.2.1.8";
    public static final String ifOperStatus_SUFFIX = "8";

    public static final String ifXTable = Snmpv2_MIB + ".31.1.1.1";

    public static final String ifName = Snmpv2_MIB + ".31.1.1.1.1";
    public static final String ifName_SUFFIX = "1";

    public static final String ifHighSpeed = Snmpv2_MIB + ".31.1.1.1.15";
    public static final String ifHighSpeed_SUFFIX = "15";

    public static final String ifConnectorPresent = Snmpv2_MIB + ".31.1.1.1.17";
    public static final String ifConnectorPresent_SUFFIX = "17";

    public static final String ifAlias = Snmpv2_MIB + ".31.1.1.1.18";
    public static final String ifAlias_SUFFIX = "18";

    public static final String ifPhysAddress_OID = ".1.3.6.1.2.1.2.2.1.6";
    public static final String ifPhysAddress_SYMBOL = ".iso.org.dod.internet.mgmt.mib-2.interfaces.ifTable.ifEntry" +
            ".ifPhysAddress";
    public static final String ifPhysAddress_SUFFIX = "6";

    public static final String ifStackStatus_OID = ".1.3.6.1.2.1.31.1.2.1.3";
    public static final String ifStackStatus_SYMBOL = ".iso.org.dod.internet.mgmt.mib-2" +
            ".ifMIB.ifMIBObjects.ifStackTable.ifStackEntry" +
            ".ifStackStatus";
    public static final String ifStackStatus_SUFFIX = "3";

    public static final int CONNECTOR_PRESENT = 1;
    public static final int CONNECTOR_NOT_PRESENT = 2;
}