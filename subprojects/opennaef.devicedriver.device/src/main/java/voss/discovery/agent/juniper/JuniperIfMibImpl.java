package voss.discovery.agent.juniper;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.common.IanaIfType;
import voss.discovery.agent.mib.InterfaceMib;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.agent.mib.InterfaceMibImpl.UnknownIfTypeException;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.discovery.iolib.snmp.builder.MibTable;
import voss.discovery.iolib.snmp.builder.MibTable.KeyHolder;
import voss.discovery.iolib.snmp.builder.MibTable.TableRow;
import voss.model.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class JuniperIfMibImpl {

    public static final String ifJnx = JuniperSmi.jnxMibs + ".3";

    public static final String ifChassisTable = ifJnx + ".2";
    public static final String ifChassisEntry = ifChassisTable + ".1";
    public static final String ifChassisFpc = ifChassisEntry + ".1";
    public static final String ifChassisPic = ifChassisEntry + ".2";
    public static final String ifChassisPort = ifChassisEntry + ".3";

    private static final Logger log = LoggerFactory.getLogger(JuniperIfMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    private Map<IntegerKey, IntSnmpEntry> ifChassisFpcMap;
    private Map<IntegerKey, IntSnmpEntry> ifChassisPicMap;
    private Map<IntegerKey, IntSnmpEntry> ifChassisPortMap;

    private final InterfaceMibImpl interfaceMib;

    public JuniperIfMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;

        interfaceMib = new InterfaceMibImpl(snmp);

        try {
            ifChassisFpcMap = SnmpUtil.getWalkResult(snmp, ifChassisFpc, intEntryBuilder, integerKeyCreator);
            ifChassisPicMap = SnmpUtil.getWalkResult(snmp, ifChassisPic, intEntryBuilder, integerKeyCreator);
            ifChassisPortMap = SnmpUtil.getWalkResult(snmp, ifChassisPort, intEntryBuilder, integerKeyCreator);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AbortedException e) {
            e.printStackTrace();
        }
    }

    public void createPhysicalPorts() throws IOException, AbortedException {

        MibTable mibIfTable = new MibTable(snmp, "ifTable", InterfaceMib.ifTable);
        mibIfTable.addColumn(InterfaceMib.ifType_SUFFIX, "ifType");
        mibIfTable.walk();

        MibTable mibIfXTable = new MibTable(snmp, "ifXTable", InterfaceMib.ifXTable);
        mibIfXTable.addColumn(InterfaceMib.ifConnectorPresent_SUFFIX, "ifConnectorPresent");
        mibIfXTable.walk();
        Map<KeyHolder, TableRow> ifXTableResult = mibIfXTable.getKeyAndRows();

        for (TableRow row : mibIfTable.getRows()) {
            int ifindex = row.getKey().intValue(0);
            IanaIfType ianaType = IanaIfType.valueOf(row.getColumnValue(InterfaceMib.ifType_SUFFIX, intEntryBuilder).intValue());

            Port p = device.getPortByIfIndex(ifindex);
            if (p != null) {
                log.warn("port exists. ifIndex=" + ifindex);
                continue;
            }

            TableRow row2 = ifXTableResult.get(row.key);
            boolean connectorPresent = row2.getColumnValue(InterfaceMib.ifConnectorPresent_SUFFIX, intEntryBuilder).intValue() == 1;
            if (!connectorPresent) {
                continue;
            }

            log.debug("createPhysicalPorts(): ifindex=" + ifindex + ", ianaType=" + ianaType);
            PhysicalPort port;
            try {
                port = interfaceMib.getPhysicalPort(ianaType);

                IntegerKey key = new IntegerKey(new BigInteger[]{BigInteger.valueOf(ifindex)});
                int fpcNumber = ifChassisFpcMap.get(key).intValue() - 1;
                int picNumber = ifChassisPicMap.get(key).intValue() - 1;
                int portNumber = ifChassisPortMap.get(key).intValue() - 1;

                if (fpcNumber != -1 && picNumber != -1 && portNumber != -1) {
                    Slot fpcSlot = device.getSlotBySlotIndex(fpcNumber);
                    Module fpc = fpcSlot.getModule();
                    Slot picSlot = fpc.getSlotBySlotIndex(picNumber);
                    Module pic = picSlot.getModule();
                    port.initModule(pic);
                } else {
                    port.initDevice(device);
                }
                port.initIfIndex(ifindex);
                port.setPortTypeName(ianaType.toString());
                log.debug("@ add port " + ifindex + " type " + port.getClass().getSimpleName()
                        + " (" + ianaType.toString() + ")"
                        + " to device='" + device.getDeviceName() + "'");
            } catch (UnknownIfTypeException e) {
                log.warn("unknown if type: ifindex=" + ifindex + ";" + e.getMessage());
                continue;
            }
        }
    }

    public void setPortIfNames() throws IOException, AbortedException {
        interfaceMib.setAllIfNames(device);
    }

    public void setPortAttributes() throws IOException, AbortedException {
        interfaceMib.setAllIfOperStatus(device);
        interfaceMib.setAllIfAdminStatus(device);
        interfaceMib.setAllIfSpeed(device);
        interfaceMib.setAllIfTypes(device);
        interfaceMib.setAllIfAliases(device);
        interfaceMib.setAllIfDescriptions(device);
    }

    public int getPhysicalIfIndex(int fpcNumber, int picNumber, int portNumber) throws IOException, AbortedException {

        for (Port port : device.getPhysicalPorts()) {

            int ifIndex = port.getIfIndex();
            IntegerKey key = new IntegerKey(new BigInteger[]{BigInteger.valueOf(ifIndex)});
            if (fpcNumber == ifChassisFpcMap.get(key).intValue() - 1 &&
                    picNumber == ifChassisPicMap.get(key).intValue() - 1 &&
                    portNumber == ifChassisPortMap.get(key).intValue() - 1) {
                return ifIndex;
            }
        }

        return 0;
    }

    public int getPhysicalIfIndex(int ifIndex) throws IOException, AbortedException {

        IntegerKey key = new IntegerKey(new BigInteger[]{BigInteger.valueOf(ifIndex)});
        int fpcNumber = ifChassisFpcMap.get(key).intValue() - 1;
        int picNumber = ifChassisPicMap.get(key).intValue() - 1;
        int portNumber = ifChassisPortMap.get(key).intValue() - 1;

        return getPhysicalIfIndex(fpcNumber, picNumber, portNumber);
    }

    public String getIfName(int ifIndex) throws AbortedException, IOException {
        try {
            return SnmpUtil.getString(snmp, InterfaceMib.ifName + "." + ifIndex);
        } catch (SnmpResponseException e) {
            throw new IOException();
        } catch (NoSuchMibException e) {
            throw new IOException();
        }
    }

    public int getIfIndex(String ifName) throws IOException, AbortedException {
        Map<IntegerKey, StringSnmpEntry> ifNameMap =
                SnmpUtil.getWalkResult(snmp, InterfaceMib.ifName, stringEntryBuilder, integerKeyCreator);
        for (IntegerKey key : ifNameMap.keySet()) {
            if (ifNameMap.get(key).getValue().equals(ifName)) {
                return key.getInt();
            }
        }
        log.debug("Juniper_Junos_Discovery No such ifName in MIB = " + ifName);
        return 0;
    }

    public IanaIfType getIfType(int ifIndex) throws AbortedException, IOException {
        try {
            return IanaIfType.valueOf(SnmpUtil.getInteger(snmp, InterfaceMib.ifType + "." + ifIndex));
        } catch (SnmpResponseException e) {
            throw new IOException();
        } catch (NoSuchMibException e) {
            throw new IOException("No such ifIndex '" + ifIndex + "'");
        }
    }
}