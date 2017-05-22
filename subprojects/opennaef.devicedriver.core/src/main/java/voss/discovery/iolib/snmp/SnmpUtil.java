package voss.discovery.iolib.snmp;

import net.snmp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.discovery.iolib.AbortedException;
import voss.discovery.utils.ByteArrayUtil;
import voss.util.VossMiscUtility;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SnmpUtil {
    private final static Logger log = LoggerFactory.getLogger(SnmpUtil.class);

    public static SnmpEntry getEntry(SnmpAccess snmp, String oid)
            throws SocketTimeoutException, SocketException, AbortedException,
            IOException, SnmpResponseException, NoSuchMibException {
        VarBind varbind = snmp.get(oid);
        if (varbind == null) {
            throw new NoSuchMibException("no such mib: " + oid);
        }
        return new SnmpEntry(oid, varbind);
    }

    public static String getString(SnmpAccess snmp, String oid)
            throws SocketTimeoutException, SocketException, AbortedException,
            IOException, SnmpResponseException, NoSuchMibException {
        VarBind varbind = snmp.get(oid);
        if (varbind == null) {
            throw new NoSuchMibException("no such mib : " + oid);
        }
        StringSnmpEntry result = new StringSnmpEntry(oid, varbind);
        return result.getValue();
    }

    public static String getString(SnmpAccess snmp, String oid, String targetName) {
        try {
            return getString(snmp, oid);
        } catch (Exception e) {
            if (targetName == null) {
                targetName = oid;
            }
            log.warn("Cannot get " + targetName, e);
            return null;
        }
    }

    public static int getInteger(SnmpAccess snmp, String oid)
            throws SocketTimeoutException, SocketException, AbortedException,
            IOException, SnmpResponseException, NoSuchMibException {
        VarBind varbind = snmp.get(oid);
        if (varbind == null) {
            throw new NoSuchMibException("no such mib : " + oid);
        }
        IntSnmpEntry result = new IntSnmpEntry(oid, varbind);
        return result.intValue();
    }

    public static long getLong(SnmpAccess snmp, String oid)
            throws SocketTimeoutException, SocketException, AbortedException,
            IOException, SnmpResponseException, NoSuchMibException {
        VarBind varbind = snmp.get(oid);
        if (varbind == null) {
            throw new NoSuchMibException("no such mib : " + oid);
        }
        IntSnmpEntry result = new IntSnmpEntry(oid, varbind);
        return result.longValue();
    }

    public static byte[] getByte(SnmpAccess snmp, String oid)
            throws SocketTimeoutException, SocketException, AbortedException,
            IOException, SnmpResponseException, NoSuchMibException {
        VarBind varbind = snmp.get(oid);
        if (varbind == null) {
            throw new NoSuchMibException("no such mib : " + oid);
        }
        ByteSnmpEntry result = new ByteSnmpEntry(oid, varbind);
        return result.getValue();
    }

    public static String getIpAddress(SnmpAccess snmp, String oid)
            throws SocketTimeoutException, SocketException, AbortedException,
            IOException, SnmpResponseException, NoSuchMibException {
        VarBind varbind = snmp.get(oid);
        if (varbind == null) {
            throw new NoSuchMibException("no such mib : " + oid);
        }
        IpAddressSnmpEntry result = new IpAddressSnmpEntry(oid, varbind);
        return result.getIpAddress();
    }

    public static String getNextIpAddress(SnmpAccess snmp, String oid)
            throws SocketTimeoutException, SocketException, AbortedException,
            IOException, SnmpResponseException, NoSuchMibException {
        VarBind varbind = snmp.getNextChild(oid);
        if (varbind == null) {
            throw new NoSuchMibException("no such mib : " + oid);
        }
        IpAddressSnmpEntry result = new IpAddressSnmpEntry(oid, varbind);
        return result.getIpAddress();
    }

    public static List<ByteSnmpEntry> getByteSnmpEntries(SnmpAccess snmp,
                                                         final String oid) throws SocketTimeoutException, SocketException,
            AbortedException, IOException, RepeatedOidException,
            SnmpResponseException {
        List<ByteSnmpEntry> result = new ArrayList<ByteSnmpEntry>();
        AbstractWalkProcessor<List<ByteSnmpEntry>> walker =
                new AbstractWalkProcessor<List<ByteSnmpEntry>>(result) {
                    private static final long serialVersionUID = 1L;

                    public void process(VarBind varbind) {
                        result.add(new ByteSnmpEntry(oid, varbind));
                    }
                };
        snmp.walk(oid, walker);
        return walker.getResult();
    }

    public static List<StringSnmpEntry> getStringSnmpEntries(SnmpAccess snmp,
                                                             final String oid) throws SocketTimeoutException, SocketException,
            AbortedException, IOException, RepeatedOidException,
            SnmpResponseException {
        final List<StringSnmpEntry> result = new ArrayList<StringSnmpEntry>();
        AbstractWalkProcessor<List<StringSnmpEntry>> walker =
                new AbstractWalkProcessor<List<StringSnmpEntry>>(result) {
                    private static final long serialVersionUID = 1L;

                    public void process(VarBind varbind) {
                        result.add(new StringSnmpEntry(oid, varbind));
                    }
                };
        snmp.walk(oid, walker);
        return walker.getResult();
    }

    public static List<IntSnmpEntry> getIntSnmpEntries(SnmpAccess snmp,
                                                       final String oid) throws SocketTimeoutException, SocketException,
            AbortedException, IOException, RepeatedOidException,
            SnmpResponseException {
        List<IntSnmpEntry> result = new ArrayList<IntSnmpEntry>();
        AbstractWalkProcessor<List<IntSnmpEntry>> walker =
                new AbstractWalkProcessor<List<IntSnmpEntry>>(result) {
                    private static final long serialVersionUID = 1L;

                    public void process(VarBind varbind) {
                        result.add(new IntSnmpEntry(oid, varbind));
                    }
                };
        snmp.walk(oid, walker);
        return walker.getResult();
    }

    public static List<SnmpEntry> getSnmpEntries(SnmpAccess snmp,
                                                 final String oid) throws SocketTimeoutException, SocketException,
            AbortedException, IOException, RepeatedOidException,
            SnmpResponseException {
        List<SnmpEntry> result = new ArrayList<SnmpEntry>();
        AbstractWalkProcessor<List<SnmpEntry>> walker =
                new AbstractWalkProcessor<List<SnmpEntry>>(result) {
                    private static final long serialVersionUID = 1L;

                    public void process(VarBind varbind) {
                        result.add(new SnmpEntry(oid, varbind));
                    }
                };
        snmp.walk(oid, walker);
        return walker.getResult();
    }

    public static List<SnmpEntry> getMultipleSnmpEntries(SnmpAccess snmp,
                                                         final List<String> oids) throws SocketTimeoutException, SocketException,
            AbortedException, IOException, SnmpResponseException, NoSuchMibException {
        String[] oidarray = oids.toArray(new String[oids.size()]);
        Map<String, VarBind> gots = snmp.multiGet(oidarray);
        List<SnmpEntry> result = new ArrayList<SnmpEntry>();
        for (Map.Entry<String, VarBind> got : gots.entrySet()) {
            String oid = got.getKey();
            VarBind vb = got.getValue();
            PDU.Generic pdu = (PDU.Generic) vb.getPdu();
            if (pdu.getErrorStatus() > 0) {
                throw new IOException("got ErrorStatus=" + pdu.getErrorStatus());
            }
            if (SnmpV2ResponseException.noSuchInstance == vb.getV2ResponseException()) {
                throw new NoSuchMibException(oid);
            }
            SnmpEntry se = new SnmpEntry(oid, vb);
            result.add(se);
        }
        return result;
    }

    public static List<IpAddressSnmpEntry> getIpAddressSnmpEntries(SnmpAccess snmp,
                                                                   final String oid) throws SocketTimeoutException, SocketException,
            AbortedException, IOException, RepeatedOidException,
            SnmpResponseException {
        List<IpAddressSnmpEntry> result = new ArrayList<IpAddressSnmpEntry>();
        AbstractWalkProcessor<List<IpAddressSnmpEntry>> walker =
                new AbstractWalkProcessor<List<IpAddressSnmpEntry>>(result) {
                    private static final long serialVersionUID = 1L;

                    public void process(VarBind varbind) {
                        result.add(new IpAddressSnmpEntry(oid, varbind));
                    }
                };
        snmp.walk(oid, walker);
        return walker.getResult();
    }

    public static List<String> getStringByWalk(SnmpAccess snmp, final String oid)
            throws SocketTimeoutException, SocketException, AbortedException,
            IOException, RepeatedOidException, SnmpResponseException {
        List<String> result = new ArrayList<String>();
        AbstractWalkProcessor<List<String>> walker =
                new AbstractWalkProcessor<List<String>>(result) {
                    private static final long serialVersionUID = 1L;

                    public void process(VarBind varbind) {
                        StringSnmpEntry entry = new StringSnmpEntry(oid, varbind);
                        result.add(entry.getValue());
                    }
                };
        snmp.walk(oid, walker);
        return walker.getResult();
    }

    public static String getOID(SnmpAccess snmp, String oid)
            throws IOException, AbortedException, SnmpResponseException {
        VarBind objectIDVarbind = snmp.get(oid);
        if (objectIDVarbind == null) {
            return null;
        }
        OidTLV objectIdOid = (OidTLV) objectIDVarbind.getValue();
        return objectIdOid.getOidString();
    }

    public static class ByteSnmpEntry extends SnmpEntry {
        private static final long serialVersionUID = 1L;

        protected ByteSnmpEntry(String rootOidString, VarBind varbind) {
            super(rootOidString, varbind);
        }

        public byte[] getValue() {
            return value;
        }

        public int getOIDSuffix(int index) throws UnexpectedVarBindException {
            if (oidSuffix.length <= index) {
                throw new UnexpectedVarBindException(varbind);
            }
            return oidSuffix[index].intValue();
        }

        public static ByteSnmpEntry getInstance(SnmpEntry entry) {
            return new ByteSnmpEntry(entry.getRootOidStr(), entry.getVarBind());
        }
    }

    public static class StringSnmpEntry extends SnmpEntry {
        private static final long serialVersionUID = 1L;

        protected StringSnmpEntry(String rootOidString, VarBind varbind) {
            super(rootOidString, varbind);
        }

        public String getValue() {
            return new String(value);
        }

        public int getOIDSuffix(int index) throws UnexpectedVarBindException {
            if (oidSuffix.length <= index) {
                throw new UnexpectedVarBindException(varbind);
            }
            return oidSuffix[index].intValue();
        }

        public static StringSnmpEntry getInstance(SnmpEntry entry) {
            return new StringSnmpEntry(entry.getRootOidStr(), entry.getVarBind());
        }
    }

    public static class IntSnmpEntry extends SnmpEntry {
        private static final long serialVersionUID = 1L;

        protected IntSnmpEntry(String rootOidString, VarBind varbind) {
            super(rootOidString, varbind);
        }

        public int intValue() {
            return getValueAsBigInteger().intValue();
        }

        public long longValue() {
            return getValueAsBigInteger().longValue();
        }

        public int getOIDSuffixLast() {
            return getLastOIDIndex().intValue();
        }

        public int getOIDSuffix(int index) throws UnexpectedVarBindException {
            if (oidSuffix.length <= index) {
                throw new UnexpectedVarBindException(varbind);
            }
            return oidSuffix[index].intValue();
        }

        public static IntSnmpEntry getInstance(SnmpEntry entry) {
            return new IntSnmpEntry(entry.getRootOidStr(), entry.getVarBind());
        }
    }

    public static class IpAddressSnmpEntry extends SnmpEntry {
        private static final long serialVersionUID = 1L;

        protected IpAddressSnmpEntry(String rootOidString, VarBind varbind) {
            super(rootOidString, varbind);
        }

        public String getIpAddress() {
            if (varbind.getValue() instanceof IpAddressTLV) {
                return ((IpAddressTLV) varbind.getValue()).getStringExpression();
            }
            throw new IllegalStateException("incompatible type: "
                    + varbind.getValue().getClass().getSimpleName());
        }

        public int getMaskLength() {
            if (varbind.getValue() instanceof IpAddressTLV) {
                byte[] bytes = varbind.getValue().getValue();
                int length = 0;
                for (byte b : bytes) {
                    length = length + Integer.bitCount(b & 0xff);
                }
                return length;
            }
            throw new IllegalStateException("incompatible type: "
                    + varbind.getValue().getClass().getSimpleName());
        }

        public static IpAddressSnmpEntry getInstance(SnmpEntry entry) {
            return new IpAddressSnmpEntry(entry.getRootOidStr(), entry.getVarBind());
        }
    }

    public static class MutableString {
        private String value;
        private boolean isSetted = false;

        public void set(String valueIn) {
            value = valueIn;
            isSetted = true;
        }

        public boolean isSetted() {
            return isSetted;
        }

        public String get() {
            if (!isSetted) {
                throw new RuntimeException();
            }
            return value;
        }
    }

    public static boolean contains(int[] list, int value) {
        for (int i = 0; i < list.length; i++) {
            if (list[i] == value) {
                return true;
            }
        }
        return false;
    }

    public static int[] decodeBitListFromBigInteger(BigInteger bits) {
        List<Integer> resultList = new ArrayList<Integer>();
        BigInteger mask = BigInteger.ONE;
        int valueCandidate = 1;
        while (mask.compareTo(bits) <= 0) {
            if ((mask.and(bits)).equals(mask)) {
                resultList.add(Integer.valueOf(valueCandidate));
            }
            valueCandidate++;
            mask = mask.shiftLeft(1);
        }

        int[] result = new int[resultList.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = resultList.get(i).intValue();
        }
        return result;
    }

    public static int[] decodeBitList(byte[] list) {
        List<Integer> ports = new ArrayList<Integer>();
        for (int i = 0; i < list.length; i++) {
            int octet = SnmpUtils.unsign(list[i]);
            for (int j = 0; j < 8; j++) {
                int bitTester = 1 << (7 - j);
                if ((octet & bitTester) != 0) {
                    ports.add(Integer.valueOf(i * 8 + j + 1));
                }
            }
        }
        int[] result = new int[ports.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ports.get(i).intValue();
        }
        return result;
    }

    public static int getMaskLength(String mask) {
        byte[] address = VossMiscUtility.getByteFormIpAddress(mask);
        int length = 0;
        for (byte b : address) {
            length = length + Integer.bitCount(b & 0xff);
        }
        return length;
    }

    public static String getPhysicalAddressString(byte[] rawPhysicalAddress) {
        String hexExpression = SnmpUtils.getHexExpression(rawPhysicalAddress);
        return hexExpression.replace(' ', ':');
    }

    public static interface KeyCreator<T> extends Serializable {
        public T getKey(BigInteger[] oidSuffix);
    }

    public static interface EntryBuilder<T extends SnmpEntry> extends Serializable {
        public T buildEntry(String oid, VarBind varbind);
    }

    public static <T, U extends SnmpEntry> Map<T, U> getWalkResult(final SnmpAccess snmp,
                                                                   final String oid, final EntryBuilder<U> builder, final KeyCreator<T> creator)
            throws IOException, AbortedException {
        Map<T, U> result = new HashMap<T, U>();
        try {
            AbstractWalkProcessor<Map<T, U>> walker =
                    new AbstractWalkProcessor<Map<T, U>>(result) {
                        private static final long serialVersionUID = 1L;

                        public void process(VarBind varbind) {
                            U entry = builder.buildEntry(oid, varbind);
                            T key = creator.getKey(entry.oidSuffix);
                            result.put(key, entry);
                            walkerlog.trace("getWalkResult():" + oid + ":" + key + "->"
                                    + new String(entry.value)
                                    + "(" + ByteArrayUtil.byteArrayToHexString(entry.value) + ")");
                        }
                    };
            snmp.walk(oid, walker);
            result = walker.getResult();
        } catch (SnmpResponseException e) {
            throw new IOException(e);
        } catch (RepeatedOidException e) {
            throw new IOException(e);
        }
        return result;
    }

    public static <T> Map<T, SnmpEntry> getWalkResult(
            final SnmpAccess snmp, final String oid, final KeyCreator<T> creator)
            throws IOException, AbortedException {
        return getWalkResult(snmp, oid, SnmpHelper.defaultEntryBuilder, creator);
    }

    public static String getIpAddressByHexaBytes(byte[] bytes) {
        StringBuffer sb = null;
        if (bytes.length == 4) {
            for (byte b : bytes) {
                if (sb == null) {
                    sb = new StringBuffer().append((int) b);
                } else {
                    sb.append(".").append((int) b);
                }
            }
        } else if (bytes.length == 16) {
            List<String> hexa = parseBytesAsHexString(bytes);
            for (String h : hexa) {
                if (sb == null) {
                    sb = new StringBuffer().append(h);
                } else {
                    sb.append(":").append(h);
                }
            }
        }
        return (sb == null ? "" : sb.toString());
    }

    public static String getIpAddressByBigInteger(BigInteger[] bigs) {
        if (bigs.length != 4 && bigs.length != 16) {
            throw new IllegalArgumentException();
        }
        byte[] bytes = new byte[bigs.length];
        for (int i = 0; i < bigs.length; i++) {
            bytes[i] = (byte) (bigs[i].byteValue() & 0xff);
        }
        return getIpAddressByHexaBytes(bytes);
    }

    private static List<String> parseBytesAsHexString(byte[] bytes) {
        List<String> result = new ArrayList<String>();
        for (byte b : bytes) {
            result.add(Integer.toHexString(b & 0xff));
        }
        return result;
    }

    public static String bigIntArrayToString(BigInteger[] array) {
        StringBuffer sb = null;
        for (BigInteger bigint : array) {
            if (sb == null) {
                sb = new StringBuffer().append(bigint.toString());
            } else {
                sb.append(".").append(bigint.toString());
            }
        }
        return sb.toString();
    }

    private final static byte splitIndicator = (byte) 0x80;

    public static String oidToOidString(byte[] arr) {
        boolean first = true;
        StringBuffer sb = new StringBuffer();
        long id = 0;
        for (byte b : arr) {
            if (first) {
                if (b == 43) {
                    sb.append(".1.3");
                } else {
                    sb.append('.');
                    sb.append(b & 0xff);
                }
                first = false;
            } else {
                id = id * 128L + (long) (b & 0x7f);
                if ((b & splitIndicator) != splitIndicator) {
                    sb.append('.');
                    sb.append(id);
                    id = 0;
                }
            }
        }
        return sb.toString();
    }

    public static <T extends SnmpEntry> Map<Integer, T> toIndexedMap(List<T> list) {
        Map<Integer, T> map = new HashMap<Integer, T>();
        for (T entry : list) {
            int index = entry.getLastOIDIndex().intValue();
            map.put(Integer.valueOf(index), entry);
        }
        return map;
    }
}