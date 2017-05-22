package voss.discovery.agent.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.*;

public class PhysicalConfigurationBuilder {
    private static final Logger log = LoggerFactory.getLogger(PhysicalConfigurationBuilder.class);

    public static Module createModule(PhysicalEntry entry, Slot slot) {
        Device device = slot.getDevice();
        Module module = new ModuleImpl();
        module.setModelTypeName(entry.name);
        module.initSlot(slot);
        log.debug("@ insert module '" + entry.name
                + "' slot " + module.getSlot().getSlotIndex()
                + " on '" + device.getDeviceName() + "'");
        module.setHardwareRevision(entry.hardwareRev);
        log.debug("@ set module '" + entry.name
                + "' slot " + module.getSlot().getSlotIndex()
                + " on '" + device.getDeviceName() + "'"
                + " hardwareRev '" + entry.hardwareRev + "'");
        module.setSerialNumber(entry.serialNumber);
        log.debug("@ set module '" + entry.name
                + "' slot " + module.getSlot().getSlotIndex()
                + " on '" + device.getDeviceName() + "'"
                + " serialNum '" + entry.serialNumber + "'");
        module.setSystemDescription(entry.descr);
        log.debug("@ set module '" + entry.name
                + "' slot " + module.getSlot().getSlotIndex()
                + " on '" + device.getDeviceName() + "'"
                + " descr '" + entry.descr + "'");
        return module;
    }

    public static Slot createSlot(PhysicalEntry entry, Container parent) {
        String slotID = null;
        slotID = String.valueOf(entry.position);
        Slot slot = createSlot(parent, entry.position, slotID, entry.physicalName);
        return slot;
    }

    public static Slot createSlot(Container parent, int index, String id, String name) {
        Slot slot = new SlotImpl();
        slot.initContainer(parent);
        slot.initSlotIndex(index);
        slot.initSlotId(id);
        slot.setSlotName(name);
        String parentName;
        if (parent instanceof Module) {
            Module module = (Module) parent;
            parentName = "module:" + module.getSlot().getSlotId()
                    + "(" + module.getModelTypeName() + "'";
        } else {
            parentName = parent.getDevice().getDeviceName();
        }
        log.debug("@ create slot '" + slot.getSlotIndex()
                + "' on '" + parentName + "'");
        parent.addSlot(slot);
        return slot;
    }

}