package voss.discovery.agent.flashwave.fw5540;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class Flashwave5500FwequipmentMibImpl {
    private static final Logger log = LoggerFactory.getLogger(Flashwave5500FwequipmentMibImpl.class);

    public static final String fwBase = ".1.3.6.1.4.1.211.1.24.7.1.1.2";

    public static final String fwEquipment = fwBase + ".2";
    public static final String fwEqShelfTable = fwEquipment + ".2";
    public static final String fwEquipShelfEntry = fwEqShelfTable + ".1";
    public static final String fwEqPIShelfSerialNum = fwEquipShelfEntry + ".5";
    public static final String fwEqConfLiuTable = fwEquipment + ".8";
    public static final String fwEqConfLiuEntry = fwEqConfLiuTable + ".1";
    public static final String fwEqConfLiuType = fwEqConfLiuEntry + ".2";
    public static final String fwEqLiuTable = fwEquipment + ".9";
    public static final String fwEqLiuEntry = fwEqLiuTable + ".1";
    public static final String fwEqPILiuFcNum = fwEqLiuEntry + ".2";
    public static final String fwEqPILiuSerialNum = fwEqLiuEntry + ".5";
    public static final String fwEqPILiuRevNum = fwEqLiuEntry + ".10";

    public enum FwEqConfLiuType {
        notSet(0),
        fetp32(1),
        fet2p32(2),
        fet3p32(3),
        gexp8(11),
        gex2p8(12),
        gex3p8(13);
        int value;

        FwEqConfLiuType(int value) {
            this.value = value;
        }

        static FwEqConfLiuType get(int value) {
            for (FwEqConfLiuType liuType : FwEqConfLiuType.values()) {
                if (liuType.value == value) {
                    return liuType;
                }
            }
            return null;
        }
    }

    public static String getChassisSerialNumber(SnmpAccess snmp) throws IOException, AbortedException {
        try {
            return SnmpUtil.getString(snmp, fwEqPIShelfSerialNum + ".1");
        } catch (NoSuchMibException e) {
            log.info("no such mib: fwEqPIShelfSerialNum.1");
            return null;
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    public static void getSlotAndModule(SnmpAccess snmp, GenericEthernetSwitch device) throws AbortedException, IOException {

        Map<IntegerKey, IntSnmpEntry> fwEqConfLiuTypeMap =
                SnmpUtil.getWalkResult(snmp, fwEqConfLiuType, intEntryBuilder, integerKeyCreator);

        Map<IntegerKey, StringSnmpEntry> fwEqPILiuFcNumMap =
                SnmpUtil.getWalkResult(snmp, fwEqPILiuFcNum, stringEntryBuilder, integerKeyCreator);
        Map<IntegerKey, StringSnmpEntry> fwEqPILiuSerialNumMap =
                SnmpUtil.getWalkResult(snmp, fwEqPILiuSerialNum, stringEntryBuilder, integerKeyCreator);
        Map<IntegerKey, StringSnmpEntry> fwEqPILiuRevNumMap =
                SnmpUtil.getWalkResult(snmp, fwEqPILiuRevNum, stringEntryBuilder, integerKeyCreator);

        for (IntegerKey key : fwEqConfLiuTypeMap.keySet()) {
            int slotId = key.getInt();
            FwEqConfLiuType liuType = FwEqConfLiuType.get(fwEqConfLiuTypeMap.get(key).intValue());

            Slot slot = new SlotImpl();
            slot.initSlotIndex(slotId);
            slot.initSlotId(Integer.toString(slotId));
            slot.initContainer(device);

            if (liuType != null && liuType != FwEqConfLiuType.notSet) {
                Module module = new ModuleImpl();
                module.initSlot(slot);
                module.setModelTypeName(fwEqPILiuFcNumMap.get(key).getValue().trim());
                module.setHardwareRevision(fwEqPILiuRevNumMap.get(key).getValue());
                module.setSerialNumber(fwEqPILiuSerialNumMap.get(key).getValue());
                module.setSystemDescription(liuType.name());

                log.debug("@ add module '" + module.getModelTypeName()
                        + "' to slot " + slotId
                        + " on device '" + device.getDeviceName() + "'");
            }
        }
    }
}