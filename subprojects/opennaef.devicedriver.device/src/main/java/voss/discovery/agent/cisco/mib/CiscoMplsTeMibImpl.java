package voss.discovery.agent.cisco.mib;

import net.snmp.RepeatedOidException;
import net.snmp.SnmpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.agent.mib.MplsTunnelHopAddrType;
import voss.discovery.agent.mib.OperStatus;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static voss.discovery.agent.cisco.mib.CiscoMplsTeMib.*;

public class CiscoMplsTeMibImpl {
    private final static Logger log = LoggerFactory.getLogger(CiscoMplsTeMibImpl.class);

    private final SnmpAccess snmp;
    private final MplsVlanDevice device;

    public CiscoMplsTeMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        this.snmp = snmp;
        this.device = device;
    }

    @SuppressWarnings("serial")
    private final static SnmpUtil.KeyCreator<MplsTunnelKey> mplsTunnelKeyCreator =
            new SnmpUtil.KeyCreator<MplsTunnelKey>() {
                public MplsTunnelKey getKey(BigInteger[] oidSuffix) {
                    return new MplsTunnelKey(oidSuffix);
                }
            };

    private final static class HopTableKey {
        public final BigInteger index;
        public final BigInteger hopcount;

        public HopTableKey(BigInteger[] key) {
            this.index = key[0];
            this.hopcount = key[1];
        }

        @Override
        public int hashCode() {
            int seed = this.toString().hashCode();
            return seed * seed + seed + 41;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof HopTableKey) {
                return this.hashCode() == ((HopTableKey) o).hashCode();
            }
            return false;
        }

        @Override
        public String toString() {
            String key = "HopTableKey:" + this.index.toString() + ":" + this.hopcount.toString();
            return key;
        }
    }

    @SuppressWarnings("serial")
    private final static SnmpUtil.KeyCreator<HopTableKey> hopTableKeyCreator =
            new SnmpUtil.KeyCreator<HopTableKey>() {
                public HopTableKey getKey(BigInteger[] oidSuffix) {
                    return new HopTableKey(oidSuffix);
                }
            };


    public void getMplsTunnels() throws IOException, AbortedException {

        Map<MplsTunnelKey, MplsTunnel> tunnels = new HashMap<MplsTunnelKey, MplsTunnel>();
        try {
            List<SnmpUtil.ByteSnmpEntry> entries = SnmpUtil.getByteSnmpEntries(snmp, mplsTunnelSessionAttributes);
            Map<MplsTunnelKey, SnmpEntry> tunnelNames = SnmpUtil.getWalkResult(snmp, mplsTunnelName, mplsTunnelKeyCreator);
            Map<MplsTunnelKey, SnmpEntry> tunnelDescriptions = SnmpUtil.getWalkResult(snmp, mplsTunnelDescr, mplsTunnelKeyCreator);
            Map<MplsTunnelKey, SnmpEntry> tunnelIsIfs = SnmpUtil.getWalkResult(snmp, mplsTunnelIsIf, mplsTunnelKeyCreator);
            Map<MplsTunnelKey, SnmpEntry> tunnelIfIndices = SnmpUtil.getWalkResult(snmp, mplsTunnelIfIndex, mplsTunnelKeyCreator);
            Map<MplsTunnelKey, SnmpEntry> arHopIndices = SnmpUtil.getWalkResult(snmp, mplsTunnelARHopTableIndex, mplsTunnelKeyCreator);
            Map<MplsTunnelKey, SnmpEntry> cHopIndices = SnmpUtil.getWalkResult(snmp, mplsTunnelCHopTableIndex, mplsTunnelKeyCreator);
            Map<MplsTunnelKey, SnmpEntry> tunnelOperStatuses = SnmpUtil.getWalkResult(snmp, mplsTunnelOperStatus, mplsTunnelKeyCreator);

            Map<BigInteger, MplsTunnelKey> arHopIndexToKey = new HashMap<BigInteger, MplsTunnelKey>();
            Map<BigInteger, MplsTunnelKey> cHopIndexToKey = new HashMap<BigInteger, MplsTunnelKey>();
            Map<MplsTunnelKey, BigInteger> keyToARHopIndex = new HashMap<MplsTunnelKey, BigInteger>();
            Map<MplsTunnelKey, BigInteger> keyToCHopIndex = new HashMap<MplsTunnelKey, BigInteger>();

            for (SnmpUtil.ByteSnmpEntry entry : entries) {
                MplsTunnelKey key = new MplsTunnelKey(entry.oidSuffix);
                log.trace("tunnel-key=" + key.getKey());
                if (tunnels.containsKey(key)) {
                    throw new IllegalStateException("duplicated key: " + key.toString());
                }

                if (arHopIndices.get(key) != null) {
                    arHopIndexToKey.put(arHopIndices.get(key).getValueAsBigInteger(), key);
                    keyToARHopIndex.put(key, arHopIndices.get(key).getValueAsBigInteger());
                }

                if (cHopIndices.get(key) != null) {
                    cHopIndexToKey.put(cHopIndices.get(key).getValueAsBigInteger(), key);
                    keyToCHopIndex.put(key, cHopIndices.get(key).getValueAsBigInteger());
                }
                StringSnmpEntry mplsTunnelNameEntry = StringSnmpEntry.getInstance(tunnelNames.get(key));
                StringSnmpEntry mplsTunnelDescrEntry = StringSnmpEntry.getInstance(tunnelDescriptions.get(key));
                IntSnmpEntry mplsTunnelIsIf = IntSnmpEntry.getInstance(tunnelIsIfs.get(key));
                IntSnmpEntry mplsTunnelIfIndex = IntSnmpEntry.getInstance(tunnelIfIndices.get(key));
                IntSnmpEntry mplsOperStatus = IntSnmpEntry.getInstance(tunnelOperStatuses.get(key));

                MplsTunnel tunnel = new MplsTunnel();
                tunnel.initDevice(device);
                if (mplsTunnelIsIf != null && mplsTunnelIfIndex != null && mplsTunnelIsIf.intValue() == 1) {
                    tunnel.initIfIndex(mplsTunnelIfIndex.intValue());
                }
                if (mplsTunnelNameEntry != null) {
                    String ifName = mplsTunnelNameEntry.getValue();
                    if (ifName != null && ifName.length() > 0) {
                        tunnel.initIfName(ifName);
                    }
                }
                if (mplsTunnelDescrEntry != null) {
                    tunnel.setIfDescr(mplsTunnelDescrEntry.getValue());
                    tunnel.setUserDescription(mplsTunnelDescrEntry.getValue());
                }
                if (mplsOperStatus != null) {
                    int statusValue = mplsOperStatus.intValue();
                    OperStatus status = OperStatus.getByMibValue(statusValue);
                    tunnel.setOperationalStatus(status.getCaption());
                }
                tunnel.setTunnelKey(key);
                tunnels.put(key, tunnel);

                byte[] bytes = entry.getValue();
                int[] flags = SnmpUtil.decodeBitList(bytes);
                for (int flag : flags) {
                    switch (flag - 1) {
                        case tunnelSessionAttribute_fastReroute:
                            tunnel.setFastReroute(true);
                            break;
                        case tunnelSessionAttribute_mergePermitted:
                            tunnel.setMergePermitted(true);
                            break;
                        case tunnelSessionAttribute_isPersistent:
                            tunnel.setPersistent(true);
                            break;
                        case tunnelSessionAttribute_isPinned:
                            tunnel.setPinned(true);
                            break;
                        case tunnelSessionAttribute_recordRoute:
                            tunnel.setRecordRoute(true);
                            break;
                        default:
                            throw new IllegalStateException("unknown attribute value: " + flag);
                    }
                }

            }

            Map<HopTableKey, SnmpEntry> arHopAddrType = SnmpUtil.getWalkResult(snmp, mplsTunnelARHopAddrType, hopTableKeyCreator);
            Map<HopTableKey, SnmpEntry> arHopIPv4Addr = SnmpUtil.getWalkResult(snmp, mplsTunnelARHopIpv4Addr, hopTableKeyCreator);
            Map<HopTableKey, SnmpEntry> arHopIpv6Addr = SnmpUtil.getWalkResult(snmp, mplsTunnelARHopIpv6Addr, hopTableKeyCreator);

            for (MplsTunnelKey key : tunnels.keySet()) {
                log.trace("actual route target: " + key);
                LspHopSeries myHops = new LspHopSeries();
                LabelSwitchedPathEndPoint path = new LabelSwitchedPathEndPoint();
                path.initDevice(device);
                path.setLspName(key.getKey());

                BigInteger arHopIndex = keyToARHopIndex.get(key);
                Map<HopTableKey, SnmpEntry> filteredAddrType = filterHopEntry(arHopIndex, arHopAddrType);
                List<HopTableKey> sortedKeys = sortKey(filteredAddrType.keySet());
                for (HopTableKey sortedKey : sortedKeys) {
                    log.trace("actual route hop key: " + sortedKey);
                    IntSnmpEntry addrTypeEntry = IntSnmpEntry.getInstance(arHopAddrType.get(sortedKey));
                    MplsTunnelHopAddrType type = MplsTunnelHopAddrType.getById(addrTypeEntry.intValue());
                    String nexthop = null;
                    switch (type) {
                        case ipv4:
                            nexthop = SnmpUtil.getIpAddressByHexaBytes(arHopIPv4Addr.get(sortedKey).value);
                            myHops.addHop(nexthop);
                            break;
                        case ipv6:
                            nexthop = SnmpUtil.getIpAddressByHexaBytes(arHopIpv6Addr.get(sortedKey).value);
                            myHops.addHop(nexthop);
                            break;
                        default:
                            log.warn("unexpected value: "
                                    + addrTypeEntry.getVarBind().getOid().getOidString()
                                    + "->" + addrTypeEntry.intValue());
                            break;
                    }
                    log.trace("actual route hop found: " + sortedKey + " " + nexthop);
                }

                MplsTunnel tunnel = tunnels.get(key);
                tunnel.putLspHopSeries(key.getTunnelInstance(), myHops);
                tunnel.addMemberLsp(0, path);
                log.trace("actual route result: " + key.toString() + "=>" + myHops.toString());
            }

            device.addTeTunnels(tunnels.values());

        } catch (RepeatedOidException e) {
            throw new IOException(e);
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        }
    }

    private Map<HopTableKey, SnmpEntry> filterHopEntry(BigInteger index, Map<HopTableKey, SnmpEntry> entries)
            throws IOException, AbortedException {
        Map<HopTableKey, SnmpEntry> result = new HashMap<HopTableKey, SnmpEntry>();
        for (Map.Entry<HopTableKey, SnmpEntry> entry : entries.entrySet()) {
            if (entry.getKey().index.equals(index)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private List<HopTableKey> sortKey(Set<HopTableKey> keyset) throws IOException, AbortedException {
        List<HopTableKey> result = new ArrayList<HopTableKey>();
        result.addAll(keyset);
        Comparator<HopTableKey> sorter = new Comparator<HopTableKey>() {
            public int compare(HopTableKey b1, HopTableKey b2) {
                if (b1 == null || b2 == null) {
                    throw new IllegalArgumentException();
                }
                if (b1.index.intValue() != b2.index.intValue()) {
                    return b1.index.intValue() - b2.index.intValue();
                } else if (b1.hopcount.intValue() != b2.hopcount.intValue()) {
                    return b1.hopcount.intValue() - b2.hopcount.intValue();
                }
                return 0;
            }
        };
        Collections.sort(result, sorter);
        return result;
    }
}