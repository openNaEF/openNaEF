package voss.discovery.agent.alaxala;


import voss.discovery.agent.alaxala.mib.Alaxala7800SMibImpl;
import voss.discovery.agent.alaxala.mib.AlaxalaMibImpl;
import voss.discovery.agent.alaxala.profile.AlaxalaVendorProfile;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.DeviceAccess;
import voss.model.Module;
import voss.model.ModuleImpl;
import voss.model.Slot;
import voss.model.SlotImpl;

import java.io.IOException;

public class Alaxala7800SDiscovery extends AlaxalaSwitchDiscovery {

    public Alaxala7800SDiscovery(DeviceAccess access, AlaxalaVendorProfile profile) {
        super(access, profile, new Alaxala7800SMibImpl(access.getSnmpAccess(), profile));
    }

    public Alaxala7800SDiscovery(DeviceAccess access, AlaxalaVendorProfile profile, AlaxalaMibImpl methods) {
        super(access, profile, methods);
    }

    protected void collectSlotsAndModules() throws IOException, AbortedException {
        int numberOfSlot = method.getNumberOfSlot();

        for (int i = 0; i < numberOfSlot; i++) {
            final Slot slot = new SlotImpl();
            slot.initContainer(device);
            device.getSlots()[i] = slot;

            slot.initSlotIndex(i);
            String moduleName = method.getNifBoardName(i + 1);
            if (moduleName != null) {
                Module module = new ModuleImpl();
                module.initSlot(slot);
                module.setModelTypeName(moduleName);
                module.setSerialNumber(method.getNifBoardSerialNumber(i + 1));
            }
        }
    }

    @Override
    public void getDynamicStatus() throws IOException, AbortedException {
    }
}