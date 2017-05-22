package voss.discovery.agent.alcatel.mib;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class TimetraChassisMibImpl implements TimetraChassisMib {

    private static final Logger log = LoggerFactory.getLogger(TimetraChassisMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public TimetraChassisMibImpl(Alcatel7710SRDiscovery discovery) {

        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDeviceInner();
    }

    public String getChassisTypeName() throws IOException, AbortedException {

        try {
            int chassisType = SnmpUtil.getInteger(snmp, tmnxChassisType + ".1");
            String chassisTypeName = SnmpUtil.getString(snmp, tmnxChassisTypeName + "." + chassisType);
            return chassisTypeName;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public String getSerialNumber() throws IOException, AbortedException {

        Map<IntegerKey, IntSnmpEntry> tmnxHwClassMap =
                SnmpUtil.getWalkResult(snmp, tmnxHwClass + ".1", intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : tmnxHwClassMap.keySet()) {
            if (tmnxHwClassMap.get(key).intValue() == 3) {
                int hwIndex = key.getInt();

                try {
                    return SnmpUtil.getString(snmp, tmnxHwSerialNumber + ".1." + hwIndex);
                } catch (SnmpResponseException e) {
                    throw new IOException(e);
                } catch (NoSuchMibException e) {
                    throw new IOException(e);
                }
            }
        }

        return null;
    }

    public void createSlotAndModule() throws IOException, AbortedException {

        Map<IntegerKey, IntSnmpEntry> tmnxCardEquippedTypeMap =
                SnmpUtil.getWalkResult(snmp, tmnxCardEquippedType + ".1", intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : tmnxCardEquippedTypeMap.keySet()) {

            int slotIndex = key.getInt();
            int equippedType = tmnxCardEquippedTypeMap.get(key).intValue();

            Slot slot = new SlotImpl();
            slot.initContainer(device);
            slot.initSlotIndex(slotIndex);
            slot.initSlotId(Integer.toString(slotIndex));
            device.addSlot(slot);
            log.debug("@ add slot " + slotIndex
                    + " to device '" + device.getDeviceName() + "'");

            if (equippedType > 1) {
                try {
                    Module module = new ModuleImpl();
                    int hwIndex = SnmpUtil.getInteger(snmp, tmnxCardHwIndex + ".1." + slotIndex);

                    String boradNumber = SnmpUtil.getString(snmp, tmnxHwMfgBoardNumber + ".1." + hwIndex);
                    String serialNumber = SnmpUtil.getString(snmp, tmnxHwSerialNumber + ".1." + hwIndex);
                    String typeName = SnmpUtil.getString(snmp, tmnxCardTypeName + "." + equippedType);
                    String typeDesc = SnmpUtil.getString(snmp, tmnxCardTypeDescription + "." + equippedType);

                    module.setModelTypeName(typeName);
                    module.setSerialNumber(serialNumber);
                    module.setSystemDescription(typeDesc);
                    module.setHardwareRevision(boradNumber);

                    module.initSlot(slot);
                    log.debug("@ add module '" + module.getModelTypeName()
                            + "' to slot " + slotIndex
                            + " on device '" + device.getDeviceName() + "'");
                } catch (SnmpResponseException e) {
                    throw new IOException(e);
                } catch (NoSuchMibException e) {
                    throw new IOException(e);
                }
            }
        }

        Map<OidKey, IntSnmpEntry> tmnxCpmCardEquippedTypeMap =
                SnmpUtil.getWalkResult(snmp, tmnxCpmCardEquippedType + ".1", intEntryBuilder, oidKeyCreator);

        for (OidKey key : tmnxCpmCardEquippedTypeMap.keySet()) {

            int slotIndex = key.getInt(0);
            int equippedType = tmnxCpmCardEquippedTypeMap.get(key).intValue();

            Slot slot = new SlotImpl();
            slot.initContainer(device);
            slot.initSlotIndex(slotIndex);
            slot.initSlotId(Integer.toString(slotIndex));
            device.addSlot(slot);
            log.debug("@ add slot " + slotIndex
                    + " to device '" + device.getDeviceName() + "'");

            if (equippedType > 1) {
                try {
                    Module module = new ModuleImpl();
                    int hwIndex = SnmpUtil.getInteger(snmp, tmnxCpmCardHwIndex + ".1." + slotIndex + ".1");

                    String boradNumber = SnmpUtil.getString(snmp, tmnxHwMfgBoardNumber + ".1." + hwIndex);
                    String serialNumber = SnmpUtil.getString(snmp, tmnxHwSerialNumber + ".1." + hwIndex);
                    String typeName = SnmpUtil.getString(snmp, tmnxCardTypeName + "." + equippedType);
                    String typeDesc = SnmpUtil.getString(snmp, tmnxCardTypeDescription + "." + equippedType);

                    module.setModelTypeName(typeName);
                    module.setSerialNumber(serialNumber);
                    module.setSystemDescription(typeDesc);
                    module.setHardwareRevision(boradNumber);

                    module.initSlot(slot);
                    log.debug("@ add module '" + module.getModelTypeName()
                            + "' to slot " + slotIndex
                            + " on device '" + device.getDeviceName() + "'");
                } catch (SnmpResponseException e) {
                    throw new IOException(e);
                } catch (NoSuchMibException e) {
                    throw new IOException(e);
                }
            }
        }

        Map<OidKey, IntSnmpEntry> tmnxMDAEquippedTypeMap =
                SnmpUtil.getWalkResult(snmp, tmnxMDAEquippedType + ".1", intEntryBuilder, oidKeyCreator);

        for (OidKey key : tmnxMDAEquippedTypeMap.keySet()) {
            int cardSlotIndex = key.getInt(0);
            int mdaSlotIndex = key.getInt(1);
            int equippedType = tmnxMDAEquippedTypeMap.get(key).intValue();

            Module parent = device.getSlotBySlotIndex(cardSlotIndex).getModule();
            Slot slot = new SlotImpl();
            slot.initContainer(parent);
            slot.initSlotIndex(mdaSlotIndex);
            slot.initSlotId(Integer.toString(mdaSlotIndex));
            parent.addSlot(slot);
            log.debug("@ add slot " + cardSlotIndex + "/" + mdaSlotIndex
                    + " to device '" + device.getDeviceName() + "'");

            if (equippedType > 1) {
                try {
                    Module module = new ModuleImpl();
                    int hwIndex = SnmpUtil.getInteger(snmp, tmnxMDAHwIndex + ".1." + cardSlotIndex + "." + mdaSlotIndex);

                    String boradNumber = SnmpUtil.getString(snmp, tmnxHwMfgBoardNumber + ".1." + hwIndex);
                    String serialNumber = SnmpUtil.getString(snmp, tmnxHwSerialNumber + ".1." + hwIndex);
                    String typeName = SnmpUtil.getString(snmp, tmnxMdaTypeName + "." + equippedType);
                    String typeDesc = SnmpUtil.getString(snmp, tmnxMdaTypeDescription + "." + equippedType);

                    module.setModelTypeName(typeName);
                    module.setSerialNumber(serialNumber);
                    module.setSystemDescription(typeDesc);
                    module.setHardwareRevision(boradNumber);

                    module.initSlot(slot);
                    log.debug("@ add module '" + module.getModelTypeName()
                            + "' to slot " + cardSlotIndex + "/" + mdaSlotIndex
                            + " on device '" + device.getDeviceName() + "'");
                } catch (SnmpResponseException e) {
                    throw new IOException(e);
                } catch (NoSuchMibException e) {
                    throw new IOException(e);
                }
            }
        }
    }
}