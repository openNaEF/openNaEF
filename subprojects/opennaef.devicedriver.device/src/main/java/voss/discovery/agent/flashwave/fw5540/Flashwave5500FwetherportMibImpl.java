package voss.discovery.agent.flashwave.fw5540;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.IntegerKey;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.model.EthernetPort;
import voss.model.EthernetPort.AutoNego;
import voss.model.EthernetPort.Duplex;
import voss.model.GenericEthernetSwitch;
import voss.model.value.PortSpeedValue;

import java.io.IOException;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.intEntryBuilder;
import static voss.discovery.iolib.snmp.SnmpHelper.integerKeyCreator;

public class Flashwave5500FwetherportMibImpl {
    public static final String fwBase = ".1.3.6.1.4.1.211.1.24.7.1.1.2";

    public static final String fwEtherPort = fwBase + ".3";
    public static final String fwEtherPortTable = fwEtherPort + ".1";
    public static final String fwEtherPortEntry = fwEtherPortTable + ".1";
    public static final String fwEtherPortType = fwEtherPortEntry + ".2";
    public static final String fwEtherPortFfpIndex = fwEtherPortEntry + ".5";
    public static final String fwEtherPortLagIndex = fwEtherPortEntry + ".6";
    public static final String fwEtherPhyPortTable = fwEtherPort + ".2";
    public static final String fwEtherPhyPortEntry = fwEtherPhyPortTable + ".1";
    public static final String fwEtherPhyPortCnfAutoNego = fwEtherPhyPortEntry + ".1";
    public static final String fwEtherPhyPortCnfSpeedSelect = fwEtherPhyPortEntry + ".4";
    public static final String fwEtherPhyPortCnfSpeedStatus = fwEtherPhyPortEntry + ".5";
    public static final String fwEtherPhyPortDuplexStatus = fwEtherPhyPortEntry + ".6";

    public enum FwEtherPortType {
        notSet(0),
        portVWAN(1),
        tagVWAN(2),
        vportVWAN(3),
        tagVWANExt(4);
        final int value;

        FwEtherPortType(int value) {
            this.value = value;
        }

        static FwEtherPortType get(int value) {
            for (FwEtherPortType type : FwEtherPortType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(Flashwave5500FwetherportMibImpl.class);

    public static int getFfpIndex(SnmpAccess snmp, int ifIndex) throws AbortedException, IOException {
        try {
            return SnmpUtil.getInteger(snmp, fwEtherPortFfpIndex + "." + ifIndex);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public static int getLagIndex(SnmpAccess snmp, int ifIndex) throws AbortedException, IOException {
        try {
            return SnmpUtil.getInteger(snmp, fwEtherPortLagIndex + "." + ifIndex);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public static void setAllPhyPortInfo(SnmpAccess snmp, GenericEthernetSwitch device) throws AbortedException, IOException {

        Map<IntegerKey, IntSnmpEntry> fwEtherPhyPortCnfAutoNegoMap =
                SnmpUtil.getWalkResult(snmp, fwEtherPhyPortCnfAutoNego, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> fwEtherPhyPortCnfSpeedSelectMap =
                SnmpUtil.getWalkResult(snmp, fwEtherPhyPortCnfSpeedSelect, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> fwEtherPhyPortCnfSpeedStatusMap =
                SnmpUtil.getWalkResult(snmp, fwEtherPhyPortCnfSpeedStatus, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> fwEtherPhyPortDuplexStatusMap =
                SnmpUtil.getWalkResult(snmp, fwEtherPhyPortDuplexStatus, intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : fwEtherPhyPortCnfAutoNegoMap.keySet()) {
            int ifIndex = key.getInt();
            EthernetPort port = (EthernetPort) device.getPortByIfIndex(ifIndex);

            switch (fwEtherPhyPortCnfAutoNegoMap.get(key).intValue()) {
                case 0:
                    port.setAutoNego(AutoNego.ON);
                    break;
                case 1:
                    port.setAutoNego(AutoNego.OFF);
                    break;
                default:
                    throw new IllegalStateException();
            }

            long adminSpeed = fwEtherPhyPortCnfSpeedSelectMap.get(key).longValue() * 1000000L;
            port.setPortAdministrativeSpeed(new PortSpeedValue.Admin(adminSpeed));

            long operSpeed = fwEtherPhyPortCnfSpeedStatusMap.get(key).longValue() * 1000000L;
            port.setPortOperationalSpeed(new PortSpeedValue.Oper(operSpeed));

            switch (fwEtherPhyPortDuplexStatusMap.get(key).intValue()) {
                case 1:
                    break;
                case 2:
                    port.setDuplex(Duplex.HALF);
                    break;
                case 3:
                    port.setDuplex(Duplex.FULL);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public static FwEtherPortType getFwEtherPortType(SnmpAccess snmp, int ifIndex) throws IOException, AbortedException {
        Map<IntegerKey, IntSnmpEntry> fwEtherPortTypeMap =
                SnmpUtil.getWalkResult(snmp, fwEtherPortType, intEntryBuilder, integerKeyCreator);
        for (IntegerKey key : fwEtherPortTypeMap.keySet()) {
            if (key.getInt() == ifIndex) {
                return FwEtherPortType.get(fwEtherPortTypeMap.get(key).intValue());
            }
        }
        throw new IllegalArgumentException("ifIndex=" + ifIndex);
    }
}