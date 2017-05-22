package voss.discovery.agent.cisco;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.EntityType;
import voss.discovery.agent.common.PhysicalEntry;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.model.*;

import java.io.IOException;

public class Cisco12000GsrIosDiscovery extends CiscoIosDiscovery {
    private static final Logger log = LoggerFactory.getLogger(Cisco12000GsrIosDiscovery.class);

    public Cisco12000GsrIosDiscovery(DeviceAccess access) throws IOException,
            AbortedException {
        super(access);
    }

    protected void createPhysicalModel(PhysicalEntry entry, VlanModel target) throws IOException, AbortedException {
        MplsVlanDevice device = (MplsVlanDevice) getDeviceInner();

        log.trace("target: " + (target == null ? "null" : target.getClass().getSimpleName()));
        log.debug("entry: " + entry.toString());
        VlanModel current = target;
        if (entry.type == EntityType.CHASSIS) {

            current = device;

        } else if (entry.type == EntityType.CONTAINER
                && target instanceof Device) {

            if (isBay(entry.name)) {
                if (isModuleBay(entry.name)) {
                    current = target;
                } else {
                    current = null;
                }
            } else {
                Slot slot = new SlotImpl();
                slot.initContainer(device);
                slot.initSlotIndex(entry.position);
                slot.initSlotId(String.valueOf(entry.position));
                slot.setSlotName(entry.physicalName);
                device.addSlot(slot);
                log.debug("@ create slot '" + slot.getSlotIndex()
                        + "' on '" + device.getDeviceName() + "'");
                current = slot;
            }

        } else if (entry.type == EntityType.MODULE && target instanceof Device) {
            Slot slot = new SlotImpl();
            slot.initContainer(device);
            slot.initSlotIndex(-1);
            slot.initSlotId("-");
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

            current = module;

        } else if (entry.type == EntityType.MODULE && target instanceof Slot) {
            Module module = new ModuleImpl();
            module.setModelTypeName(entry.name);
            module.initSlot((Slot) target);
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

            current = module;

        } else if (entry.type == EntityType.PORT && target instanceof Module) {
            Module module = (Module) target;
            String ifname = CiscoIosCommandParseUtil.getShortIfName(entry.physicalName);
            PhysicalPort port = CiscoIosCommandParseUtil.createCiscoPhysicalPort(getSnmpAccess(), device, ifname, entry.ifindex);
            module.addPort(port);
            current = port;
            log.debug("@ add port ifname='" + ifname + "'"
                    + " on device '" + device.getDeviceName() + "'");

        } else if (entry.type == EntityType.CONTAINER && target instanceof Module) {
            Module module = (Module) target;
            if (CiscoHardwareUtil.isSubSlot(entry)) {
                Slot slot = new SlotImpl();
                slot.initContainer(module);
                slot.initSlotIndex(entry.position);
                slot.initSlotId("" + entry.position);
                slot.setSlotName(entry.physicalName);
                module.addSlot(slot);
                log.debug("@ create slot '" + slot.getSlotId()
                        + "' on 'module:" + module.getSlot().getSlotId()
                        + "(" + module.getModelTypeName() + ")'");

                current = slot;
            } else if (CiscoHardwareUtil.isPortModule(entry)) {
                current = module;
            } else {
                String portName = CiscoIosCommandParseUtil.guessIfName(entry.physicalName);
                log.debug("guessed: " + portName);
                PhysicalPort port = (PhysicalPort) device.getPortByIfName(portName);
                module.addPort(port);
                current = port;
                log.debug("@ add port '" + port.getIfIndex()
                        + " to slot '" + module.getSlot().getSlotIndex()
                        + " on device '" + device.getDeviceName() + "'");
            }

        } else {
            log.debug("ignored: " + entry.toString());
        }
        for (PhysicalEntry child : entry.getChildren()) {
            createPhysicalModel(child, current);
        }
    }

    private boolean isModuleBay(String moduleName) {
        if (moduleName == null) {
            return false;
        }
        return moduleName.matches("cevContainer.*ModuleBay");
    }

    private boolean isBay(String moduleName) {
        if (moduleName == null) {
            return false;
        }
        return moduleName.matches("cev.*Bay");
    }
}