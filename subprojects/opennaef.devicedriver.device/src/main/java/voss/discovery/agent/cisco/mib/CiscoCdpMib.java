package voss.discovery.agent.cisco.mib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.discovery.iolib.snmp.SnmpUtil;
import voss.discovery.iolib.snmp.SnmpUtil.IpAddressSnmpEntry;
import voss.discovery.iolib.snmp.SnmpUtil.KeyCreator;
import voss.discovery.iolib.snmp.SnmpUtil.StringSnmpEntry;
import voss.model.Port;
import voss.model.VlanDevice;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static voss.discovery.iolib.snmp.SnmpHelper.ipAddressEntryBuilder;
import static voss.discovery.iolib.snmp.SnmpHelper.stringEntryBuilder;

public class CiscoCdpMib {
    public static final String cdpCacheAddress = ".1.3.6.1.4.1.9.9.23.1.2.1.1.4";
    public static final String SYMBOL_cdpCacheAddress = "enterprises.cisco.ciscoMgmt.ciscoCdpMIB.ciscoCdpMIBObjects.cdpCache"
            + ".cdpCacheTable.cdpCacheEntry.cdpCacheAddress";

    public static final String cdpCacheDevicePort = ".1.3.6.1.4.1.9.9.23.1.2.1.1.7";
    public static final String SYMBOL_cdpCacheDevicePort = "enterprises.cisco.ciscoMgmt.ciscoCdpMIB.ciscoCdpMIBObjects.cdpCache"
            + ".cdpCacheTable.cdpCacheEntry.cdpCacheDevicePort";

    private final static Logger log = LoggerFactory.getLogger(CiscoCdpMib.class);
    private final SnmpAccess snmp;
    private final VlanDevice device;

    public CiscoCdpMib(SnmpAccess snmp, VlanDevice device) {
        if (snmp == null) {
            throw new IllegalArgumentException();
        }
        if (device == null) {
            throw new IllegalArgumentException();
        }
        this.snmp = snmp;
        this.device = device;
    }

    public void get() throws IOException, AbortedException {
        Map<CdpCacheTableKey, IpAddressSnmpEntry> cdpCacheAddresses =
                SnmpUtil.getWalkResult(snmp, cdpCacheAddress,
                        ipAddressEntryBuilder, cdpCacheTableKeyCreator);
        Map<CdpCacheTableKey, StringSnmpEntry> cdpCacheDevicePorts =
                SnmpUtil.getWalkResult(snmp, cdpCacheDevicePort,
                        stringEntryBuilder, cdpCacheTableKeyCreator);
        for (CdpCacheTableKey key : cdpCacheAddresses.keySet()) {
            int localIfIndex = key.getLocalIfIndex();
            String neighborIp = cdpCacheAddresses.get(key).getIpAddress();
            String neighborPortIfName = cdpCacheDevicePorts.get(key).getValue();

            Port local = this.device.getPortByIfIndex(localIfIndex);

            log.debug("local=" + local.getIfName() + ", neighborId="
                    + neighborIp + ", neighborIfName=" + neighborPortIfName);
        }
    }

    private final static class CdpCacheTableKey {
        private final BigInteger localIfIndex;
        private final BigInteger deviceIndex;

        public CdpCacheTableKey(BigInteger[] oidSuffix) {
            if (oidSuffix == null || oidSuffix.length != 2) {
                throw new IllegalArgumentException();
            }
            this.localIfIndex = oidSuffix[0];
            this.deviceIndex = oidSuffix[1];
        }

        public int getLocalIfIndex() {
            return this.localIfIndex.intValue();
        }

        public int getDeviceIfIndex() {
            return this.deviceIndex.intValue();
        }

        public boolean equals(Object o) {
            if (o != null && o instanceof CdpCacheTableKey) {
                return this.hashCode() == ((CdpCacheTableKey) o).hashCode();
            }
            return false;
        }

        public int hashCode() {
            int i = localIfIndex.intValue() * deviceIndex.intValue();
            return i * i + i + 41;
        }

        public String toString() {
            String key = this.getClass().getSimpleName() + ":"
                    + localIfIndex.intValue() + ":" + deviceIndex.intValue();
            return key;
        }
    }

    @SuppressWarnings("serial")
    private final static KeyCreator<CdpCacheTableKey> cdpCacheTableKeyCreator
            = new KeyCreator<CdpCacheTableKey>() {
        public CdpCacheTableKey getKey(BigInteger[] oidSuffix) {
            return new CdpCacheTableKey(oidSuffix);
        }

    };
}