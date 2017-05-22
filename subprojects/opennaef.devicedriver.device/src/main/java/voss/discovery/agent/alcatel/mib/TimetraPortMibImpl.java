package voss.discovery.agent.alcatel.mib;

import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.alcatel.Alcatel7710SRDiscovery;
import voss.discovery.agent.common.IanaIfType;
import voss.discovery.agent.mib.InterfaceMibImpl;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.NoSuchMibException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.ByteSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class TimetraPortMibImpl extends InterfaceMibImpl implements TimetraPortMib {

    private static final Logger log = LoggerFactory.getLogger(TimetraPortMibImpl.class);
    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public TimetraPortMibImpl(Alcatel7710SRDiscovery discovery) {
        super(discovery.getSnmpAccess());

        this.snmp = discovery.getSnmpAccess();
        this.device = (MplsVlanDevice) discovery.getDeviceInner();

    }

    public int getPortState(int portId) throws IOException, AbortedException {
        final Map<IntegerKey, IntSnmpEntry> tmnxPortStates =
                SnmpUtil.getWalkResult(snmp, tmnxPortState + ".1", intEntryBuilder, integerKeyCreator);

        return tmnxPortStates.get(new IntegerKey(new BigInteger[]{BigInteger.valueOf(portId)})).intValue();
    }

    public boolean isPhysicalPort(int portId) {
        return (portId & 0xe0000000) == 0x00000000;
    }

    public boolean isChannel(int portId) {
        return (portId & 0xe0000000) == 0x20000000;
    }

    public void createPhysicalPorts() throws IOException, AbortedException {

        Map<IntegerKey, IntSnmpEntry> tmnxPortStateMap =
                SnmpUtil.getWalkResult(snmp, tmnxPortState + ".1", intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IntSnmpEntry> tmnxPortConnectorTypeMap =
                SnmpUtil.getWalkResult(snmp, tmnxPortConnectorType + ".1", intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, StringSnmpEntry> tmnxPortConnectTypeNameMap =
                SnmpUtil.getWalkResult(snmp, tmnxPortConnectTypeName, stringEntryBuilder, integerKeyCreator);

        for (IntegerKey key : tmnxPortStateMap.keySet()) {

            int ifindex = key.getInt();
            if (!isPhysicalPort(ifindex)) continue;
            int slotIndex = (ifindex & 0x1e000000) / 0x02000000;
            int mdaIndex = (ifindex & 0x01e00000) / 0x00200000;
            int portIndex = (ifindex & 0x001f8000) / 0x00008000;

            TmnxPortState portState = TmnxPortState.get(tmnxPortStateMap.get(key).intValue());
            switch (portState) {
                case none:
                case ghost:
                    log.debug("ifindex=" + ifindex + " (" + slotIndex + "/" + mdaIndex + "/" + portIndex
                            + ") is not physically present.");
                    continue;
                case linkDown:
                case linkUp:
                case up:
                case diagnose:
                    break;
                default:
                    throw new IllegalStateException();
            }

            IanaIfType ianaType = getIfType(ifindex);

            try {

                PhysicalPort port = getPhysicalPort(ianaType);
                if (slotIndex > 0) {
                    Module module = device.getSlotBySlotIndex(slotIndex).getModule();
                    if (mdaIndex > 0) {
                        module = module.getSlotBySlotIndex(mdaIndex).getModule();
                    }

                    port.initModule(module);
                } else {
                    port.initDevice(device);
                }
                port.initIfIndex(ifindex);
                port.setPortTypeName(ianaType.toString());

                int connectorType = tmnxPortConnectorTypeMap.get(key).intValue();
                StringSnmpEntry connectTypeNameEntry =
                        tmnxPortConnectTypeNameMap.get(new IntegerKey(new BigInteger[]{BigInteger.valueOf(connectorType)}));
                if (connectTypeNameEntry != null) {
                    port.setConnectorTypeName(connectTypeNameEntry.getValue());
                }

                log.debug("@ add port " + ifindex + " type " + port.getClass().getSimpleName()
                        + " (" + ianaType.toString() + ")"
                        + " to device='" + device.getDeviceName() + "'");

            } catch (UnknownIfTypeException e) {
                log.warn("unknown if type: ifindex=" + ifindex + ";" + e.getMessage());
                continue;
            }
        }
    }

    public void createLogicalPorts() throws IOException, AbortedException {

        Map<IntegerKey, IntSnmpEntry> tmnxPortStateMap =
                SnmpUtil.getWalkResult(snmp, tmnxPortState + ".1", intEntryBuilder, integerKeyCreator);

        for (IntegerKey key : tmnxPortStateMap.keySet()) {

            int ifIndex = key.getInt();
            if ((ifIndex & 0xf0000000) == 0x50000000) continue;
            if ((ifIndex & 0xe0000000) == 0x00000000) continue;
            int slotIndex = (ifIndex & 0x1e000000) / 0x02000000;
            int mdaIndex = (ifIndex & 0x01e00000) / 0x00200000;
            int portIndex = (ifIndex & 0x001f8000) / 0x00008000;

            TmnxPortState portState = TmnxPortState.get(tmnxPortStateMap.get(key).intValue());
            switch (portState) {
                case none:
                case ghost:
                    log.debug("ifindex=" + ifIndex + " (" + slotIndex + "/" + mdaIndex + "/" + portIndex
                            + ") is not physically present.");
                    continue;
                case linkDown:
                case linkUp:
                case up:
                case diagnose:
                    break;
                default:
                    throw new IllegalStateException();
            }

            IanaIfType ianaType = getIfType(ifIndex);
            String portName = getPortName(ifIndex);

            int physicalIfIndex = slotIndex * 0x02000000 + mdaIndex * 0x00200000 + portIndex * 0x00008000;

            if (ianaType == IanaIfType.sonetPath ||
                    ianaType == IanaIfType.sonetVT) {
                SonetPathImpl sonetPath = new SonetPathImpl();
                sonetPath.initDevice(device);
                sonetPath.initIfIndex(ifIndex);
                sonetPath.initIfName(portName);
            } else if (ianaType == IanaIfType.ds0Bundle) {
                Channel channel = new ChannelImpl();
                channel.initDevice(device);
                channel.initIfIndex(ifIndex);
                channel.initIfName(portName);

                String channelGroupId = portName.replaceAll(".*\\.", "");
                channel.setChannelGroupId(channelGroupId);
                setTimeslot(channel, ifIndex);

                POS pos = (POS) device.getPortByIfIndex(physicalIfIndex);
                ChannelFeatureImpl feature = (ChannelFeatureImpl) pos.getLogicalFeature();
                if (feature == null) {
                    feature = createChannelFeature(pos.getIfName(), ifIndex);
                    pos.setLogicalFeature(feature);
                }
                feature.addChannel(channel);
            } else {
                log.debug("Skip port " + ifIndex + " " + getPortName(ifIndex) + " (" + ianaType.toString() + ")");
            }
        }

        for (IntegerKey key : tmnxPortStateMap.keySet()) {

            int ifIndex = key.getInt();
            if ((ifIndex & 0xf0000000) != 0x50000000) continue;
            IanaIfType ianaType = getIfType(ifIndex);
            String portName = getPortName(ifIndex);

            int lowestIfIndex = getLowestIfIndex(ifIndex);

            if (ianaType == IanaIfType.sonetPath ||
                    ianaType == IanaIfType.sonetVT) {
                SonetPathImpl sonetPath = new SonetPathImpl();
                sonetPath.initDevice(device);
                sonetPath.initIfIndex(ifIndex);
                sonetPath.initIfName(portName);
            } else if (ianaType == IanaIfType.ds0Bundle) {
                Channel channel = new ChannelImpl();
                channel.initDevice(device);
                channel.initIfIndex(ifIndex);
                channel.initIfName(portName);

                String channelGroupId = portName.replaceAll(".*\\.", "");
                channel.setChannelGroupId(channelGroupId);
                setTimeslot(channel, ifIndex);

                POSAPSImpl aps = (POSAPSImpl) device.getPortByIfIndex(lowestIfIndex);
                ChannelFeatureImpl feature = (ChannelFeatureImpl) aps.getLogicalFeature();
                if (feature == null) {
                    feature = createChannelFeature(aps.getIfName(), ifIndex);
                    aps.setLogicalFeature(feature);
                }
                feature.addChannel(channel);
            } else {
                log.debug("Skip port " + ifIndex + " " + getPortName(ifIndex) + " (" + ianaType.toString() + ")");
            }
        }
    }

    private int getLowestIfIndex(int ifIndex) throws IOException, AbortedException {

        Map<OidKey, IntSnmpEntry> ifStackStatusMap =
                SnmpUtil.getWalkResult(snmp, ifStackStatus_OID, intEntryBuilder, oidKeyCreator);

        int lower = 0;

        do {
            boolean found = false;
            for (OidKey oidKey : ifStackStatusMap.keySet()) {
                if (oidKey.getInt(0) == ifIndex) {
                    lower = oidKey.getInt(1);
                    if (lower != 0) {
                        ifIndex = lower;
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException();
            }
        }
        while (lower != 0);

        return ifIndex;
    }

    private void setTimeslot(Channel channel, int ifindex) throws IOException, AbortedException {

        Map<IntegerKey, ByteSnmpEntry> tmnxDS0ChanGroupTimeSlotsMap =
                SnmpUtil.getWalkResult(snmp, tmnxDS0ChanGroupTimeSlots + ".1", byteEntryBuilder, integerKeyCreator);

        IntegerKey key = new IntegerKey(new BigInteger[]{BigInteger.valueOf(ifindex)});
        int[] bits = SnmpUtil.decodeBitList(tmnxDS0ChanGroupTimeSlotsMap.get(key).value);
        List<Integer> timeslot = new ArrayList<Integer>();
        for (int bit : bits) {
            timeslot.add(bit);
        }
        channel.setTimeslot(timeslot);
    }

    private ChannelFeatureImpl createChannelFeature(String ifName, int ifindex) throws IOException, AbortedException {

        Map<IntegerKey, IntSnmpEntry> tmnxDS0ChanGroupSpeedMap =
                SnmpUtil.getWalkResult(snmp, tmnxDS0ChanGroupSpeed + ".1", intEntryBuilder, integerKeyCreator);

        IntegerKey key = new IntegerKey(new BigInteger[]{BigInteger.valueOf(ifindex)});

        ChannelFeatureImpl feature = new ChannelFeatureImpl();
        feature.initDevice(device);
        feature.initIfName("[feature]" + ifName);
        TmnxDS0ChanGroupSpeed groupSpeed = TmnxDS0ChanGroupSpeed.get(tmnxDS0ChanGroupSpeedMap.get(key).intValue());
        feature.setSlotUnit(groupSpeed.getSpeed());

        return feature;
    }

    public int getIfIndex(String portName) throws IOException, AbortedException {
        Map<IntegerKey, StringSnmpEntry> ifNameMap =
                SnmpUtil.getWalkResult(snmp, ifName, stringEntryBuilder, integerKeyCreator);
        for (IntegerKey key : ifNameMap.keySet()) {
            if (ifNameMap.get(key).getValue().equals(portName)) {
                return key.getInt();
            }
        }
        return 0;
    }

    public String getPortName(int ifIndex) throws AbortedException, IOException {

        try {
            return SnmpUtil.getString(snmp, tmnxPortName + ".1." + ifIndex);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public int getPortParentPortId(int ifIndex) throws AbortedException, IOException {

        try {
            return SnmpUtil.getInteger(snmp, tmnxPortParentPortID + ".1." + ifIndex);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }

    public TmnxPortEncapType getPortEncapType(int ifIndex) throws AbortedException, IOException {
        try {
            return TmnxPortEncapType.get(SnmpUtil.getInteger(snmp, tmnxPortEncapType + ".1." + ifIndex));
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (NoSuchMibException e) {
            throw new IOException(e);
        }
    }
}