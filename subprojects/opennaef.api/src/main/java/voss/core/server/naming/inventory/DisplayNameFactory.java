package voss.core.server.naming.inventory;

import naef.dto.*;
import naef.dto.atm.AtmPvcIfDto;
import naef.dto.ip.IpIfDto;
import naef.dto.serial.TdmSerialIfDto;
import naef.dto.vlan.VlanIfDto;
import tef.skelton.NamedModel;
import voss.core.server.constant.DiffObjectType;

public class DisplayNameFactory {

    public static String getDisplayName(NaefDto dto) {
        if (dto instanceof NamedModel) {
            return ((NamedModel) dto).getName();
        }
        return dto.getAbsoluteName();
    }

    public static String getDisplayName(Class<? extends NaefDto> cls) {
        return cls.getSimpleName().replace("Dto", "");
    }

    public static String getDisplayName(Class<? extends NaefDto> cls, String attributeName) {
        return attributeName;
    }

    public static String getTypeName(NaefDto dto) {
        if (dto == null) {
            return null;
        } else if (dto instanceof NodeDto) {
            return DiffObjectType.NODE.getCaption();
        } else if (dto instanceof ChassisDto) {
            return DiffObjectType.CHASSIS.getCaption();
        } else if (dto instanceof SlotDto) {
            return DiffObjectType.SLOT.getCaption();
        } else if (dto instanceof ModuleDto) {
            return DiffObjectType.MODULE.getCaption();
        } else if (dto instanceof VlanIfDto) {
            return DiffObjectType.VLAN_SUBIF.getCaption();
        } else if (dto instanceof AtmPvcIfDto) {
            return DiffObjectType.ATM_PVC.getCaption();
        } else if (dto instanceof TdmSerialIfDto) {
            return DiffObjectType.SERIAL.getCaption();
        } else if (dto instanceof IpIfDto) {
            return DiffObjectType.LOOPBACK.getCaption();
        } else if (dto instanceof PortDto) {
            return DiffObjectType.PORT.getCaption();
        }
        return dto.getObjectTypeName();
    }

}