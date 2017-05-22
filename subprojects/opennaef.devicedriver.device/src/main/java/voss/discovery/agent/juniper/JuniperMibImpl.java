package voss.discovery.agent.juniper;

import net.snmp.SnmpResponseException;
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

public class JuniperMibImpl implements JuniperMib {

    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public JuniperMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
    }

    public void createSlotAndModule() throws IOException, AbortedException {

        Map<OidKey, IntSnmpEntry> jnxFruTypeMap =
                SnmpUtil.getWalkResult(snmp, jnxFruType, intEntryBuilder, oidKeyCreator);
        Map<OidKey, IntSnmpEntry> jnxFruStateMap =
                SnmpUtil.getWalkResult(snmp, jnxFruState, intEntryBuilder, oidKeyCreator);

        Map<OidKey, StringSnmpEntry> jnxContentsDescrMap =
                SnmpUtil.getWalkResult(snmp, jnxContentsDescr, stringEntryBuilder, oidKeyCreator);
        Map<OidKey, StringSnmpEntry> jnxContentsSerialNoMap =
                SnmpUtil.getWalkResult(snmp, jnxContentsSerialNo, stringEntryBuilder, oidKeyCreator);
        Map<OidKey, StringSnmpEntry> jnxContentsRevisionMap =
                SnmpUtil.getWalkResult(snmp, jnxContentsRevision, stringEntryBuilder, oidKeyCreator);
        Map<OidKey, StringSnmpEntry> jnxContentsPartNoMap =
                SnmpUtil.getWalkResult(snmp, jnxContentsPartNo, stringEntryBuilder, oidKeyCreator);

        for (OidKey key : jnxFruTypeMap.keySet()) {

            JnxFruType fruType = JnxFruType.get(jnxFruTypeMap.get(key).intValue());

            if (fruType == JnxFruType.flexiblePicConcentrator) {
                int L1 = key.getInt(1) - 1;

                Slot slot = new SlotImpl();
                slot.initContainer(device);
                slot.initSlotId("" + L1);
                slot.initSlotIndex(L1);

                JnxFruState fruState = JnxFruState.get(jnxFruStateMap.get(key).intValue());
                if (fruState == JnxFruState.online) {
                    String contentsDescr = jnxContentsDescrMap.get(key).getValue();
                    String typeName = contentsDescr.replaceAll(" @ \\d+/\\*/\\*$", "");

                    Module fpc = new ModuleImpl();
                    fpc.initSlot(slot);
                    fpc.setHardwareRevision(jnxContentsRevisionMap.get(key).getValue());
                    fpc.setModelTypeName(typeName);
                    fpc.setSerialNumber(jnxContentsSerialNoMap.get(key).getValue());
                    fpc.setSystemDescription(jnxContentsPartNoMap.get(key).getValue());
                }
            }
        }

        for (OidKey key : jnxFruTypeMap.keySet()) {

            JnxFruType fruType = JnxFruType.get(jnxFruTypeMap.get(key).intValue());

            if (fruType == JnxFruType.portInterfaceCard) {

                if (jnxContentsDescrMap.get(key) == null) continue;

                int L1 = key.getInt(1) - 1;
                int L2 = key.getInt(2) - 1;

                Module fpc = device.getSlotBySlotIndex(L1).getModule();
                if (fpc == null) continue;

                Slot slot = new SlotImpl();
                slot.initContainer(fpc);
                slot.initSlotId(String.valueOf(L2));
                slot.initSlotIndex(L2);

                JnxFruState fruState = JnxFruState.get(jnxFruStateMap.get(key).intValue());
                if (fruState == JnxFruState.online) {
                    String contentsDescr = jnxContentsDescrMap.get(key).getValue();
                    String typeName = contentsDescr.replaceAll(" @ \\d+/\\d+/\\*$", "");

                    Module pic = new ModuleImpl();
                    pic.initSlot(slot);
                    pic.setHardwareRevision(jnxContentsRevisionMap.get(key).getValue());
                    pic.setModelTypeName(typeName);
                    pic.setSerialNumber(jnxContentsSerialNoMap.get(key).getValue());
                    pic.setSystemDescription(jnxContentsPartNoMap.get(key).getValue());
                }
            }
        }
    }

    public String getBoxDescr() throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, jnxBoxDescr + ".0");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public String getBoxSerialNo() throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, jnxBoxSerialNo + ".0");
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }
}