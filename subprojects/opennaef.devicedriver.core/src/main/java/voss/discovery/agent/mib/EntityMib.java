package voss.discovery.agent.mib;

import net.snmp.VarBind;
import voss.discovery.iolib.snmp.SnmpEntry;

public class EntityMib {

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalDescrEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.2";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalDescr";

        public EntityMibEntPhysicalDescrEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getPotyType() {
            return new String(value).intern();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalVendorTypeEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.3";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalVendorType";

        public EntityMibEntPhysicalVendorTypeEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getVendorType() {
            return new String(value).intern();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalContainedInEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.4";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalContainedIn";

        public EntityMibEntPhysicalContainedInEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public int getContainerPhysicalIndex() {
            return getValueAsBigInteger().intValue();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalClassEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.5";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalClass";

        public EntityMibEntPhysicalClassEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public int getValue() {
            return this.getValueAsBigInteger().intValue();
        }

        public PhysicalClass getPhysicalClass() {
            return PhysicalClass.getByIndex(getValue());
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalParentRelPosEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.6";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalParentRelPos";

        public EntityMibEntPhysicalParentRelPosEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public int getPortIndex() {
            return (int) getValueAsBigInteger().intValue();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalNameEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.7";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalName";

        public EntityMibEntPhysicalNameEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getPhysicalName() {
            return new String(value).intern();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalHardwareRevEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.8";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalHardwareRev";

        public EntityMibEntPhysicalHardwareRevEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getHardwareRev() {
            return new String(value).intern();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalFirmwareRevEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.9";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalFirmwareRev";

        public EntityMibEntPhysicalFirmwareRevEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getFirmwareRev() {
            return new String(value).intern();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalSoftwareRevEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.10";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalSoftwareRev";

        public EntityMibEntPhysicalSoftwareRevEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getFirmwareRev() {
            return new String(value).intern();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalSerialNumEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.11";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalSerialNum";

        public EntityMibEntPhysicalSerialNumEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getSerialNumber() {
            return new String(value);
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalMfgNameEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.12";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalMfgName";

        public EntityMibEntPhysicalMfgNameEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getPhysicalMfgName() {
            return new String(value);
        }
    }

    @SuppressWarnings("serial")
    public static class EntityMibEntPhysicalModelNameEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.1.1.1.13";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityPhysical.entPhysicalTable"
                        + ".entPhysicalEntry.entPhysicalModelName";

        public EntityMibEntPhysicalModelNameEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getIndex() {
            assert oidSuffix.length == 1;
            return oidSuffix[0].intValue();
        }

        public String getModelName() {
            return new String(value);
        }
    }

    @SuppressWarnings("serial")
    public static class EntAliasMappingIdentifierEntry extends SnmpEntry {
        public static final String OID = ".1.3.6.1.2.1.47.1.3.2.1.2";
        public static final String SYMBOL =
                "entityMIB.entityMIBObjects.entityMapping.entAliasMappingTable"
                        + ".entAliasMappingEntry.entAliasMappingIdentifier";

        public EntAliasMappingIdentifierEntry(VarBind varbind) {
            super(OID, varbind);
        }

        public int getEntityPhysicalIfIndex() {
            assert oidSuffix.length == 2;
            return oidSuffix[0].intValue();
        }

        public int getIfIndex() {
            return (int) value[value.length - 1];
        }
    }

}