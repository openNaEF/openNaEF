package voss.discovery.agent.alcatel.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpHelper.*;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IntSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.LspHopSeries;
import voss.model.MplsVlanDevice;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

import static voss.discovery.iolib.snmp.SnmpHelper.*;

public class MplsTeMibImpl implements MplsTeMib {
    private static final Logger log = LoggerFactory.getLogger(MplsTeMibImpl.class);
    private final SnmpAccess snmp;
    public MplsTeMibImpl(SnmpAccess snmp, MplsVlanDevice device) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
    }

    public LspHopSeries getMplsTunnelHop(String tunnelKey) throws IOException, AbortedException {

        Map<String, IntSnmpEntry> mplsTunnelHopTableIndexMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelHopTableIndex, intEntryBuilder, stringKeyCreator);

        int index = mplsTunnelHopTableIndexMap.get(tunnelKey).intValue();

        Map<IntegerKey, IntSnmpEntry> mplsTunnelHopAddrTypeMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelHopAddrType + "." + index, intEntryBuilder, integerKeyCreator);
        Map<IntegerKey, IpAddressSnmpEntry> mplsTunnelHopIpv4AddrMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelHopIpv4Addr + "." + index, ipAddressEntryBuilder, integerKeyCreator);

        TreeMap<Integer, String> hopTreeMap = new TreeMap<Integer, String>();

        for (IntegerKey key : mplsTunnelHopAddrTypeMap.keySet()) {
            int priority = key.getInt();
            String hop = mplsTunnelHopIpv4AddrMap.get(key).getIpAddress();
            hopTreeMap.put(priority, hop);
        }

        LspHopSeries hops = new LspHopSeries();
        for (int key : hopTreeMap.keySet()) {
            String hop;
            hop = hopTreeMap.get(key);
            hops.addHop(hop);
        }

        return hops;
    }

    public LspHopSeries lspHopSeries(int index) throws IOException, AbortedException {

        Map<String, IntSnmpEntry> mplsTunnelARHopAddrTypeMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelARHopAddrType + "." + index, intEntryBuilder, stringKeyCreator);
        Map<String, IpAddressSnmpEntry> mplsTunnelARHopIpv4AddrMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelARHopIpv4Addr + "." + index, ipAddressEntryBuilder, stringKeyCreator);
        Map<String, IpAddressSnmpEntry> mplsTunnelARHopIpv6AddrMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelARHopIpv6Addr + "." + index, ipAddressEntryBuilder, stringKeyCreator);
        Map<String, IntSnmpEntry> mplsTunnelARHopAsNumberMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelARHopAsNumber + "." + index, intEntryBuilder, stringKeyCreator);

        LspHopSeries hops = new LspHopSeries();

        for (int i = 1; i < mplsTunnelARHopAddrTypeMap.size(); i++) {

            String hop;
            switch (mplsTunnelARHopAddrTypeMap.get("." + i).intValue()) {
                case 1:
                    hop = mplsTunnelARHopIpv4AddrMap.get("." + i).getIpAddress();
                    break;
                case 2:
                    hop = mplsTunnelARHopIpv6AddrMap.get("." + i).getIpAddress();
                    break;
                case 3:
                    hop = Integer.toString(mplsTunnelARHopAsNumberMap.get("." + i).intValue());
                    break;
                default:
                    throw new IOException("" + mplsTunnelARHopAddrTypeMap.get("." + i).intValue());
            }
            hops.addHop(hop);
        }

        return hops;
    }

    public String getTunnelName(String key) throws IOException, AbortedException {

        Map<String, StringSnmpEntry> mplsTunnelNameMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelName, stringEntryBuilder, stringKeyCreator);

        if (!mplsTunnelNameMap.containsKey(key)) {
            log.debug("mplsTunnelName: not found: " + key);
            return null;
        }
        return mplsTunnelNameMap.get(key).getValue();
    }

    public int getTunnelInstancePriority(String key) throws IOException, AbortedException {

        Map<String, IntSnmpEntry> mplsTunnelInstancePriorityMap =
                SnmpUtil.getWalkResult(snmp, mplsTunnelInstancePriority, intEntryBuilder, stringKeyCreator);

        return mplsTunnelInstancePriorityMap.get(key).intValue();
    }

    private final KeyCreator<String> stringKeyCreator =
            new KeyCreator<String>() {
                private static final long serialVersionUID = 1L;

                public String getKey(BigInteger[] oidSuffix) {
                    String key = "";
                    for (BigInteger bi : oidSuffix) {
                        key += "." + bi.toString();
                    }
                    return key;
                }
            };
}