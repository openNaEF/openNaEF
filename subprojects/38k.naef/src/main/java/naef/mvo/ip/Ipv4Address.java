package naef.mvo.ip;

import naef.NaefUtils;
import tef.ExtraObjectCoder;
import tef.skelton.FormatException;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ipv4Address extends IpAddress {

    public static final ExtraObjectCoder EXTRA_OBJECT_CODER
        = new ExtraObjectCoder<Ipv4Address>("ipv4-address", Ipv4Address.class)
    {
        @Override public String encode(Ipv4Address object) {
            return Integer.toHexString(object.getRawAddress());
        }

        @Override public Ipv4Address decode(String str) {
            return Ipv4Address.gain(NaefUtils.parseHexInt(str));
        }
    };

    private static final List<Ipv4Address> instances__ = new ArrayList<Ipv4Address>();

    public static synchronized Ipv4Address gain(int address) {
        Ipv4Address addressObj = new Ipv4Address(address);
        int index = Collections.<Ipv4Address>binarySearch(instances__, addressObj);
        if (0 <= index) {
            return instances__.get(index);
        } else {
            instances__.add(addressObj);
            Collections.<Ipv4Address>sort(instances__);
            return addressObj;
        }
    }

    public static Ipv4Address gain(byte[] addressBytes) throws FormatException {
        if (addressBytes.length != 4) {
            throw new FormatException("IPv4 アドレスを指定してください.");
        }

        int rawAddress = 0;
        for (int i = 0; i < 4; i++) {
            rawAddress <<= 8;
            rawAddress |= addressBytes[i] & 0xff;
        }

        return gain(rawAddress);
    }

    public static Ipv4Address gain(String addressStr) throws FormatException {
        try {
            return gain(InetAddress.getByName(addressStr).getAddress());
        } catch (java.net.UnknownHostException uhe) {
            throw new FormatException("IPv4 アドレスの解析に失敗しました: " + uhe.getMessage());
        }
    }

    private final int address_;

    private Ipv4Address(int address) {
        address_ = address;
    }

    public int getRawAddress() {
        return address_;
    }

    @Override public int ipVersionBitLength() {
        return 32;
    }

    @Override public byte[] toBytes() {
        return toBytes(address_ & 0xffffffffl);
    }

    public static byte[] toBytes(long value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte)(value >> 24 & 0xff);
        bytes[1] = (byte)(value >> 16 & 0xff);
        bytes[2] = (byte)(value >> 8 & 0xff);
        bytes[3] = (byte)(value & 0xff);
        return bytes;
    }

    @Override public Ipv4Address offset(BigInteger offset) {
        byte[] address = new BigInteger(1, toBytes()).add(offset).toByteArray();
        byte[] result = new byte[4];
        for (int i = 0; i < Math.min(address.length, result.length); i++) {
            result[result.length - i - 1] = address[address.length - i - 1];
        }
        return gain(result);
    }

    public static int compare(Ipv4Address o1, Ipv4Address o2) {
        int addr1 = o1.getRawAddress();
        int addr2 = o2.getRawAddress();
        for (int i = 3; i >= 0; i--) {
            int signum = Integer.signum((addr1 >> (i * 8) & 0xff) - (addr2 >> (i * 8) & 0xff));
            if (signum != 0) {
                return signum;
            }
        }
        return 0;
    }

    public Object readResolve() throws java.io.ObjectStreamException {
        return gain(address_);
    }

    @Override public String toString() {
        return NaefUtils.formatIntAsDottedOctet(address_);
    }

    @Override public int hashCode() {
        return address_;
    }

    @Override public boolean equals(Object obj) {
        if (obj == null
            || obj.getClass() != this.getClass())
        {
            return false;
        } else {
            Ipv4Address another = (Ipv4Address) obj;
            return another.address_ == this.address_;
        }
    }
}
