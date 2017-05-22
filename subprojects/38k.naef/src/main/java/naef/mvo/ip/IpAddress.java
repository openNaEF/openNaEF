package naef.mvo.ip;

import tef.skelton.AttributeType;
import tef.skelton.FormatException;
import tef.skelton.ValueException;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * NAEF が扱う IP アドレスです. java.net.InetAddress との相互変換が可能です.
 */
public abstract class IpAddress implements Comparable<IpAddress>, Serializable {

    /**
     * subnet address (network address と mask length で指定されるアドレス空間) の計算を補助する
     * ユーテリティ クラスです.
     */
    public static class SubnetAddressUtils {

        private SubnetAddressUtils() {
        }

        public static BigInteger size(IpAddress networkAddress, int maskLength)
            throws ValueException
        {
            BigInteger size = BigInteger.ONE.shiftLeft(networkAddress.ipVersionBitLength() - maskLength);
            if (! networkAddress.toBigInteger().mod(size).equals(BigInteger.ZERO)) {
                throw new ValueException(
                    "ネットワーク アドレスとマスク長が整合していません(ネットワーク アドレスが"
                    + "マスク長で指定されたサブネット サイズの倍数になっていません).");
            }
            return size;
        }

        public static IpAddress endAddress(IpAddress networkAddress, int maskLength)
            throws ValueException
        {
            BigInteger size = size(networkAddress, maskLength);
            return networkAddress.offset(size.subtract(BigInteger.ONE));
        }

        public static java.lang.Integer maskLength(IpAddress lowerAddress, IpAddress upperAddress) {
            if (lowerAddress == null || upperAddress == null) {
                return null;
            }

            try {
                checkCanonicalSubnetAddressRange(lowerAddress, upperAddress);
            } catch (ValueException ve) {
                return null;
            }

            BigInteger lower = lowerAddress.toBigInteger();
            BigInteger upper = upperAddress.toBigInteger();
            BigInteger size = upper.subtract(lower);

            return new java.lang.Integer(upperAddress.ipVersionBitLength() - size.bitLength());
        }

        public static void checkCanonicalSubnetAddressRange(IpAddress lowerBound, IpAddress upperBound) {
            BigInteger size = getRangeSize(lowerBound, upperBound);
            boolean isPowersOfTwo = size.bitCount() == 1;
            if (! isPowersOfTwo) {
                throw new ValueException("サイズが2の冪になっていません.");
            }
            if (! lowerBound.toBigInteger().mod(size).equals(BigInteger.ZERO)) {
                throw new ValueException("始点アドレス(ネットワーク アドレス)がサイズの倍数になっていません.");
            }
        }

        private static BigInteger getRangeSize(IpAddress lowerBound, IpAddress upperBound) {
            return upperBound.toBigInteger()
                .subtract(lowerBound.toBigInteger())
                .add(BigInteger.ONE);
        }
    }

    public static final AttributeType<IpAddress> TYPE = new AttributeType<IpAddress>(IpAddress.class, true) {

        @Override public String format(IpAddress obj) {
            return obj.toString();
        }

        @Override public IpAddress parse(String str) {
            return IpAddress.gain(str);
        }
    };

    public static IpAddress gain(String addressStr) throws FormatException {
        try {
            return Ipv4Address.gain(addressStr);
        } catch (FormatException fe) {
        }
        try {
            return Ipv6Address.gain(addressStr);
        } catch (FormatException fe) {
        }

        throw new FormatException("IPアドレスの解析に失敗しました.");
    }

    public static java.net.InetAddress toJavaInetAddress(IpAddress address) {
        try {
            return address == null ? null : java.net.InetAddress.getByAddress(address.toBytes());
        } catch (java.net.UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }

    public static IpAddress valueOf(java.net.InetAddress javaAddress) throws ValueException {
        if (javaAddress == null) {
            return null;
        }

        byte[] rawAddress = javaAddress.getAddress();
        switch (rawAddress.length) {
            case 4:
                return Ipv4Address.gain(rawAddress);
            case 16:
                return Ipv6Address.gain(rawAddress);
            default:
                throw new ValueException("不明なIPアドレス バージョンです: " + rawAddress.length);
        }
    }

    public IpAddress() {
    }

    abstract public int ipVersionBitLength();

    abstract public byte[] toBytes();

    /**
     * この IpAddress の byte 配列表現を unsigned として解釈した正数の BigInteger を返します.
     * この変換はアドレス計算をサポートするためのものであり, {@link #offset(BigInteger)} 
     * と合わせての使用を想定しています.
     * <p>
     * このメソッドの逆変換に相当する BigInteger から IpAddress への変換メソッドは用意されて
     * いません. これは IpAddress から BigInteger への変換は不可逆であるためです. 
     * 例えば, v4 の 0.0.0.0 と v6 の ::0 はどちらも {0} (長さ1の byte 配列) と等価の BigInteger 
     * となるため, バージョン情報は失われてしまいます.
     * <p>
     * 別の言い方をすると, IpAddress を表す byte 配列表現は配列長が 4 または 16 のどちらかで
     * なければならないということで, bit 長が暗黙にバージョン情報を表しているのですが, このメソッド
     * が返す BigInteger はアドレス空間内の位置情報だけであると言えます.
     */
    public BigInteger toBigInteger() {
        return new BigInteger(1, toBytes());
    }

    abstract public IpAddress offset(BigInteger offset);

    @Override public int compareTo(IpAddress another) {
        if (this.getClass() == Ipv4Address.class) {
            if (another.getClass() == Ipv4Address.class) {
                return Ipv4Address.compare((Ipv4Address) this, (Ipv4Address) another);
            } else if (another.getClass() == Ipv6Address.class) {
                return 1;
            }
        } else if (this.getClass() == Ipv6Address.class) {
            if (another.getClass() == Ipv4Address.class) {
                return -1;
            } else if (another.getClass() == Ipv6Address.class) {
                return Ipv6Address.compare((Ipv6Address) this, (Ipv6Address) another);
            }
        }

        throw new RuntimeException(this.getClass() + ", " + another.getClass());
    }
}
