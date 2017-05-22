package naef.mvo.ip;

import naef.NaefUtils;
import tef.ExtraObjectCoder;
import tef.skelton.FormatException;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ipv6Address extends IpAddress {

    public static final ExtraObjectCoder EXTRA_OBJECT_CODER
        = new ExtraObjectCoder<Ipv6Address>("ipv6-address", Ipv6Address.class)
    {
        @Override public String encode(Ipv6Address object) {
            return Long.toHexString(object.getRawUpperAddress()) + ":" + Long.toHexString(object.getRawLowerAddress());
        }

        @Override public Ipv6Address decode(String str) {
            int delimiterIndex = str.indexOf(":");
            return Ipv6Address.gain(
                NaefUtils.parseHexLong(str.substring(0, delimiterIndex)),
                NaefUtils.parseHexLong(str.substring(delimiterIndex + 1)));
        }
    };

    private static final List<Ipv6Address> instances__ = new ArrayList<Ipv6Address>();

    public static synchronized Ipv6Address gain(long upperAddress, long lowerAddress) {
        Ipv6Address addressObj = new Ipv6Address(upperAddress, lowerAddress);
        int index = Collections.<Ipv6Address>binarySearch(instances__, addressObj);
        if (0 <= index) {
            return instances__.get(index);
        } else {
            instances__.add(addressObj);
            Collections.<Ipv6Address>sort(instances__);
            return addressObj;
        }
    }

    public static Ipv6Address gain(byte[] addressBytes) throws FormatException {
        if (addressBytes.length != 16) {
            throw new FormatException("IPv6 アドレスを指定してください.");
        }

        long rawUpperAddress = 0;
        for (int i = 0; i < 8; i++) {
            rawUpperAddress <<= 8;
            rawUpperAddress |= addressBytes[i] & 0xff;
        }

        long rawLowerAddress = 0;
        for (int i = 0; i < 8; i++) {
            rawLowerAddress <<= 8;
            rawLowerAddress |= addressBytes[i + 8] & 0xff;
        }

        return gain(rawUpperAddress, rawLowerAddress);
    }

    public static Ipv6Address gain(String addressStr) throws FormatException {
        try {
            return gain(InetAddress.getByName(addressStr).getAddress());
        } catch (java.net.UnknownHostException uhe) {
            throw new FormatException("IPv6 アドレスの解析に失敗しました: " + uhe.getMessage());
        }
    }

    private final long upperAddress_;
    private final long lowerAddress_;

    private Ipv6Address(long upperAddress, long lowerAddress) {
        upperAddress_ = upperAddress;
        lowerAddress_ = lowerAddress;
    }

    public long getRawUpperAddress() {
        return upperAddress_;
    }

    public long getRawLowerAddress() {
        return lowerAddress_;
    }

    @Override public int ipVersionBitLength() {
        return 128;
    }

    @Override public byte[] toBytes() {
        byte[] result = new byte[16];
        System.arraycopy(toBytes(getRawUpperAddress()), 0, result, 0, 8);
        System.arraycopy(toBytes(getRawLowerAddress()), 0, result, 8, 8);
        return result;
    }

    private static byte[] toBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte)(value >> ((7 - i) * 8) & 0xff);
        }
        return bytes;
    }

    @Override public Ipv6Address offset(BigInteger offset) {
        byte[] address = new BigInteger(1, toBytes()).add(offset).toByteArray();
        byte[] result = new byte[16];
        for (int i = 0; i < Math.min(address.length, result.length); i++) {
            result[result.length - i - 1] = address[address.length - i - 1];
        }
        return gain(result);
    }

    public static int compare(Ipv6Address o1, Ipv6Address o2) {
        int upperResult = compare(o1.getRawUpperAddress(), o2.getRawUpperAddress());
        if (upperResult != 0) {
            return upperResult;
        }
        return compare(o1.getRawLowerAddress(), o2.getRawLowerAddress());
    }

    private static int compare(long value1, long value2) {
        for (int i = 7; i >= 0; i--) {
            int signum = Long.signum((value1 >> (i * 8) & 0xff) - (value2 >> (i * 8) & 0xff));
            if (signum != 0) {
                return signum;
            }
        }
        return 0;
    }

    public Object readResolve() throws java.io.ObjectStreamException {
        return gain(upperAddress_, lowerAddress_);
    }

    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        byte[] bytes = toBytes();
        for (int i = 0; i < bytes.length / 2; i++) {
            result.append(result.length() == 0 ? "" : ":");

            int value = (bytes[i * 2] << 8) + (bytes[i * 2 + 1] & 0xff);
            result.append(Integer.toString(value, 16));
        }
        return result.toString();
    }

    @Override public int hashCode() {
        return (int)(lowerAddress_ & 0xffffffffl);
    }

    @Override public boolean equals(Object obj) {
        if (obj == null
            || obj.getClass() != this.getClass())
        {
            return false;
        } else {
            Ipv6Address another = (Ipv6Address) obj;
            return another.upperAddress_ == this.upperAddress_
                && another.lowerAddress_ == this.lowerAddress_;
        }
    }
}
