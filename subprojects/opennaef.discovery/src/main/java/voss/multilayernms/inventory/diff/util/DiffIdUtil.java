package voss.multilayernms.inventory.diff.util;

import naef.dto.*;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import voss.core.server.naming.inventory.InventoryIdCalculator;
import voss.multilayernms.inventory.renderer.NodeRenderer;

public class DiffIdUtil {

    public static String getDiffId(NaefDto element) {
        if (element instanceof NodeDto) return getExternalInventoryDBId(element);
        if (element instanceof ChassisDto) return getDiffId((ChassisDto) element);
        if (element instanceof SlotDto) return getDiffId((SlotDto) element);
        if (element instanceof ModuleDto) return getDiffId((ModuleDto) element);
        if (element instanceof PortDto) return getDiffId((PortDto) element);
        if (element instanceof LocationDto) return getExternalInventoryDBId(element);
        return null;
    }

    public static String getExternalInventoryDBId(NaefDto dto) {
        return NodeRenderer.getExternalInventoryDBID((NodeDto) dto);
    }

    private static String getDiffId(ChassisDto chassis) {
        return chassis.getOwner().getName();
    }

    private static String getDiffId(SlotDto slot) {
        StringBuffer buf = new StringBuffer();
        buf.append(slot.getNode().getName());
        buf.append(":");
        buf.append(slot.getName());
        return buf.toString();
    }

    private static String getDiffId(ModuleDto module) {
        StringBuffer buf = new StringBuffer();
        buf.append(module.getNode().getName());
        buf.append(":");
        buf.append(module.getOwner().getName());
        buf.append("-");
        buf.append(module.getName());
        return buf.toString();
    }

    private static String getDiffId(PortDto port) {
        StringBuffer buf = new StringBuffer();
        buf.append(port.getNode().getName());
        buf.append(":");
        buf.append(port.getOwner().getName());
        buf.append(":");
        buf.append(port.getName());
        return buf.toString();
    }

    public static String getInventoryId(NaefDto onDB) {
        if (onDB instanceof NodeDto) return InventoryIdCalculator.getId((NodeDto) onDB);
        if (onDB instanceof ChassisDto) return InventoryIdCalculator.getId((ChassisDto) onDB);
        if (onDB instanceof SlotDto) return InventoryIdCalculator.getId((SlotDto) onDB);
        if (onDB instanceof ModuleDto) return InventoryIdCalculator.getId((ModuleDto) onDB);
        if (onDB instanceof PortDto) return InventoryIdCalculator.getId((PortDto) onDB);
        if (onDB instanceof LocationDto) return ((LocationDto) onDB).getName();
        if (onDB instanceof JackDto) return InventoryIdCalculator.getId((JackDto) onDB);
        if (onDB instanceof RsvpLspDto) return InventoryIdCalculator.getId((RsvpLspDto) onDB);
        if (onDB instanceof PseudowireDto) return InventoryIdCalculator.getId((PseudowireDto) onDB);
        return null;
    }
}