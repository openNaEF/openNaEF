package voss.nms.inventory.diff.network.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.model.*;
import voss.model.LogicalEthernetPort.TagChanger;
import voss.nms.inventory.diff.network.DiffConstants;
import voss.util.ModelUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceAnalyzer {
    private final Device device;
    private final Map<String, Renderer> result = new HashMap<String, Renderer>();
    private final Map<String, String> idsMap = new HashMap<String, String>();
    private final Logger log = LoggerFactory.getLogger(DeviceAnalyzer.class);

    public DeviceAnalyzer(Device device) {
        if (device == null) {
            throw new IllegalArgumentException();
        }
        this.device = device;
    }

    public Map<String, Renderer> analyze() {
        buildRenderer(device, DiffConstants.nodeDepth, null);
        return result;
    }

    private void buildRenderer(VlanModel model, int depth, String parentID) {
        if (model == null) {
            return;
        }
        if (model instanceof Port && ((Port) model).isAliasPort()) {
            buildAliasPortRenderer(model, depth, parentID);
        } else if (model instanceof Device) {
            buildDeviceRenderer(model, depth);
        } else if (model instanceof Slot) {
            buildSlotRenderer(model, depth, parentID);
        } else if (model instanceof Module) {
            buildModuleRenderer(model, depth, parentID);
        } else if (model instanceof EthernetPortsAggregator) {
            buildEthernetLAGRenderer((EthernetPortsAggregator) model, depth, parentID);
        } else if (model instanceof EthernetProtectionPort) {
            buildEthernetEPSRenderer((EthernetProtectionPort) model, depth, parentID);
        } else if (model instanceof EthernetPort) {
            buildEthernetPortRenderer((EthernetPort) model, depth, parentID);
        } else if (model instanceof AtmAPSImpl) {
            buildAtmAPSPortRenderer((AtmAPSImpl) model, depth, parentID);
        } else if (model instanceof POSAPSImpl) {
            buildPOSAPSPortRenderer((POSAPSImpl) model, depth, parentID);
        } else if (model instanceof POS) {
            buildPosPortRenderer((POSImpl) model, depth, parentID);
        } else if (model instanceof SerialPort) {
            buildSerialPortRenderer((SerialPortImpl) model, depth, parentID);
        } else if (model instanceof AtmPortImpl) {
            buildAtmPortRenderer((AtmPortImpl) model, depth, parentID);
        } else if (model instanceof RouterVlanIf) {
            buildVlanIfRenderer((RouterVlanIf) model, depth, parentID);
        } else if (model instanceof VlanIf) {
            buildVlanIfRenderer((VlanIf) model, depth, parentID);
        } else if (model instanceof LoopbackInterface) {
            buildLoopbackPortRenderer(model, depth, parentID);
        } else if (model instanceof PseudoWirePort) {
        } else if (model instanceof MplsTunnel) {
        } else if (model instanceof LabelSwitchedPathEndPoint) {
        } else if (model instanceof VplsInstance) {
        } else if (model instanceof VrfInstance) {
        } else if (model instanceof NodePipe<?>) {
        } else {
            log.warn("not-supported: " + model.getClass().getName() + " " + ((Port) model).getFullyQualifiedName());
        }
    }

    private void buildAliasPortRenderer(VlanModel model, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        AliasPortRenderer renderer = new AliasPortRenderer((Port) model, parentID, depth + 1, idsMap);
        String id = renderer.getID();
        log.trace("* AliasPortID:" + id);
        result.put(id, renderer);
    }

    private void buildAtmAPSPortRenderer(AtmAPSImpl aps, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        ATMAPSRenderer renderer = new ATMAPSRenderer(aps, parentID, depth + 100, idsMap);
        depth = renderer.getDepth();
        String id = renderer.getID();
        log.trace("* ATM APS ID:" + id);
        result.put(id, renderer);
        AtmPort atmFeature = ModelUtils.getAtmPort(aps);
        if (atmFeature != null) {
            for (AtmVp vp : atmFeature.getVps()) {
                buildAtmVpRenderer(vp, depth + 1, id);
            }
        }
    }

    private void buildAtmPortRenderer(AtmPortImpl atm, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        AtmPortRenderer renderer = new AtmPortRenderer(atm, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* AtmPortID:" + id);
        result.put(id, renderer);
        AtmPort atmFeature = ModelUtils.getAtmPort(atm);
        if (atmFeature != null) {
            for (AtmVp vp : atmFeature.getVps()) {
                buildAtmVpRenderer(vp, depth + 200, id);
            }
        }
    }

    private void buildAtmPvcRenderer(AtmPvc pvc, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        AtmPvcIfRenderer renderer = new AtmPvcIfRenderer(pvc, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* AtmPvcIfID:" + id);
        result.put(id, renderer);
    }

    private void buildAtmVpRenderer(AtmVp vp, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        AtmVpRenderer renderer = new AtmVpRenderer(vp, parentID, depth, idsMap);
        String id = renderer.getID();
        log.debug("* AtmVpIfID:" + id);
        result.put(id, renderer);
        for (AtmPvc pvc : vp.getPvcs()) {
            buildAtmPvcRenderer(pvc, depth + 1, id);
        }
    }

    private void buildDeviceRenderer(VlanModel model, int depth) {
        Device device = (Device) model;
        DeviceRenderer renderer = new DeviceRenderer(device, depth, idsMap);
        String deviceInventoryID = renderer.getID();
        result.put(deviceInventoryID, renderer);
        log.trace("* deviceID:" + deviceInventoryID);
        ChassisRenderer renderer2 = new ChassisRenderer(device, deviceInventoryID, depth + 5, idsMap);
        String chassisID = renderer2.getID();
        result.put(chassisID, renderer2);
        log.trace("* chassisID:" + chassisID);
        for (Slot slot : device.getSlots()) {
            buildSlotRenderer(slot, depth + 10, chassisID);
        }
        for (PhysicalPort port : device.selectPorts(PhysicalPort.class)) {
            if (port.getModule() != null) {
                continue;
            }
            buildRenderer(port, depth + 20, chassisID);
        }
        for (Port port : device.getPorts()) {
            if (!isSupportedDeviceLevelPort(port)) {
                log.trace("skipped:" + port.getFullyQualifiedName());
                continue;
            }
            buildRenderer(port, depth + 30, deviceInventoryID);
        }
    }

    private void buildEthernetEPSRenderer(EthernetProtectionPort eps, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        depth = depth + 100;
        EthernetEPSRenderer renderer = new EthernetEPSRenderer(eps, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* EthernetPortID:" + id);
        result.put(id, renderer);
        depth = renderer.getDepth();
        if (eps.getDevice() instanceof MplsVlanDevice) {
            buildEthernetTypeSubInterfaceRenderer(eps, depth + 1, id);
        }
        if (eps.getDevice() instanceof VlanDevice) {
            buildTagChangerRenderers(eps, depth + 1, id);
        }
    }

    private void buildEthernetLAGRenderer(EthernetPortsAggregator lag, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        depth = depth + 100;
        EthernetLAGRenderer renderer = new EthernetLAGRenderer(lag, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* EthernetPortID:" + id);
        result.put(id, renderer);
        depth = renderer.getDepth();
        if (lag.getDevice() instanceof MplsVlanDevice) {
            buildEthernetTypeSubInterfaceRenderer(lag, depth + 1, id);
        }
        if (lag.getDevice() instanceof VlanDevice) {
            buildTagChangerRenderers(lag, depth + 1, id);
        }
    }

    private void buildEthernetPortRenderer(EthernetPort port, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        Renderer renderer = null;
        String _ = "";
        if (port.getDevice().isVirtualDevice()) {
            renderer = new VMEthernetPortRenderer(port, parentID, depth, idsMap);
            _ = "(Virtual) ";
        } else {
            renderer = new EthernetPortRenderer(port, parentID, depth, idsMap);
        }
        String id = renderer.getID();
        log.trace("* EthernetPortID:" + _ + id);
        log.trace("  - " + port.getPortTypeName());
        result.put(id, renderer);
        if (port.getDevice() instanceof MplsVlanDevice) {
            MplsVlanDevice mplsDevice = (MplsVlanDevice) port.getDevice();
            LogicalEthernetPort le = mplsDevice.getLogicalEthernetPort(port);
            buildEthernetTypeSubInterfaceRenderer(le, depth + 1, id);
        }
        if (port.getDevice() instanceof VlanDevice) {
            VlanDevice vlanDevice = (VlanDevice) port.getDevice();
            LogicalEthernetPort le = vlanDevice.getLogicalEthernetPort(port);
            buildTagChangerRenderers(le, depth + 101, id);
        }
    }

    private void buildEthernetTypeSubInterfaceRenderer(LogicalEthernetPort le, int depth, String parentID) {
        if (!(le.getDevice() instanceof MplsVlanDevice)) {
            return;
        }
        MplsVlanDevice device = (MplsVlanDevice) le.getDevice();
        List<VlanIf> routerVlans = device.getRouterVlanIfsOn(le);
        for (VlanIf routerVlan : routerVlans) {
            buildVlanIfRenderer(routerVlan, depth + 1, parentID);
        }
    }

    private void buildLoopbackPortRenderer(VlanModel model, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        LoopbackPortRenderer renderer = new LoopbackPortRenderer(
                (LoopbackInterface) model, parentID, depth + 1, idsMap);
        String id = renderer.getID();
        log.trace("* LoopbackID:" + id);
        result.put(id, renderer);
    }

    private void buildModuleRenderer(VlanModel model, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        Module module = (Module) model;
        ModuleRenderer renderer = new ModuleRenderer(module, parentID, depth, idsMap);
        String moduleID = renderer.getID();
        result.put(moduleID, renderer);
        log.trace("* moduleID:" + moduleID);
        if (module.getPorts() != null) {
            for (Port port : module.getPorts()) {
                buildRenderer(port, depth + 1, moduleID);
            }
        }
        if (module.getSlots() != null) {
            for (Slot slot : module.getSlots()) {
                buildSlotRenderer(slot, depth + 1, moduleID);
            }
        }
    }

    private void buildPOSAPSPortRenderer(POSAPSImpl aps, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        POSAPSRenderer renderer = new POSAPSRenderer(
                (POSAPSImpl) aps, parentID, depth, idsMap);
        depth = renderer.getDepth();
        String id = renderer.getID();
        log.trace("* POS APS ID:" + id);
        result.put(id, renderer);
        if (aps.getLogicalFeature() != null && aps.getLogicalFeature() instanceof ChannelPortFeature) {
            ChannelPortFeature cesFeature = (ChannelPortFeature) aps.getLogicalFeature();
            for (Channel ch : cesFeature.getChannels()) {
                buildTdmSerialIfRenderer(ch, depth + 1, id);
            }
        }
    }

    private void buildPosPortRenderer(POSImpl pos, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        PosPortRenderer renderer = new PosPortRenderer(pos, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* PosPortID:" + id);
        result.put(id, renderer);
        AtmPort atmFeature = ModelUtils.getAtmPort(pos);
        if (atmFeature != null) {
            for (AtmVp vp : atmFeature.getVps()) {
                buildAtmVpRenderer(vp, depth + 1, id);
            }
        }
        if (pos.getLogicalFeature() != null && pos.getLogicalFeature() instanceof ChannelPortFeature) {
            ChannelPortFeature cesFeature = (ChannelPortFeature) pos.getLogicalFeature();
            for (Channel ch : cesFeature.getChannels()) {
                buildTdmSerialIfRenderer(ch, depth + 1, id);
            }
        }
    }

    private void buildSerialPortRenderer(SerialPortImpl port, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        SerialPortRenderer renderer = new SerialPortRenderer(port, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* SerialPortID:" + id);
        log.trace("  - " + port.getPortTypeName());
        result.put(id, renderer);
        if (port.getLogicalFeature() != null && port.getLogicalFeature() instanceof ChannelPortFeature) {
            ChannelPortFeature cesFeature = (ChannelPortFeature) port.getLogicalFeature();
            for (Channel ch : cesFeature.getChannels()) {
                buildTdmSerialIfRenderer(ch, depth + 1, id);
            }
        }
    }

    private void buildSlotRenderer(VlanModel model, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        Slot slot = (Slot) model;
        SlotRenderer renderer = new SlotRenderer(slot, parentID, depth, idsMap);
        String slotID = renderer.getID();
        result.put(slotID, renderer);
        log.trace("* slotID:" + slotID);
        if (slot.getModule() != null) {
            buildModuleRenderer(slot.getModule(), depth + 1, slotID);
        }
    }

    private void buildTagChangerRenderers(LogicalEthernetPort le, int depth, String parentID) {
        if (!(le.getDevice() instanceof VlanDevice)) {
            return;
        }
        for (TagChanger tc : le.getTagChangers()) {
            buildTagChangerRenderer(tc, depth + 1, parentID);
        }
    }

    private void buildTagChangerRenderer(TagChanger tc, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        TagChangerRenderer renderer = new TagChangerRenderer(tc, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* TagChangerID:" + id);
        result.put(id, renderer);
    }

    private void buildTdmSerialIfRenderer(Channel channel, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        TdmSerialIfRenderer renderer = new TdmSerialIfRenderer(channel, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* ChannelIfID:" + id);
        result.put(id, renderer);
    }

    private void buildVlanIfRenderer(VlanIf vlanIf, int depth, String parentID) {
        if (parentID == null) {
            throw new IllegalArgumentException();
        }
        log.trace("** parentID=" + parentID + "->" + idsMap.get(parentID));
        VlanIfRenderer renderer = new VlanIfRenderer(vlanIf, parentID, depth, idsMap);
        String id = renderer.getID();
        log.trace("* VlanIfID:" + id);
        result.put(id, renderer);
        VlanIfBindingRenderer renderer2 = new VlanIfBindingRenderer(vlanIf, parentID, depth + 200, idsMap);
        String id2 = renderer2.getID();
        log.trace("* VlanIfBindingID:" + id2);
        result.put(id2, renderer2);
    }

    private boolean isSupportedDeviceLevelPort(Port port) {
        if (port == null) {
            return false;
        } else if (port.isAliasPort()) {
            return true;
        } else if (port instanceof LoopbackInterface) {
            return true;
        } else if (port instanceof AtmAPSImpl) {
            return true;
        } else if (port instanceof POSAPSImpl) {
            return true;
        } else if (port instanceof EthernetPortsAggregator) {
            return true;
        } else if (port instanceof EthernetProtectionPort) {
            return true;
        } else if (port instanceof TagChanger) {
            return false;
        } else if (port instanceof VlanIf) {
            if (port instanceof RouterVlanIf) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
}