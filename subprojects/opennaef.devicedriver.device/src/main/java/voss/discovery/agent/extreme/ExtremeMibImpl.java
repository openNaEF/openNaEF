package voss.discovery.agent.extreme;


import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.DeviceInfoUtil;
import voss.discovery.agent.common.OSType;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.LldpMib;
import voss.discovery.agent.mib.Mib2;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.*;
import voss.discovery.iolib.snmp.SnmpHelper.IfIndexKey;
import voss.discovery.iolib.snmp.SnmpHelper.IntegerKey;
import voss.discovery.iolib.snmp.SnmpHelper.TwoIntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;
import voss.model.EsrpGroup.MasterSlaveRelationship;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class ExtremeMibImpl implements ExtremeMib {
    private static final Logger log = LoggerFactory.getLogger(ExtremeMibImpl.class);

    public ExtremeMibImpl(SnmpAccess snmp) {
        this.snmp = snmp;
    }

    protected final SnmpAccess snmp;
    protected boolean fixedChassis;
    protected IfNameProcessor ifNameProcessor;

    public void prepare(Device device) throws IOException, AbortedException {
        int slots = getNumberOfSlot();
        this.fixedChassis = (slots == 0
                || (slots == 1 && device.getModelTypeName().toLowerCase().startsWith("summit")));
        log.info("prepare():fixedChassis? " + this.fixedChassis);
        this.ifNameProcessor = IfNameProcessor.getProcessor(this.fixedChassis);
    }

    private int getNumberOfSlot() throws IOException, AbortedException {
        try {
            List<IntSnmpEntry> list = SnmpUtil.getIntSnmpEntries(snmp, extremeMasterMSMSlot_OID);
            if (list.size() == 0) {
                return 0;
            }
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }

        try {
            List<IntSnmpEntry> slots = SnmpUtil.getIntSnmpEntries(snmp, extremeSlotNumber_OID);
            return slots.size();
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public String getModelName() throws IOException, AbortedException {
        try {
            byte[] sysObjectID = SnmpUtil.getByte(snmp, Mib2.sysObjectID + ".0");
            int machineTypeID = sysObjectID[sysObjectID.length - 1];
            String modelName = ExtremeMibUtil.getType(machineTypeID);
            if (modelName == null) {
                modelName = "Unknown Extreme Switch(" + machineTypeID + ")";
            }
            return modelName;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public String getOsType(String osVersion) throws IOException, AbortedException {
        if (osVersion.indexOf('.') > -1) {
            int major = Integer.parseInt(osVersion.split("\\.")[0]);
            if (major >= 10) {
                return OSType.EXTREMEXOS.caption;
            }
        }
        return OSType.EXTREMEWARE.caption;
    }

    public String getOsVersion() throws IOException, AbortedException {
        try {
            String version = SnmpUtil.getString(snmp, extremePrimarySoftwareRev_OID + ".0");
            return version;
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public void updateIpAddress(Device device) throws IOException, AbortedException {
    }

    public void createSlots(Device device) throws IOException, AbortedException {
        if (isStackConfiguration()) {
            createSlotsForStackChassis(device);
        } else {
            createSlotsForNonStackChassis(device);
        }
    }

    public void createSlotsForStackChassis(Device device) throws IOException, AbortedException {
        try {
            List<SnmpUtil.ByteSnmpEntry> stackedDeviceTypes = SnmpUtil.getByteSnmpEntries(this.snmp, extremeStackMemberType_OID);
            for (final SnmpUtil.ByteSnmpEntry entry : stackedDeviceTypes) {
                int slotID = entry.oidSuffix[0].intValue();
                int modelID = 0xff & entry.getValue()[entry.getValue().length - 1];
                String modelName = ExtremeMibUtil.getType(modelID);
                Slot slot = new SlotImpl();
                slot.initContainer(device);
                slot.initSlotIndex(slotID);
                slot.initSlotId(Integer.toString(slotID));
                log.debug("@create slot " + slotID + " on [" + device.getDeviceName() + "]");

                Module stack = new ModuleImpl();
                stack.initSlot(slot);
                stack.setModelTypeName(modelName);
                log.debug("@insert slot " + slotID + " module [" + modelName + "] on [" + device.getDeviceName() + "]");
            }
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public void createSlotsForNonStackChassis(Device device) throws IOException, AbortedException {
        if (this.fixedChassis) {
            return;
        }

        Map<IntegerKey, IntSnmpEntry> slotNumbers =
                SnmpUtil.getWalkResult(snmp, extremeSlotNumber_OID, SnmpHelper.intEntryBuilder, SnmpHelper.integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> slotModuleInsertedTypes =
                SnmpUtil.getWalkResult(snmp, extremeSlotModuleInsertedType_OID, SnmpHelper.intEntryBuilder, SnmpHelper.integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> slotModuleStates =
                SnmpUtil.getWalkResult(snmp, extremeSlotModuleState_OID, SnmpHelper.intEntryBuilder, SnmpHelper.integerKeyCreator);

        for (IntegerKey key : slotNumbers.keySet()) {
            Slot slot = new SlotImpl();
            slot.initContainer(device);
            slot.initSlotIndex(slotNumbers.get(key).intValue());
            slot.initSlotId(String.valueOf(slotNumbers.get(key).intValue()));
            log.debug("@ add slot " + slot.getSlotIndex() + " on device '" + device.getDeviceName() + "'");

            int moduleState = slotModuleStates.get(key).intValue();
            if (moduleState != VALUE_NOT_PRESENT) {
                Module module = new ModuleImpl();
                module.initSlot(slot);

                int typeID = slotModuleInsertedTypes.get(key).intValue();
                module.setModelTypeName(ExtremeMibUtil.getModuleType(typeID));
                log.debug("@ insert module " + module.getModelTypeName()
                        + " to slot " + slot.getSlotIndex()
                        + " on device '" + device.getDeviceName() + "'");
            }
        }
    }

    private boolean isStackConfiguration() {
        final String extremeStackDetection_OID = ".1.3.6.1.4.1.1916.1.33.1.0";
        try {
            Integer detection = SnmpUtil.getInteger(this.snmp, extremeStackDetection_OID);
            return detection.intValue() == 1;
        } catch (NoSuchMibException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void createPhysicalPorts(VlanDevice device) throws IOException, AbortedException {
        Map<IfIndexKey, IntSnmpEntry> connectorPresents =
                SnmpUtil.getWalkResult(snmp, InterfaceMib.ifConnectorPresent, SnmpHelper.intEntryBuilder, SnmpHelper.ifIndexKeyCreator);
        Map<IfIndexKey, StringSnmpEntry> ifnames =
                SnmpUtil.getWalkResult(snmp, InterfaceMib.ifName, SnmpHelper.stringEntryBuilder, SnmpHelper.ifIndexKeyCreator);

        for (IfIndexKey key : connectorPresents.keySet()) {
            if (connectorPresents.get(key).intValue() == InterfaceMib.CONNECTOR_NOT_PRESENT) {
                continue;
            }
            int ifindex = key.getIfIndex();
            String ifName = ifnames.get(key).getValue();

            if (fixedChassis) {
                initPort(device, ifindex, ifName);
            } else {
                if (isManagement(ifName)) {
                    initPort(device, ifindex, ifName);
                } else {
                    int slotID = this.ifNameProcessor.getSlotID(ifName);
                    Slot slot = device.getSlotBySlotIndex(slotID);
                    if (slot == null) {
                        throw new IllegalStateException("port " + ifName + " found, bot no slot found: " + slotID);
                    }
                    Module module = slot.getModule();
                    if (module == null) {
                        log.warn("port " + ifName + " found, bot no module found: " + slotID);
                        continue;
                    }
                    EthernetPort port = initPort(device, ifindex, ifName);
                    module.addPort(port);
                }
            }
        }
    }

    private EthernetPort initPort(VlanDevice device, int ifindex, String ifName) {
        EthernetPort port = new EthernetPortImpl();
        port.initDevice(device);
        port.initIfIndex(ifindex);
        if (isManagement(ifName)) {
            port.initIfName(ifName);
            return port;
        }

        port.initIfName(this.ifNameProcessor.getIfName(ifName));
        port.initPortIndex(this.ifNameProcessor.getPortID(ifName));
        log.debug("@create port ifIndex=" + ifindex + " ifName=" + port.getIfName()
                + " portIndex=" + port.getPortIndex() + " on device " + device.getDeviceName());
        return port;
    }

    private boolean isManagement(String ifName) {
        if (ifName == null) {
            return false;
        } else if (ifName.toLowerCase().startsWith("mgmt")) {
            return true;
        } else if (ifName.toLowerCase().startsWith("management")) {
            return true;
        }
        return false;
    }

    public void createAggregationGroup(VlanDevice device) throws IOException, AbortedException {
        try {
            Set<Integer> knownLagID = new HashSet<Integer>();
            List<IntSnmpEntry> entries = SnmpUtil.getIntSnmpEntries(snmp, extremePortLoadshare2Status_OID);
            for (IntSnmpEntry entry : entries) {
                assert entry.oidSuffix.length == 2;
                int masterIfIndex = entry.oidSuffix[0].intValue();
                int slaveIfIndex = entry.oidSuffix[1].intValue();

                EthernetPort master = (EthernetPort) device.getPortByIfIndex(masterIfIndex);
                EthernetPort slave = (EthernetPort) device.getPortByIfIndex(slaveIfIndex);

                EthernetPortsAggregator lag =
                        DeviceInfoUtil.getOrCreateLogicalEthernetPortByAggregationId(
                                device, masterIfIndex,
                                "LAG-" + master.getIfName(),
                                master.getIfName());

                if (!knownLagID.contains(masterIfIndex)) {
                    lag.initMasterPort(master);
                    lag.addPhysicalPort(master);
                }
                lag.addPhysicalPort(slave);
                knownLagID.add(masterIfIndex);
            }

            if (entries.size() != 0) {
                return;
            }

            List<IntSnmpEntry> lldpLagEntries = SnmpUtil.getIntSnmpEntries(snmp,
                    LldpMib.lldpXdot3LocLinkAggPortId_OID);
            for (IntSnmpEntry lldpLagEntry : lldpLagEntries) {
                int lagID = lldpLagEntry.intValue();
                if (lagID == 0) {
                    continue;
                }

                String oid = LldpMib.lldpLocPortIdSubtype_OID + "." + lldpLagEntry.oidSuffix[0].intValue();
                int idType = SnmpUtil.getInteger(snmp, oid);
                if (idType != 5) {
                    throw new IOException("non-ifName entry found on: " + device.getDeviceName() + ", oid=" + oid);
                }

                EthernetPortsAggregator lag =
                        DeviceInfoUtil.getOrCreateLogicalEthernetPortByAggregationId(device, lagID);

                String ifName = SnmpUtil.getString(snmp,
                        LldpMib.lldpLocPortId_OID + "." + lldpLagEntry.oidSuffix[0].intValue());
                EthernetPort member = (EthernetPort) device.getPortByIfName(ifName);
                assert member != null;
                lag.addPhysicalPort(member);
            }
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    private final Map<Integer, Integer> vlanStackMapHigherToLower = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> vlanStackMapLowerToHigher = new HashMap<Integer, Integer>();

    public void createVlanIf(VlanDevice device) throws IOException, AbortedException {
        Map<IntegerKey, StringSnmpEntry> vlanIfDescriptions =
                SnmpUtil.getWalkResult(snmp, extremeVlanIfDescr_OID,
                        SnmpHelper.stringEntryBuilder, SnmpHelper.integerKeyCreator);

        Map<IntegerKey, IntSnmpEntry> vlanIfVlanIds =
                SnmpUtil.getWalkResult(snmp, extremeVlanIfVlanId_OID, SnmpHelper.intEntryBuilder, SnmpHelper.integerKeyCreator);

        Map<IntegerKey, IntSnmpEntry> vlanEncapsIfTags =
                SnmpUtil.getWalkResult(snmp, extremeVlanEncapsIfTag_OID,
                        SnmpHelper.intEntryBuilder, SnmpHelper.integerKeyCreator);

        Map<TwoIntegerKey, IntSnmpEntry> extremeVlanStackHigherLayers =
                SnmpUtil.getWalkResult(snmp, extremeVlanStackLowerLayer_OID,
                        SnmpHelper.intEntryBuilder, SnmpHelper.twoIntegerKeyCreator);

        Map<IntegerKey, SnmpEntry> extremeVlanIpNetAddresses =
                SnmpUtil.getWalkResult(snmp, extremeVlanIpNetAddress_OID, SnmpHelper.integerKeyCreator);
        Map<IntegerKey, SnmpEntry> extremeVlanIpNetMasks =
                SnmpUtil.getWalkResult(snmp, extremeVlanIpNetMask_OID, SnmpHelper.integerKeyCreator);

        for (TwoIntegerKey key : extremeVlanStackHigherLayers.keySet()) {
            vlanStackMapHigherToLower.put(key.intValue1(), key.intValue2());
            vlanStackMapLowerToHigher.put(key.intValue2(), key.intValue1());
            log.debug("vlanStack: higher=" + key.intValue1() + " lower=" + key.intValue2());
        }

        for (IntegerKey key : vlanIfDescriptions.keySet()) {
            int vlanIfIndex = key.getInt();
            VlanIf vlan = new VlanIfImpl();
            vlan.initDevice(device);
            vlan.initIfIndex(vlanIfIndex);
            vlan.initIfName(vlanIfDescriptions.get(key).getValue());
            vlan.setVlanName(vlan.getIfName());
            log.info("@ create vlan ifindex=" + vlanIfIndex + " name='" + vlan.getVlanName() + "'");

            Integer lowerIfIndex = vlanStackMapHigherToLower.get(vlanIfIndex);
            IntegerKey lowerKey = null;
            if (lowerIfIndex != null) {
                BigInteger[] bi = {BigInteger.valueOf(lowerIfIndex)};
                lowerKey = new IntegerKey(bi);
            }

            int vlanID = -1;
            if (vlanIfVlanIds.size() > 0) {
                IntSnmpEntry e = vlanIfVlanIds.get(key);
                if (e != null) {
                    vlanID = e.intValue();
                }
            } else if (lowerKey != null) {
                IntSnmpEntry vlanEncapsIfTagEntry = vlanEncapsIfTags.get(lowerKey);
                if (vlanEncapsIfTagEntry != null) {
                    vlanID = vlanEncapsIfTagEntry.intValue();
                }
            }
            vlan.initVlanId(vlanID);
            log.info("@ set vlan ifindex=" + vlanIfIndex + " id=" + vlan.getVlanId());

            if (lowerKey != null) {
                SnmpEntry ip = extremeVlanIpNetAddresses.get(lowerKey);
                if (ip != null) {
                    SnmpEntry mask = extremeVlanIpNetMasks.get(lowerKey);
                    String ipAddress = IpAddressSnmpEntry.getInstance(ip).getIpAddress();
                    String netMask = IpAddressSnmpEntry.getInstance(mask).getIpAddress();

                    String[] ips = {ipAddress + "/" + netMask};
                    vlan.setIpAddresses(ips);
                    log.info("@ set vlan ifindex=" + vlanIfIndex + " ipaddress='" + ips[0] + "'");
                }
            }
        }
        setupTaggedVlan(device);
        setupUntaggedVlan(device);
    }

    protected void setupTaggedVlan(VlanDevice device) throws IOException, AbortedException {
        Map<TwoIntegerKey, SnmpEntry> extremeVlanOpaqueTaggedPorts =
                SnmpUtil.getWalkResult(snmp, extremeVlanOpaqueTaggedPorts_OID, SnmpHelper.twoIntegerKeyCreator);

        for (TwoIntegerKey key : extremeVlanOpaqueTaggedPorts.keySet()) {
            int vlanIfIndex = key.intValue1();
            VlanIf vlan = (VlanIf) device.getPortByIfIndex(vlanIfIndex);

            int slotID = key.intValue2();
            byte[] portList = extremeVlanOpaqueTaggedPorts.get(key).value;
            int[] ports = SnmpUtil.decodeBitList(portList);
            for (int i = 0; i < ports.length; i++) {
                if (isDedicatedManagementPort(this.fixedChassis, slotID, ports[i])) {
                    continue;
                }
                String ifName = ifNameProcessor.getIfName(slotID, ports[i]);
                EthernetPort port = (EthernetPort) device.getPortByIfName(ifName);
                LogicalEthernetPort logical = device.getLogicalEthernetPort(port);
                if (port != null) {
                    vlan.addTaggedPort(logical);
                    log.info("@ vlan " + vlan.getVlanId()
                            + " ifindex=" + vlan.getIfIndex()
                            + " add tagged port " + logical.getIfName()
                            + " on device '" + device.getDeviceName() + "'");
                } else {
                    log.debug("extremeVlanOpaqueTaggedPortsEntry(): port not found: ifName=" + ifName);
                }
            }
        }
    }

    protected void setupUntaggedVlan(VlanDevice device) throws IOException, AbortedException {
        Map<TwoIntegerKey, SnmpEntry> extremeVlanOpaqueUntaggedPorts =
                SnmpUtil.getWalkResult(snmp, extremeVlanOpaqueUntaggedPorts_OID, SnmpHelper.twoIntegerKeyCreator);

        for (TwoIntegerKey key : extremeVlanOpaqueUntaggedPorts.keySet()) {
            int vlanIfIndex = key.intValue1();
            VlanIf vlan = (VlanIf) device.getPortByIfIndex(vlanIfIndex);

            int slotID = key.intValue2();
            byte[] portList = extremeVlanOpaqueUntaggedPorts.get(key).value;
            int[] ports = SnmpUtil.decodeBitList(portList);
            for (int i = 0; i < ports.length; i++) {
                if (isDedicatedManagementPort(this.fixedChassis, slotID, ports[i])) {
                    continue;
                }
                String ifName = ifNameProcessor.getIfName(slotID, ports[i]);
                EthernetPort port = (EthernetPort) device.getPortByIfName(ifName);
                LogicalEthernetPort logical = device.getLogicalEthernetPort(port);
                if (port != null) {
                    vlan.addUntaggedPort(logical);
                    log.info("@ vlan " + vlan.getVlanId()
                            + " ifindex=" + vlan.getIfIndex()
                            + " add untagged port " + logical.getIfName()
                            + " on device '" + device.getDeviceName() + "'");
                } else {
                    log.debug("extremeVlanOpaqueUntaggedPortsEntry(): port not found: ifName=" + ifName);
                }
            }
        }
    }

    public static boolean isDedicatedManagementPort(boolean isFixedChassis, int slotIndex, int portIndex) {
        if (isFixedChassis && slotIndex > 1) {
            return true;
        }
        return false;
    }

    private MasterSlaveRelationship.StateType getStateType(int value) {
        switch (value) {
            case 1:
                return MasterSlaveRelationship.StateType.NEUTRAL;
            case 2:
                return MasterSlaveRelationship.StateType.MASTER;
            case 3:
                return MasterSlaveRelationship.StateType.SLAVE;
            default:
                throw new IllegalStateException("unknown value: " + value);
        }
    }

    private void addMasterSlaveRelation
            (EsrpGroup esrpGroup, MasterSlaveRelationship masterSlaveRelation) {
        if (esrpGroup.getMasterSlaveRelationships() == null) {
            esrpGroup.setMasterSlaveRelationships(
                    new MasterSlaveRelationship[]{
                            masterSlaveRelation});
        } else {
            List<MasterSlaveRelationship> relations = Arrays.asList(esrpGroup.getMasterSlaveRelationships());
            if (!relations.contains(masterSlaveRelation)) {
                relations.add(masterSlaveRelation);
            }
            esrpGroup.setMasterSlaveRelationships(relations.toArray(new MasterSlaveRelationship[0]));
        }
    }

    public void vlanStp(VlanDevice device) throws Exception {
        final Map<VlanIf, MasterSlaveRelationship> masterSlaveRelations =
                new HashMap<VlanIf, MasterSlaveRelationship>();
        final Map<Integer, EsrpGroup> esrpGroups = new HashMap<Integer, EsrpGroup>();

        Map<TwoIntegerKey, IntSnmpEntry> extremeEsrpStates =
                SnmpUtil.getWalkResult(snmp, extremeEsrpState_OID, SnmpHelper.intEntryBuilder, SnmpHelper.twoIntegerKeyCreator);
        Map<TwoIntegerKey, IntSnmpEntry> extremeEsrpPriorities =
                SnmpUtil.getWalkResult(snmp, extremeEsrpPriority_OID, SnmpHelper.intEntryBuilder, SnmpHelper.twoIntegerKeyCreator);

        for (TwoIntegerKey key : extremeEsrpStates.keySet()) {
            int vlanIfIndex = key.intValue1();
            int esrpGroupID = key.intValue2();

            EsrpGroup esrp = esrpGroups.get(esrpGroupID);
            if (esrp == null) {
                esrp = new EsrpGroupImpl();
                esrp.setGroupIndex(esrpGroupID);
                esrpGroups.put(esrpGroupID, esrp);
                device.addVlanStpElement(esrp);
            }
            MasterSlaveRelationship masterSlaveRelation =
                    new EsrpGroupImpl.MasterSlaveRelationshipImpl();
            VlanIf vlanIf = (VlanIf) device.getPortByIfIndex(vlanIfIndex);

            List<VlanIf> vlanIfs = Arrays.asList(esrp.getVlanIfs());
            if (!vlanIfs.contains(vlanIf)) {
                vlanIfs.add(vlanIf);
                esrp.setVlanIfs(vlanIfs.toArray(new VlanIf[0]));
            }

            masterSlaveRelation.setVlanIf(vlanIf);
            masterSlaveRelation.setEsrpGroup(esrp);
            masterSlaveRelation.setStateType(getStateType(extremeEsrpStates.get(key).intValue()));
            addMasterSlaveRelation(esrp, masterSlaveRelation);
            masterSlaveRelations.put(vlanIf, masterSlaveRelation);

            int priority = extremeEsrpPriorities.get(key).intValue();
            masterSlaveRelation.setPriority(priority);
        }
    }

}